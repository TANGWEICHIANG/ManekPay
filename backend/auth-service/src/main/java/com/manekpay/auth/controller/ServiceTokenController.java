package com.manekpay.auth.controller;

import com.manekpay.auth.dto.ServiceTokenRequest;
import com.manekpay.auth.dto.ServiceTokenResponse;
import com.manekpay.auth.service.JwtService;
import com.manekpay.auth.service.ServiceTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceTokenController {

    private final ServiceTokenService serviceTokenService;

    public ServiceTokenController(ServiceTokenService serviceTokenService) {
        this.serviceTokenService = serviceTokenService;
    }

    @PostMapping("/service-token")
    public ServiceTokenResponse issue(@Valid @RequestBody ServiceTokenRequest request) {
        String token = serviceTokenService.issueToken(request.clientId(), request.clientSecret());
        return new ServiceTokenResponse(token, JwtService.SERVICE_TOKEN_TTL.toSeconds());
    }
}
