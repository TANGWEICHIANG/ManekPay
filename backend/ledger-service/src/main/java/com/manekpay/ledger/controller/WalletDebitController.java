package com.manekpay.ledger.controller;

import com.manekpay.ledger.dto.WalletDebitRequest;
import com.manekpay.ledger.dto.WalletDebitResponse;
import com.manekpay.ledger.service.WalletDebitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/wallets")
public class WalletDebitController {

    private final WalletDebitService walletDebitService;

    public WalletDebitController(WalletDebitService walletDebitService) {
        this.walletDebitService = walletDebitService;
    }

    @PostMapping("/debit")
    public WalletDebitResponse debit(@Valid @RequestBody WalletDebitRequest request,
                                      @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        return walletDebitService.debit(request, idempotencyKey);
    }
}
