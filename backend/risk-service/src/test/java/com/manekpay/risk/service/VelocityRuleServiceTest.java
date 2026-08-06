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
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
