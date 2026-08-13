package com.manekpay.vaults.entity;

import java.time.Instant;
import java.time.ZoneOffset;

// Instant has no calendar-aware plus(1, MONTHS) - route through ZonedDateTime(UTC) so MONTHLY
// does real calendar-month arithmetic (e.g. Jan 31 -> Feb 28) instead of a fixed-duration guess.
public enum SweepFrequency {
    DAILY {
        @Override
        public Instant nextRunAfter(Instant from) {
            return from.atZone(ZoneOffset.UTC).plusDays(1).toInstant();
        }
    },
    WEEKLY {
        @Override
        public Instant nextRunAfter(Instant from) {
            return from.atZone(ZoneOffset.UTC).plusWeeks(1).toInstant();
        }
    },
    MONTHLY {
        @Override
        public Instant nextRunAfter(Instant from) {
            return from.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        }
    };

    public abstract Instant nextRunAfter(Instant from);
}
