package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionCreatedEvent;
import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityRuleServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RiskFlagRepository riskFlagRepository;
    @Mock
    private RiskEventPublisher riskEventPublisher;

    @Test
    void skipsTransactionsBelowTheHighValueThreshold() {
        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500.0000"), null, null, Instant.now());

        service.evaluate(event);

        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void doesNotFlagWhenCountStaysAtOrBelowTheLimit() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(5L);

        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1500.0000"), null, null, Instant.now());

        service.evaluate(event);

        verify(riskFlagRepository, never()).save(any());
        verify(riskEventPublisher, never()).publishTransactionFlagged(any(), any(), anyString(), anyString());
    }

    @Test
    void flagsAndPublishesWhenCountExceedsTheLimit() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(6L);

        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("1500.0000"), null, null, Instant.now());

        service.evaluate(event);

        verify(riskFlagRepository).save(any(RiskFlag.class));
        verify(riskEventPublisher).publishTransactionFlagged(eq(customerId), eq(transactionId), eq("VELOCITY"), anyString());
    }

    @Test
    void setsATtlOnTheVelocityKeySoInactiveCustomersDontLeakRedisMemoryForever() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(1L);

        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        UUID customerId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("1500.0000"), null, null, Instant.now());

        service.evaluate(event);

        verify(redisTemplate).expire("risk:velocity:" + customerId, VelocityRuleService.WINDOW);
    }

    @Test
    void prunesRelativeToNowSoAStaleReplayedEventCannotResurrectAnExpiredWindow() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(1L);

        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        UUID customerId = UUID.randomUUID();
        // An event whose own occurredAt is well outside the 60s window (simulating a stale
        // redelivery/DLT replay) must still be pruned relative to real time, not its own score -
        // the buggy version anchored the prune boundary to the event's own score, which would
        // compute a boundary ~10 minutes in the past and fail to prune anything close to now.
        Instant staleTimestamp = Instant.now().minus(Duration.ofMinutes(10));
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("1500.0000"), null, null, staleTimestamp);

        service.evaluate(event);

        ArgumentCaptor<Double> boundaryCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOps).removeRangeByScore(eq("risk:velocity:" + customerId), eq(0.0), boundaryCaptor.capture());
        double expectedBoundary = Instant.now().minus(VelocityRuleService.WINDOW).toEpochMilli();
        assertThat(boundaryCaptor.getValue()).isCloseTo(expectedBoundary, org.assertj.core.data.Offset.offset(5000.0));
    }

    @Test
    void skipsPublishingWhenTheFlagWasAlreadyRecorded() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(6L);
        when(riskFlagRepository.save(any(RiskFlag.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        VelocityRuleService service = new VelocityRuleService(redisTemplate, riskFlagRepository, riskEventPublisher);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1500.0000"), null, null, Instant.now());

        service.evaluate(event);

        verify(riskEventPublisher, never()).publishTransactionFlagged(any(), any(), anyString(), anyString());
    }
}
