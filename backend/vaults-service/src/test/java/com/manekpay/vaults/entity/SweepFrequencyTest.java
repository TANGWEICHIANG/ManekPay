package com.manekpay.vaults.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SweepFrequencyTest {

    @Test
    void dailyAdvancesByOneDay() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        assertThat(SweepFrequency.DAILY.nextRunAfter(from)).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void weeklyAdvancesBySevenDays() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        assertThat(SweepFrequency.WEEKLY.nextRunAfter(from)).isEqualTo(Instant.parse("2026-01-08T00:00:00Z"));
    }

    @Test
    void monthlyAdvancesByOneCalendarMonth() {
        Instant from = Instant.parse("2026-01-31T00:00:00Z");
        // Calendar-month arithmetic, not a fixed 30-day duration - Jan 31 + 1 month clamps to Feb 28.
        assertThat(SweepFrequency.MONTHLY.nextRunAfter(from)).isEqualTo(Instant.parse("2026-02-28T00:00:00Z"));
    }
}
