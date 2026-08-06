package com.manekpay.vaults.service;

import com.manekpay.vaults.dto.TransactionCreatedEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedListener {

    public static final String TOPIC = "transaction.created";

    private final VaultService vaultService;

    public TransactionCreatedListener(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @KafkaListener(topics = TOPIC)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        vaultService.applyRoundUp(event);
    }
}
