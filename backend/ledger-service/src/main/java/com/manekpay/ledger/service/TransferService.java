package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RecipientDto;
import com.manekpay.ledger.dto.RiskStatusResponse;
import com.manekpay.ledger.dto.TransferRequest;
import com.manekpay.ledger.dto.TransferResponse;
import com.manekpay.ledger.dto.TransfersResponse;
import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Direction;
import com.manekpay.ledger.entity.LedgerEntry;
import com.manekpay.ledger.entity.ProxyType;
import com.manekpay.ledger.entity.Transfer;
import com.manekpay.ledger.entity.Wallet;
import com.manekpay.ledger.exception.AccountRestrictedException;
import com.manekpay.ledger.exception.FxServiceUnavailableException;
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
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private final RiskServiceClient riskServiceClient;
    private final EntityManager entityManager;

    public TransferService(AccountService accountService, AccountRepository accountRepository,
                            AccountProxyRepository proxyRepository, WalletRepository walletRepository,
                            TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository,
                            FxRateProvider fxRateProvider, AuthServiceClient authServiceClient,
                            RiskServiceClient riskServiceClient, EntityManager entityManager) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.proxyRepository = proxyRepository;
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fxRateProvider = fxRateProvider;
        this.authServiceClient = authServiceClient;
        this.riskServiceClient = riskServiceClient;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransferResponse transfer(UUID customerId, String bearerToken, Currency homeCurrency,
                                      TransferRequest request, String idempotencyKey) {
        if (!"APPROVED".equals(authServiceClient.getLiveKycStatus(bearerToken))) {
            throw new KycNotApprovedException();
        }
        RiskStatusResponse riskStatus = riskServiceClient.getRiskStatus(customerId);
        if (riskStatus.restricted()) {
            throw new AccountRestrictedException(riskStatus.restrictedUntil());
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
        if (crossCurrency && fxRate == null) {
            // An empty/malformed rate must fail closed like any other fx-service unavailability,
            // not NPE on the multiply below - there is no fallback for the main transfer's own
            // conversion the way there is for the optional top-up.
            throw new FxServiceUnavailableException(new IllegalStateException("fx-service returned an empty rate"));
        }
        BigDecimal destAmount = crossCurrency
                ? request.amount().multiply(fxRate).setScale(4, RoundingMode.HALF_EVEN)
                : request.amount();

        // A customer whose home currency differs from this transfer's source currency is
        // eligible for an automatic top-up if the source wallet falls short - see BR-2.3.
        boolean topUpEligible = homeCurrency != request.sourceCurrency();
        // An unlocked pre-read: only worth fetching the top-up rate or including the home-currency/
        // clearing wallets in the lock set if the source wallet actually looks short right now. This is
        // a scoping optimization, not a correctness check - if a concurrent debit lands between this
        // read and the lock being acquired, this optimization may occasionally skip a top-up that
        // (depending on read/lock semantics) could still have been served, or lock the home wallets
        // unnecessarily when none was needed after all - either way the unified balance check below
        // still throws InsufficientBalanceException exactly as it would have before this feature
        // existed, never letting an underfunded transfer through.
        boolean topUpLikelyNeeded = topUpEligible && senderWalletRef.getBalance().compareTo(request.amount()) < 0;
        // The top-up rate is fetched here, before any wallet is locked, for the same reason the main
        // transfer's own fxRate above is fetched before locking: an external HTTP call must never run
        // while holding FOR UPDATE on shared rows (the clearing wallets are process-wide singletons -
        // holding their locks during a slow/blocked fx-service call would serialize every transfer in
        // that currency pair). Gated on topUpLikelyNeeded, not merely topUpEligible, so a fully-funded
        // transfer where home != source never pays this call or depends on fx-service being reachable.
        BigDecimal homeToSourceRate = topUpLikelyNeeded
                ? fxRateProvider.getRate(homeCurrency, request.sourceCurrency(), bearerToken)
                : null;

        Wallet sourceClearingRef = null;
        Wallet destClearingRef = null;
        Wallet homeWalletRef = null;
        Wallet homeClearingRef = null;
        List<UUID> walletIdsToLock = new ArrayList<>(List.of(senderWalletRef.getId(), recipientWalletRef.getId()));
        if (crossCurrency) {
            sourceClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(request.sourceCurrency())
                    .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + request.sourceCurrency()));
            destClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(request.destCurrency())
                    .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + request.destCurrency()));
            walletIdsToLock.add(sourceClearingRef.getId());
            walletIdsToLock.add(destClearingRef.getId());
        }
        if (topUpLikelyNeeded) {
            homeWalletRef = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), homeCurrency)
                    .orElseThrow(() -> new IllegalStateException("Missing home-currency wallet for " + homeCurrency));
            homeClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(homeCurrency)
                    .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + homeCurrency));
            walletIdsToLock.add(homeWalletRef.getId());
            walletIdsToLock.add(homeClearingRef.getId());
            if (sourceClearingRef == null) {
                sourceClearingRef = walletRepository.findByAccountIdIsNullAndCurrency(request.sourceCurrency())
                        .orElseThrow(() -> new IllegalStateException("Missing clearing wallet for " + request.sourceCurrency()));
                walletIdsToLock.add(sourceClearingRef.getId());
            }
        }
        // Dedupe before sorting: the home-currency clearing wallet and the destination-currency
        // clearing wallet are the same row whenever destCurrency == homeCurrency (a transfer that is
        // both cross-currency and top-up-eligible, converting into the customer's own home currency) -
        // findByIdForUpdate must be called once per wallet, not twice.
        walletIdsToLock = walletIdsToLock.stream().distinct().sorted().toList();

        Map<UUID, Wallet> locked = new HashMap<>();
        for (UUID walletId : walletIdsToLock) {
            Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                    .orElseThrow(() -> new IllegalStateException("Wallet disappeared mid-transfer: " + walletId));
            // findByIdForUpdate's SELECT ... FOR UPDATE correctly acquires the row lock, but every
            // wallet reaching this loop was already loaded unlocked earlier in this method (as
            // senderWalletRef, recipientWalletRef, or one of the clearing/home refs) - Hibernate's
            // persistence-context identity map returns that same, already-managed instance rather
            // than re-hydrating it from this query's result, so the in-memory balance can still be
            // the pre-lock snapshot even though the correct DB-level lock was acquired. Force a real
            // reload now that the lock is held, or a concurrent update committed between the
            // unlocked read and this point is invisible to every balance check below.
            entityManager.refresh(wallet);
            locked.put(walletId, wallet);
        }

        Wallet senderWallet = locked.get(senderWalletRef.getId());

        // Determine whether a top-up is needed and, if so, whether the home wallet can cover it -
        // purely a check here, no mutation yet. All balance checks must complete before the
        // Transfer row is created below (ledger tables are append-only; a failed attempt must
        // never leave a row behind).
        BigDecimal topUpAmount = null;
        Currency topUpCurrency = null;
        BigDecimal topUpFxRate = null;
        BigDecimal topUpAmountInSourceCurrency = null;
        if (homeWalletRef != null && homeToSourceRate != null && homeToSourceRate.signum() > 0
                && senderWallet.getBalance().compareTo(request.amount()) < 0) {
            BigDecimal shortfall = request.amount().subtract(senderWallet.getBalance());
            BigDecimal shortfallInHomeCurrency = shortfall.divide(homeToSourceRate, 4, RoundingMode.HALF_EVEN);

            Wallet homeWallet = locked.get(homeWalletRef.getId());
            if (homeWallet.getBalance().compareTo(shortfallInHomeCurrency) >= 0) {
                topUpAmount = shortfallInHomeCurrency;
                topUpCurrency = homeCurrency;
                topUpFxRate = homeToSourceRate;
                topUpAmountInSourceCurrency = shortfall;
            }
        }

        // Single unified check: passes trivially when there was no shortfall, passes when a
        // top-up was found and fully covers the shortfall, and correctly still fails when
        // topUpAmountInSourceCurrency is null (no top-up was eligible, or the home wallet
        // couldn't cover it) - matching today's behavior exactly in that case.
        BigDecimal effectiveBalance = topUpAmountInSourceCurrency != null
                ? senderWallet.getBalance().add(topUpAmountInSourceCurrency)
                : senderWallet.getBalance();
        if (effectiveBalance.compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException();
        }
        Wallet recipientWallet = locked.get(recipientWalletRef.getId());

        Transfer transfer = transferRepository.save(new Transfer(senderWallet.getId(), recipientWallet.getId(),
                request.amount(), request.sourceCurrency(), destAmount, request.destCurrency(), fxRate, idempotencyKey,
                topUpAmount, topUpCurrency, topUpFxRate));

        if (topUpAmount != null) {
            Wallet homeWallet = locked.get(homeWalletRef.getId());
            homeWallet.setBalance(homeWallet.getBalance().subtract(topUpAmount));
            walletRepository.save(homeWallet);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), homeWallet.getId(), Direction.DEBIT,
                    topUpAmount, homeCurrency, homeWallet.getBalance()));

            Wallet homeClearing = locked.get(homeClearingRef.getId());
            homeClearing.setBalance(homeClearing.getBalance().add(topUpAmount));
            walletRepository.save(homeClearing);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), homeClearing.getId(), Direction.CREDIT,
                    topUpAmount, homeCurrency, homeClearing.getBalance()));

            Wallet sourceClearingForTopUp = locked.get(sourceClearingRef.getId());
            sourceClearingForTopUp.setBalance(sourceClearingForTopUp.getBalance().subtract(topUpAmountInSourceCurrency));
            walletRepository.save(sourceClearingForTopUp);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), sourceClearingForTopUp.getId(), Direction.DEBIT,
                    topUpAmountInSourceCurrency, request.sourceCurrency(), sourceClearingForTopUp.getBalance()));

            senderWallet.setBalance(senderWallet.getBalance().add(topUpAmountInSourceCurrency));
            walletRepository.save(senderWallet);
            ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), senderWallet.getId(), Direction.CREDIT,
                    topUpAmountInSourceCurrency, request.sourceCurrency(), senderWallet.getBalance()));
        }

        senderWallet.setBalance(senderWallet.getBalance().subtract(request.amount()));
        walletRepository.save(senderWallet);
        ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), senderWallet.getId(), Direction.DEBIT,
                request.amount(), request.sourceCurrency(), senderWallet.getBalance()));

        if (crossCurrency) {
            // sourceClearingRef may be the same wallet the top-up block above already debited
            // (when this transfer is both cross-currency and top-up-eligible) - locked.get(...)
            // returns that same, already-mutated instance, so this credit applies on top of it
            // rather than overwriting it.
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
                transfer.getDestAmount(), transfer.getDestCurrency(), transfer.getFxRate(), transfer.getCreatedAt(),
                transfer.getTopUpAmount(), transfer.getTopUpCurrency(), transfer.getTopUpFxRate());
    }
}
