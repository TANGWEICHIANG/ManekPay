package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionFlaggedEvent;

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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = RiskEventPublisher.TOPIC)
@ExtendWith(EmbeddedKafkaCondition.class)
class RiskEventPublisherTest {

    private final EmbeddedKafkaBroker embeddedKafkaBroker = EmbeddedKafkaCondition.getBroker();

    @Test
    void publishesAnEventThatCanBeConsumedBack() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        KafkaTemplate<String, TransactionFlaggedEvent> kafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        RiskEventPublisher publisher = new RiskEventPublisher(kafkaTemplate);

        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.manekpay.risk.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionFlaggedEvent.class.getName());

        try (Consumer<String, TransactionFlaggedEvent> consumer = new KafkaConsumer<>(consumerProps)) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, RiskEventPublisher.TOPIC);

            publisher.publishTransactionFlagged(customerId, transactionId, "VELOCITY", "6 high-value transfers within the last 60 seconds");

            ConsumerRecord<String, TransactionFlaggedEvent> received =
                    KafkaTestUtils.getSingleRecord(consumer, RiskEventPublisher.TOPIC, Duration.ofSeconds(10));

            assertThat(received.key()).isEqualTo(customerId.toString());
            assertThat(received.value().transactionId()).isEqualTo(transactionId);
            assertThat(received.value().customerId()).isEqualTo(customerId);
            assertThat(received.value().rule()).isEqualTo("VELOCITY");
            assertThat(received.value().detail()).isEqualTo("6 high-value transfers within the last 60 seconds");
        }
    }
}
