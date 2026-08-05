package com.manekpay.ledger.controller;

import com.manekpay.ledger.config.CurrentCustomer;
import com.manekpay.ledger.dto.MyAccountResponse;
import com.manekpay.ledger.dto.WalletDto;
import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.repository.WalletRepository;
import com.manekpay.ledger.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts/me")
public class AccountController {

    private final AccountService accountService;
    private final WalletRepository walletRepository;

    public AccountController(AccountService accountService, WalletRepository walletRepository) {
        this.accountService = accountService;
        this.walletRepository = walletRepository;
    }

    @GetMapping
    public MyAccountResponse me() {
        Account account = accountService.getOrCreateAccount(CurrentCustomer.id());
        List<WalletDto> wallets = walletRepository.findByAccountId(account.getId()).stream()
                .map(w -> new WalletDto(w.getCurrency(), w.getBalance()))
                .toList();
        return new MyAccountResponse(account.getId(), account.getAccountNumber(), wallets);
    }
}
