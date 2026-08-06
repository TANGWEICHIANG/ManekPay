package com.manekpay.risk.config;

import com.manekpay.risk.service.RiskEventPublisher;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic transactionFlaggedTopic() {
        return TopicBuilder.name(RiskEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Three retries, 1 second apart; after that the record is published to
    // transaction.created.DLT (Spring Kafka's default dead-letter naming convention) instead of
    // blocking the consumer indefinitely or silently dropping the message.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
