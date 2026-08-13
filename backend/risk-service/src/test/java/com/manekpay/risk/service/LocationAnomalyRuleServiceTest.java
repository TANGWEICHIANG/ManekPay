package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionCreatedEvent;
import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationAnomalyRuleServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RiskFlagRepository riskFlagRepository;
    @Mock
    private RiskEventPublisher riskEventPublisher;

    @Test
    void skipsEventsWithNoLocation() {
        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.0000"), null, null, Instant.now(), null, null);

        service.evaluate(event);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void recordsABaselineWithoutFlaggingOnTheCustomersFirstLocatedTransaction() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        UUID customerId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, Instant.now(), 3.139, 101.6869);

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
        verify(valueOps).set(eq("risk:location:" + customerId), eq("3.139,101.6869," + event.occurredAt().toEpochMilli()));
    }

    @Test
    void doesNotFlagWhenImpliedSpeedIsPlausible() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        Instant earlier = Instant.now().minusSeconds(3600);
        // Kuala Lumpur -> Singapore, ~300km, over 1 hour = ~300 km/h, well under the threshold.
        when(valueOps.get("risk:location:" + customerId)).thenReturn("3.139,101.6869," + earlier.toEpochMilli());

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, Instant.now(), 1.3521, 103.8198);

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
    }

    @Test
    void flagsAndPublishesWhenImpliedSpeedExceedsTheThreshold() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Instant earlier = Instant.now().minusSeconds(60);
        // Kuala Lumpur -> Singapore, ~300km, in 1 minute -> far beyond any plausible speed.
        when(valueOps.get("risk:location:" + customerId)).thenReturn("3.139,101.6869," + earlier.toEpochMilli());

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("10.0000"), null, null, Instant.now(), 1.3521, 103.8198);

        service.evaluate(event);

        verify(riskFlagRepository).save(any(RiskFlag.class));
        verify(riskEventPublisher).publishTransactionFlagged(eq(customerId), eq(transactionId), eq("IMPOSSIBLE_TRAVEL"), anyString());
    }

    @Test
    void flagsInstantlyWhenElapsedTimeIsZeroAndDistanceIsNonzero() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();
        when(valueOps.get("risk:location:" + customerId)).thenReturn("3.139,101.6869," + now.toEpochMilli());

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, now, 1.3521, 103.8198);

        service.evaluate(event);

        verify(riskFlagRepository).save(any(RiskFlag.class));
    }

    @Test
    void doesNotFlagWhenElapsedTimeAndDistanceAreBothZero() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();
        when(valueOps.get("risk:location:" + customerId)).thenReturn("3.139,101.6869," + now.toEpochMilli());

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, now, 3.139, 101.6869);

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
    }

    @Test
    void doesNotFlagAndDoesNotRewindBaselineWhenEventIsOlderThanStoredBaseline() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        Instant stored = Instant.now();
        Instant older = stored.minusSeconds(60);
        when(valueOps.get("risk:location:" + customerId)).thenReturn("1.3521,103.8198," + stored.toEpochMilli());

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, older, 3.139, 101.6869);

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
        verify(valueOps, never()).set(anyString(), anyString());
    }

    @Test
    void doesNotFlagAndDoesNotThrowWhenStoredValueIsMalformed() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        when(valueOps.get("risk:location:" + customerId)).thenReturn("garbage");

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, Instant.now(), 1.3521, 103.8198);

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
    }

    @Test
    void skipsPublishingWhenTheFlagWasAlreadyRecorded() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID customerId = UUID.randomUUID();
        Instant earlier = Instant.now().minusSeconds(60);
        when(valueOps.get("risk:location:" + customerId)).thenReturn("3.139,101.6869," + earlier.toEpochMilli());
        when(riskFlagRepository.save(any(RiskFlag.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        LocationAnomalyRuleService service = new LocationAnomalyRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("10.0000"), null, null, Instant.now(), 1.3521, 103.8198);

        service.evaluate(event);

        verify(riskEventPublisher, never()).publishTransactionFlagged(any(), any(), anyString(), anyString());
    }

    @Test
    void haversineDistanceBetweenKualaLumpurAndSingaporeIsApproximatelyCorrect() {
        double distanceKm = LocationAnomalyRuleService.haversineKm(3.139, 101.6869, 1.3521, 103.8198);
        assertThat(distanceKm).isCloseTo(315.0, within(50.0));
    }
}
