package com.manekpay.risk.service;

import com.manekpay.risk.dto.TransactionCreatedEvent;
import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LocationAnomalyRuleService {

    private static final Logger log = LoggerFactory.getLogger(LocationAnomalyRuleService.class);

    // Deliberately simple, illustrative threshold - not a real fraud-scoring model, matching
    // VelocityRuleService's own documented standard for this phase. Roughly commercial-jet
    // cruising speed, generous enough to avoid flagging real travel with a layover.
    static final double MAX_PLAUSIBLE_KMH = 1000.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StringRedisTemplate redisTemplate;
    private final RiskFlagRepository riskFlagRepository;
    private final RiskEventPublisher riskEventPublisher;

    public LocationAnomalyRuleService(StringRedisTemplate redisTemplate, RiskFlagRepository riskFlagRepository,
                                       RiskEventPublisher riskEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.riskFlagRepository = riskFlagRepository;
        this.riskEventPublisher = riskEventPublisher;
    }

    // No TTL on the location key, deliberately unlike VelocityRuleService's windowed keys -
    // "impossible travel" must always compare against the customer's actual last known location
    // no matter how long ago it was recorded; expiring it would silently disable the check after
    // any quiet period.
    public void evaluate(TransactionCreatedEvent event) {
        if (event.latitude() == null || event.longitude() == null) {
            return;
        }

        String key = "risk:location:" + event.customerId();
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null) {
            Location previous = parseLocation(stored, event.transactionId());
            if (previous != null) {
                if (event.occurredAt().toEpochMilli() < previous.epochMillis()) {
                    // Out-of-order delivery (clock skew across ledger-service instances, or a
                    // DLT replay) - not travel evidence. Return without touching the baseline,
                    // so a stale/replayed event can't rewind it backward for the next real
                    // comparison.
                    return;
                }
                evaluateAgainstPrevious(event, previous);
            }
        }

        redisTemplate.opsForValue().set(key, event.latitude() + "," + event.longitude() + "," + event.occurredAt().toEpochMilli());
    }

    private record Location(double latitude, double longitude, long epochMillis) {
    }

    // A malformed/legacy stored value is treated as "no baseline" rather than propagating an
    // exception - TransactionCreatedListener calls this and VelocityRuleService.evaluate from
    // the same method, so an uncaught exception here would also break velocity checking for
    // this event.
    private Location parseLocation(String stored, UUID transactionId) {
        try {
            String[] parts = stored.split(",");
            return new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Long.parseLong(parts[2]));
        } catch (RuntimeException e) {
            log.warn("Could not parse stored location for transaction {}, treating as no baseline: {}", transactionId, stored);
            return null;
        }
    }

    private void evaluateAgainstPrevious(TransactionCreatedEvent event, Location previous) {
        double distanceKm = haversineKm(previous.latitude(), previous.longitude(), event.latitude(), event.longitude());
        long elapsedMillis = event.occurredAt().toEpochMilli() - previous.epochMillis();

        String detail;
        if (elapsedMillis == 0) {
            // Same instant: a meaningfully nonzero jump is impossible regardless of speed, but
            // GPS jitter of a few metres between back-to-back reads is not evidence of anything.
            if (distanceKm < 1.0) {
                return;
            }
            detail = "%.1f km apart with no elapsed time between transactions".formatted(distanceKm);
        } else {
            double elapsedHours = elapsedMillis / 3_600_000.0;
            double impliedKmh = distanceKm / elapsedHours;
            if (impliedKmh <= MAX_PLAUSIBLE_KMH) {
                return;
            }
            detail = "%.1f km apart in %.1f minutes implies %.0f km/h".formatted(distanceKm, elapsedMillis / 60_000.0, impliedKmh);
        }

        try {
            riskFlagRepository.save(new RiskFlag(event.customerId(), event.transactionId(), "IMPOSSIBLE_TRAVEL", detail));
        } catch (DataIntegrityViolationException e) {
            log.debug("Location-anomaly flag for transaction {} already recorded, skipping redelivery", event.transactionId());
            return;
        }
        riskEventPublisher.publishTransactionFlagged(event.customerId(), event.transactionId(), "IMPOSSIBLE_TRAVEL", detail);
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
