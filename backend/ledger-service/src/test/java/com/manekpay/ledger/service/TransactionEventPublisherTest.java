package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.TransactionCreatedEvent;
import com.manekpay.ledger.entity.Currency;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = TransactionEventPublisher.TOPIC)
@ExtendWith(EmbeddedKafkaCondition.class)
class TransactionEventPublisherTest {

    private final EmbeddedKafkaBroker embeddedKafkaBroker = EmbeddedKafkaCondition.getBroker();

    @Test
    void publishesAnEventThatCanBeConsumedBack() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        TransactionEventPublisher publisher = new TransactionEventPublisher(kafkaTemplate);

        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("42.5000"), Currency.MYR, Currency.SGD, Instant.now(), null, null);

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.manekpay.ledger.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionCreatedEvent.class.getName());

        try (Consumer<String, TransactionCreatedEvent> consumer = new KafkaConsumer<>(consumerProps)) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TransactionEventPublisher.TOPIC);

            publisher.publishTransactionCreated(event);

            ConsumerRecord<String, TransactionCreatedEvent> received =
                    KafkaTestUtils.getSingleRecord(consumer, TransactionEventPublisher.TOPIC, Duration.ofSeconds(10));

            assertThat(received.key()).isEqualTo(customerId.toString());
            assertThat(received.value().transactionId()).isEqualTo(transactionId);
            assertThat(received.value().customerId()).isEqualTo(customerId);
            assertThat(received.value().amount()).isEqualByComparingTo("42.5000");
            assertThat(received.value().currency()).isEqualTo(Currency.MYR);
            assertThat(received.value().homeCurrency()).isEqualTo(Currency.SGD);
        }
    }
}
