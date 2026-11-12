package com.licenseleak.fees.application;

import com.licenseleak.fees.api.FeeQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Calculates the flat daily fee for calendar days beyond the contract period. */
@Service
public class DailyFeeCalculator {

    private final BigDecimal dailyRate;
    private final String currency;

    public DailyFeeCalculator(
            @Value("${licenseleak.fee.daily-rate:10.00}") BigDecimal dailyRate,
            @Value("${licenseleak.fee.currency:USD}") String currency) {
        if (dailyRate == null || dailyRate.signum() <= 0) {
            throw new IllegalArgumentException("The configured daily fee rate must be greater than zero");
        }
        Currency.getInstance(currency);
        this.dailyRate = dailyRate.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * Bills each calendar day after the contract's final valid date through the date
     * the out-of-contract ad is received, inclusive of that receipt date.
     */
    public FeeQuote calculate(LocalDate contractValidThrough, LocalDate adReceivedOn) {
        if (contractValidThrough == null || adReceivedOn == null) {
            throw new IllegalArgumentException("Contract end date and ad receipt date are required");
        }

        long billableDays = Math.max(0, ChronoUnit.DAYS.between(contractValidThrough, adReceivedOn));
        BigDecimal amount = dailyRate
                .multiply(BigDecimal.valueOf(billableDays))
                .setScale(2, RoundingMode.HALF_UP);

        return new FeeQuote(
                amount,
                currency,
                billableDays,
                dailyRate,
                "DAILY_RATE_PER_BILLABLE_DAY",
                contractValidThrough,
                adReceivedOn);
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public String getCurrency() {
        return currency;
    }
}
