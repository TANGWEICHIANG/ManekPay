package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionCreatedEvent;
import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class VelocityRuleService {

    private static final Logger log = LoggerFactory.getLogger(VelocityRuleService.class);

    // Deliberately simple, illustrative thresholds - not a real fraud-scoring model. All 5
    // supported currencies (MYR/SGD/USD/EUR/GBP) are roughly comparable in magnitude, so a
    // single flat cross-currency threshold is an acceptable simplification for this phase.
    static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000");
    static final Duration WINDOW = Duration.ofSeconds(60);
    static final int MAX_HIGH_VALUE_IN_WINDOW = 5;

    private final StringRedisTemplate redisTemplate;
    private final RiskFlagRepository riskFlagRepository;
    private final RiskEventPublisher riskEventPublisher;

    public VelocityRuleService(StringRedisTemplate redisTemplate, RiskFlagRepository riskFlagRepository,
                                RiskEventPublisher riskEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.riskFlagRepository = riskFlagRepository;
        this.riskEventPublisher = riskEventPublisher;
    }

    // No extra locking needed: TransactionEventPublisher (ledger-service) keys every message by
    // customerId, so Kafka guarantees one customer's events land on the same partition and are
    // consumed strictly in order by a single thread. ZADD is naturally idempotent on redelivery
    // of the same transactionId (it upserts the member's score rather than duplicating it), and
    // the unique(transaction_id, rule) constraint below guards the flag+publish step itself.
    public void evaluate(TransactionCreatedEvent event) {
        if (event.amount().compareTo(HIGH_VALUE_THRESHOLD) < 0) {
            return;
        }
        String key = "risk:velocity:" + event.customerId();
        double score = event.occurredAt().toEpochMilli();
        redisTemplate.opsForZSet().add(key, event.transactionId().toString(), score);
        // Prune relative to wall-clock now, not the just-added event's own score - anchoring to
        // the event's score means a stale/replayed event (e.g. manually reprocessed off the
        // transaction.created.DLT) can resurrect itself into the live window without ever being
        // pruned, since its own prune call can't reach forward past its own timestamp.
        long nowMillis = Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, nowMillis - WINDOW.toMillis());
        redisTemplate.expire(key, WINDOW);

        Long count = redisTemplate.opsForZSet().zCard(key);
        if (count == null || count <= MAX_HIGH_VALUE_IN_WINDOW) {
            return;
        }

        String detail = count + " high-value transfers within the last 60 seconds";
        try {
            riskFlagRepository.save(new RiskFlag(event.customerId(), event.transactionId(), "VELOCITY", detail));
        } catch (DataIntegrityViolationException e) {
            log.debug("Velocity flag for transaction {} already recorded, skipping redelivery", event.transactionId());
            return;
        }
        riskEventPublisher.publishTransactionFlagged(event.customerId(), event.transactionId(), "VELOCITY", detail);
    }
}
