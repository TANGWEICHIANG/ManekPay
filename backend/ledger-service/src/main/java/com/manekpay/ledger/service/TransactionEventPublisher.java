package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.TransactionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    public static final String TOPIC = "transaction.created";

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Fire-and-forget: a Kafka outage must never fail or roll back a transfer. The transfer's
    // own DB transaction (already committed by the time this is called - see TransferController)
    // is the source of truth for money movement; a failed publish is logged and otherwise
    // invisible to the caller.
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.customerId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction.created for transfer {}", event.transactionId(), ex);
                    }
                });
    }
}
