package com.manekpay.ledger.controller;

import com.manekpay.ledger.config.CurrentCustomer;
import com.manekpay.ledger.dto.CreateProxyRequest;
import com.manekpay.ledger.dto.MyAccountResponse;
import com.manekpay.ledger.dto.ProxiesResponse;
import com.manekpay.ledger.dto.ProxyResponse;
import com.manekpay.ledger.dto.WalletDto;
import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.repository.WalletRepository;
import com.manekpay.ledger.service.AccountService;
import com.manekpay.ledger.service.ProxyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/me")
public class AccountController {

    private final AccountService accountService;
    private final ProxyService proxyService;
    private final WalletRepository walletRepository;

    public AccountController(AccountService accountService, ProxyService proxyService, WalletRepository walletRepository) {
        this.accountService = accountService;
        this.proxyService = proxyService;
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

    @PostMapping("/proxies")
    @ResponseStatus(HttpStatus.CREATED)
    public ProxyResponse linkProxy(@Valid @RequestBody CreateProxyRequest request) {
        Account account = accountService.getOrCreateAccount(CurrentCustomer.id());
        AccountProxy proxy = proxyService.linkProxy(account.getId(), request.type(), request.value());
        return new ProxyResponse(proxy.getId(), proxy.getType(), proxy.getValue(), proxy.getCreatedAt());
    }

    @GetMapping("/proxies")
    public ProxiesResponse listProxies() {
        Account account = accountService.getOrCreateAccount(CurrentCustomer.id());
        List<ProxyResponse> proxies = proxyService.listProxies(account.getId()).stream()
                .map(p -> new ProxyResponse(p.getId(), p.getType(), p.getValue(), p.getCreatedAt()))
                .toList();
        return new ProxiesResponse(proxies);
    }

    @DeleteMapping("/proxies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProxy(@PathVariable UUID id) {
        Account account = accountService.getOrCreateAccount(CurrentCustomer.id());
        proxyService.deleteProxy(account.getId(), id);
    }
}
