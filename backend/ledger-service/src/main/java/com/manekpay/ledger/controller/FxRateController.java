package com.manekpay.ledger.controller;

import com.manekpay.ledger.dto.FxRateResponse;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.service.FxRateProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FxRateController {

    private final FxRateProvider fxRateProvider;

    public FxRateController(FxRateProvider fxRateProvider) {
        this.fxRateProvider = fxRateProvider;
    }

    @GetMapping("/fx-rates/{from}/{to}")
    public FxRateResponse getRate(@PathVariable Currency from, @PathVariable Currency to) {
        return new FxRateResponse(from, to, fxRateProvider.getRate(from, to));
    }
}
