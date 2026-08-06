package com.manekpay.fx.controller;

import com.manekpay.fx.dto.FxRateResponse;
import com.manekpay.fx.entity.Currency;
import com.manekpay.fx.service.FxRateService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FxRateController {

    private final FxRateService fxRateService;

    public FxRateController(FxRateService fxRateService) {
        this.fxRateService = fxRateService;
    }

    @GetMapping("/fx/rates/{from}/{to}")
    public FxRateResponse getRate(@PathVariable Currency from, @PathVariable Currency to) {
        return new FxRateResponse(from, to, fxRateService.getRate(from, to));
    }
}
