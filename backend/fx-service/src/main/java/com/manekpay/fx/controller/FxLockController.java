package com.manekpay.fx.controller;

import com.manekpay.fx.dto.CreateLockRequest;
import com.manekpay.fx.dto.FxLockResponse;
import com.manekpay.fx.service.FxLockService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fx/locks")
public class FxLockController {

    private final FxLockService fxLockService;

    public FxLockController(FxLockService fxLockService) {
        this.fxLockService = fxLockService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FxLockResponse createLock(@Valid @RequestBody CreateLockRequest request) {
        return toResponse(fxLockService.createLock(request.from(), request.to()));
    }

    @GetMapping("/{lockId}")
    public FxLockResponse getLock(@PathVariable String lockId) {
        return toResponse(fxLockService.getLock(lockId));
    }

    private FxLockResponse toResponse(FxLockService.FxLock lock) {
        return new FxLockResponse(lock.lockId(), lock.from(), lock.to(), lock.rate(), lock.expiresAt());
    }
}
