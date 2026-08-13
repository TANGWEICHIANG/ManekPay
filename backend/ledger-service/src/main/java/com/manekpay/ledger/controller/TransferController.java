package com.manekpay.ledger.controller;

import com.manekpay.ledger.config.CurrentCustomer;
import com.manekpay.ledger.dto.TransactionCreatedEvent;
import com.manekpay.ledger.dto.TransferRequest;
import com.manekpay.ledger.dto.TransferResponse;
import com.manekpay.ledger.dto.TransfersResponse;
import com.manekpay.ledger.service.TransactionEventPublisher;
import com.manekpay.ledger.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransactionEventPublisher eventPublisher;

    public TransferController(TransferService transferService, TransactionEventPublisher eventPublisher) {
        this.transferService = transferService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody TransferRequest request,
                                    @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        UUID customerId = CurrentCustomer.id();
        TransferResponse response = transferService.transfer(customerId, CurrentCustomer.bearerToken(), request, idempotencyKey);
        Double latitude = request.location() != null ? request.location().latitude() : null;
        Double longitude = request.location() != null ? request.location().longitude() : null;
        eventPublisher.publishTransactionCreated(new TransactionCreatedEvent(
                response.transferId(), customerId, response.sourceAmount(), response.sourceCurrency(),
                CurrentCustomer.homeCurrency(), response.createdAt(), latitude, longitude));
        return response;
    }

    @GetMapping
    public TransfersResponse list() {
        return transferService.listTransfers(CurrentCustomer.id());
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return transferService.getTransfer(CurrentCustomer.id(), id);
    }
}
