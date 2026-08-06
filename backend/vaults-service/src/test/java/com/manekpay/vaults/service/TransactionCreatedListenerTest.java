package com.manekpay.vaults.service;

import com.manekpay.vaults.dto.TransactionCreatedEvent;
import com.manekpay.vaults.entity.Currency;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.verify;

@EmbeddedKafka(partitions = 1, topics = TransactionCreatedListener.TOPIC)
@ExtendWith(EmbeddedKafkaCondition.class)
class TransactionCreatedListenerTest {

    private final EmbeddedKafkaBroker embeddedKafkaBroker = EmbeddedKafkaCondition.getBroker();

    @Test
    void deserializesAPublishedEventAndInvokesVaultService() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("12.3000"), Currency.MYR, Currency.MYR, Instant.now());

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("vaults-service-test", "true", embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.manekpay.vaults.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionCreatedEvent.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        try (Consumer<String, TransactionCreatedEvent> consumer = new KafkaConsumer<>(consumerProps)) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TransactionCreatedListener.TOPIC);

            kafkaTemplate.send(TransactionCreatedListener.TOPIC, customerId.toString(), event);

            ConsumerRecord<String, TransactionCreatedEvent> received =
                    KafkaTestUtils.getSingleRecord(consumer, TransactionCreatedListener.TOPIC, Duration.ofSeconds(10));

            assertThat(received.value().transactionId()).isEqualTo(transactionId);
            assertThat(received.value().customerId()).isEqualTo(customerId);
            assertThat(received.value().amount()).isEqualByComparingTo("12.3000");
            assertThat(received.value().currency()).isEqualTo(Currency.MYR);
            assertThat(received.value().homeCurrency()).isEqualTo(Currency.MYR);

            VaultService vaultService = Mockito.mock(VaultService.class);
            TransactionCreatedListener listener = new TransactionCreatedListener(vaultService);
            listener.onTransactionCreated(received.value());

            verify(vaultService).applyRoundUp(received.value());
        }
    }
}
