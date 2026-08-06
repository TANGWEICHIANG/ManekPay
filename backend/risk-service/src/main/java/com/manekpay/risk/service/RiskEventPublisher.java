package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionFlaggedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RiskEventPublisher {

    public static final String TOPIC = "transaction.flagged";

    private static final Logger log = LoggerFactory.getLogger(RiskEventPublisher.class);

    private final KafkaTemplate<String, TransactionFlaggedEvent> kafkaTemplate;

    public RiskEventPublisher(KafkaTemplate<String, TransactionFlaggedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Fire-and-forget, same pattern as ledger-service's TransactionEventPublisher: a Kafka
    // outage must never fail the velocity check itself. The RiskFlag row (already committed by
    // the time this is called - see VelocityRuleService) is the source of truth for the flag; a
    // failed publish is logged and otherwise invisible to the caller.
    public void publishTransactionFlagged(UUID customerId, UUID transactionId, String rule, String detail) {
        TransactionFlaggedEvent event = new TransactionFlaggedEvent(transactionId, customerId, rule, detail, Instant.now());
        kafkaTemplate.send(TOPIC, customerId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction.flagged for transaction {}", transactionId, ex);
                    }
                });
    }
}
