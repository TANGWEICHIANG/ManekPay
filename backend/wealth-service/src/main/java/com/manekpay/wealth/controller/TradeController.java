package com.manekpay.wealth.controller;

import com.manekpay.wealth.dto.CreateTradeRequest;
import com.manekpay.wealth.dto.TradeResponse;
import com.manekpay.wealth.entity.Trade;
import com.manekpay.wealth.service.TradeService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/trades")
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse createTrade(@Valid @RequestBody CreateTradeRequest request,
                                      @RequestHeader("X-Idempotency-Key") String idempotencyKey,
                                      @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Trade trade = tradeService.buy(customerId, request.assetSymbol(), request.amount(), idempotencyKey);
        return new TradeResponse(trade.getId(), request.assetSymbol(), trade.getAmount(), trade.getShares(), trade.getPricePerShare(), trade.getCreatedAt());
    }
}
