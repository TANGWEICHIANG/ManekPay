package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RecipientDto;
import com.manekpay.ledger.dto.TransferRequest;
import com.manekpay.ledger.dto.TransferResponse;
import com.manekpay.ledger.dto.TransfersResponse;
import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.Direction;
import com.manekpay.ledger.entity.LedgerEntry;
import com.manekpay.ledger.entity.ProxyType;
import com.manekpay.ledger.entity.Transfer;
import com.manekpay.ledger.entity.Wallet;
import com.manekpay.ledger.exception.InsufficientBalanceException;
import com.manekpay.ledger.exception.KycNotApprovedException;
import com.manekpay.ledger.exception.RecipientNotFoundException;
import com.manekpay.ledger.exception.SelfTransferException;
import com.manekpay.ledger.exception.TransferNotFoundException;
import com.manekpay.ledger.repository.AccountProxyRepository;
import com.manekpay.ledger.repository.AccountRepository;
import com.manekpay.ledger.repository.LedgerEntryRepository;
import com.manekpay.ledger.repository.TransferRepository;
import com.manekpay.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final AccountProxyRepository proxyRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FxRateProvider fxRateProvider;
    private final AuthServiceClient authServiceClient;

    public TransferService(AccountService accountService, AccountRepository accountRepository,
                            AccountProxyRepository proxyRepository, WalletRepository walletRepository,
                            TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository,
                            FxRateProvider fxRateProvider, AuthServiceClient authServiceClient) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.proxyRepository = proxyRepository;
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fxRateProvider = fxRateProvider;
        this.authServiceClient = authServiceClient;
    }

    @Transactional
    public TransferResponse transfer(UUID customerId, String bearerToken, TransferRequest request, String idempotencyKey) {
        if (!"APPROVED".equals(authServiceClient.getLiveKycStatus(bearerToken))) {
            throw new KycNotApprovedException();
        }

        Account senderAccount = accountService.getOrCreateAccount(customerId);
        UUID recipientAccountId = resolveRecipient(request.recipient());
        if (recipientAccountId.equals(senderAccount.getId())) {
            throw new SelfTransferException();
        }

        Wallet senderWalletRef = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), request.sourceCurrency())
                .orElseThrow(() -> new IllegalStateException("Missing sender wallet for " + request.sourceCurrency()));
        Wallet recipientWalletRef = walletRepository.findByAccountIdAndCurrency(recipientAccountId, request.destCurrency())
                .orElseThrow(() -> new IllegalStateException("Missing recipient wallet for " + request.destCurrency()));

        boolean crossCurrency = request.sourceCurrency() != request.destCurrency();
        BigDecimal fxRate = crossCurrency ? fxRateProvider.getRate(request.sourceCurrency(), request.destCurrency(), bearerToken) : null;
        BigDecimal destAmount = crossCurrency
                ? request.amount().multiply(fxRate).setScale(4, RoundingMode.HALF_EVEN)
                : request.amount();

        Wallet sourceClearingRef = null;
        Wallet destClearingRef = null;
        List<UUID> walletIdsToLock = new ArrayList<>(List.of(senderWalletRef.getId(), recipientWalletRef.getId()));
        if (crossCurrency) {
            sourceClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(request.sourceCurrency())
                    .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + request.sourceCurrency()));
            destClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(request.destCurrency())
                    .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + request.destCurrency()));
            walletIdsToLock.add(sourceClearingRef.getId());
            walletIdsToLock.add(destClearingRef.getId());
        }
        walletIdsToLock.sort(Comparator.naturalOrder());

        Map<UUID, Wallet> locked = new HashMap<>();
        for (UUID walletId : walletIdsToLock) {
            locked.put(walletId, walletRepository.findByIdForUpdate(walletId)
                    .orElseThrow(() -> new IllegalStateException("Wallet disappeared mid-transfer: " + walletId)));
        }

        Wallet senderWallet = locked.get(senderWalletRef.getId());
        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException();
        }
        Wallet recipientWallet = locked.get(recipientWalletRef.getId());

        Transfer transfer = transferRepository.save(new Transfer(senderWallet.getId(), recipientWallet.getId(),
                request.amount(), request.sourceCurrency(), destAmount, request.destCurrency(), fxRate, idempotencyKey));

        senderWallet.setBalance(senderWallet.getBalance().subtract(request.amount()));
        walletRepository.save(senderWallet);
        ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), senderWallet.getId(), Direction.DEBIT,
                request.amount(), request.sourceCurrency(), senderWallet.getBalance()));

        if (crossCurrency) {
            Wallet sourceClearing = locked.get(sourceClearingRef.getId());
            sourceClearing.setBalance(sourceClearing.getBalance().add(request.amount()));
            walletRepository.save(sourceClearing);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), sourceClearing.getId(), Direction.CREDIT,
                    request.amount(), request.sourceCurrency(), sourceClearing.getBalance()));

            Wallet destClearing = locked.get(destClearingRef.getId());
            destClearing.setBalance(destClearing.getBalance().subtract(destAmount));
            walletRepository.save(destClearing);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), destClearing.getId(), Direction.DEBIT,
                    destAmount, request.destCurrency(), destClearing.getBalance()));
        }

        recipientWallet.setBalance(recipientWallet.getBalance().add(destAmount));
        walletRepository.save(recipientWallet);
        ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), recipientWallet.getId(), Direction.CREDIT,
                destAmount, request.destCurrency(), recipientWallet.getBalance()));

        return toResponse(transfer);
    }

    public TransfersResponse listTransfers(UUID customerId) {
        List<UUID> walletIds = myWalletIds(customerId);
        List<TransferResponse> transfers = transferRepository.findByWalletIdsOrderByCreatedAtDesc(walletIds).stream()
                .map(this::toResponse)
                .toList();
        return new TransfersResponse(transfers);
    }

    public TransferResponse getTransfer(UUID customerId, UUID transferId) {
        List<UUID> walletIds = myWalletIds(customerId);
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(TransferNotFoundException::new);
        if (!walletIds.contains(transfer.getFromWalletId()) && !walletIds.contains(transfer.getToWalletId())) {
            throw new TransferNotFoundException();
        }
        return toResponse(transfer);
    }

    private List<UUID> myWalletIds(UUID customerId) {
        Account account = accountService.getOrCreateAccount(customerId);
        return walletRepository.findByAccountId(account.getId()).stream().map(Wallet::getId).toList();
    }

    private UUID resolveRecipient(RecipientDto recipient) {
        return switch (recipient.type()) {
            case ACCOUNT_NUMBER -> accountRepository.findByAccountNumber(recipient.value())
                    .map(Account::getId)
                    .orElseThrow(RecipientNotFoundException::new);
            case NRIC -> proxyRepository.findByTypeAndValue(ProxyType.NRIC, recipient.value())
                    .map(AccountProxy::getAccountId)
                    .orElseThrow(RecipientNotFoundException::new);
            case MOBILE -> proxyRepository.findByTypeAndValue(ProxyType.MOBILE, recipient.value())
                    .map(AccountProxy::getAccountId)
                    .orElseThrow(RecipientNotFoundException::new);
        };
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getSourceAmount(), transfer.getSourceCurrency(),
                transfer.getDestAmount(), transfer.getDestCurrency(), transfer.getFxRate(), transfer.getCreatedAt());
    }
}
