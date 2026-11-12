package com.licenseleak.fees.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A deterministic fee result. Billable days must come from validated contract/evidence inputs. */
public record FeeQuote(
        BigDecimal amount,
        String currency,
        long billableDays,
        BigDecimal dailyRate,
        String ruleReference,
        LocalDate contractValidThrough,
        LocalDate adReceivedOn) {
}
