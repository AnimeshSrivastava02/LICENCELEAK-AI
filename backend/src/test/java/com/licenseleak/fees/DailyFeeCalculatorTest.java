package com.licenseleak.fees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.licenseleak.fees.application.DailyFeeCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyFeeCalculatorTest {

    private final DailyFeeCalculator calculator = new DailyFeeCalculator(new BigDecimal("10.00"), "USD");

    @Test
    void chargesEachCalendarDayAfterContractThroughReceiptDate() {
        var quote = calculator.calculate(LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-03"));

        assertEquals(3, quote.billableDays());
        assertEquals(new BigDecimal("30.00"), quote.amount());
        assertEquals(LocalDate.parse("2026-06-30"), quote.contractValidThrough());
        assertEquals(LocalDate.parse("2026-07-03"), quote.adReceivedOn());
    }

    @Test
    void returnsZeroWhenAdWasReceivedOnOrBeforeContractExpiry() {
        assertEquals(new BigDecimal("0.00"),
                calculator.calculate(LocalDate.parse("2026-06-30"), LocalDate.parse("2026-06-30")).amount());
        assertEquals(new BigDecimal("0.00"),
                calculator.calculate(LocalDate.parse("2026-06-30"), LocalDate.parse("2026-06-28")).amount());
    }

    @Test
    void refusesMissingDates() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null, LocalDate.parse("2026-07-03")));
    }
}
