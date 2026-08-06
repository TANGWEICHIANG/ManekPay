package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionCreatedEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedListener {

    public static final String TOPIC = "transaction.created";

    private final VelocityRuleService velocityRuleService;

    public TransactionCreatedListener(VelocityRuleService velocityRuleService) {
        this.velocityRuleService = velocityRuleService;
    }

    @KafkaListener(topics = TOPIC)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        velocityRuleService.evaluate(event);
    }
}
