package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    private static final BigDecimal DAILY_RATE = new BigDecimal("0.0004");

    @Test
    void storesAmountAtTheCurrencyScale() {
        assertEquals("1200.00", Money.of("AED", "1200").amount().toPlainString());
        assertEquals("10.000", Money.of("BHD", "10").amount().toPlainString());
        assertEquals(Money.of("AED", "250.00"), Money.of("AED", "250"));
    }

    @Test
    void zeroSitsAtTheCurrencyScale() {
        assertEquals(Money.of("BHD", "0.000"), Money.zero(Currency.getInstance("BHD")));
        assertEquals(Money.of("AED", "0.00"), Money.zero(Currency.getInstance("AED")));
    }

    @Test
    void printsAsCurrencyCodeAndAmount() {
        assertEquals("AED 250.00", Money.of("AED", "250").toString());
        assertEquals("AED -230.00", Money.of("AED", "-230.00").toString());
        assertEquals("BHD 0.004", Money.of("BHD", "0.004").toString());
    }

    @Test
    void refusesMoreDecimalsThanTheCurrencyHolds() {
        assertThrows(ArithmeticException.class, () -> Money.of("AED", "1.005"));
        assertThrows(ArithmeticException.class, () -> Money.of("BHD", "1.0005"));
    }

    @Test
    void addsAndSubtractsWithinOneCurrency() {
        assertEquals(Money.of("AED", "250.00"), Money.of("AED", "1200.00").subtract(Money.of("AED", "950.00")));
        assertEquals(Money.of("AED", "650.00"), Money.of("AED", "250.00").add(Money.of("AED", "400.00")));
    }

    @Test
    void subtractionCanGoBelowZero() {
        assertEquals(Money.of("AED", "-370.00"), Money.of("AED", "250.00").subtract(Money.of("AED", "620.00")));
    }

    @Test
    void negateFlipsTheSign() {
        assertEquals(Money.of("AED", "-620.00"), Money.of("AED", "620.00").negate());
        assertEquals(Money.of("AED", "620.00"), Money.of("AED", "-620.00").negate());
    }

    @Test
    void onlyAmountsAboveZeroArePositive() {
        assertTrue(Money.of("AED", "0.01").isPositive());
        assertFalse(Money.of("AED", "0.00").isPositive());
        assertFalse(Money.of("AED", "-0.01").isPositive());
    }

    @Test
    void onlyAmountsBelowZeroAreNegative() {
        assertTrue(Money.of("AED", "-0.01").isNegative());
        assertFalse(Money.of("AED", "0.00").isNegative());
        assertFalse(Money.of("AED", "0.01").isNegative());
    }

    @Test
    void refusesToCombineCurrencies() {
        Money aed = Money.of("AED", "25.00");
        Money bhd = Money.of("BHD", "10.000");
        assertThrows(IllegalArgumentException.class, () -> bhd.add(aed));
        assertThrows(IllegalArgumentException.class, () -> bhd.subtract(aed));
    }

    @Test
    void multiplyingByARateRoundsToTheCurrencyScale() {
        assertEquals(Money.of("AED", "0.19"), Money.of("AED", "465.00").multiply(DAILY_RATE));
        assertEquals(Money.of("AED", "0.10"), Money.of("AED", "250.00").multiply(DAILY_RATE));
        assertEquals(Money.of("BHD", "0.004"), Money.of("BHD", "10.000").multiply(DAILY_RATE));
    }

    @Test
    void anExactHalfRoundsUp() {
        assertEquals(Money.of("AED", "0.05"), Money.of("AED", "112.50").multiply(DAILY_RATE));
    }

    @Test
    void splitsIntoPartsThatSumToTheWhole() {
        Money whole = Money.of("BHD", "10.000");

        List<Money> parts = whole.allocate(3);

        assertEquals(List.of(Money.of("BHD", "3.334"), Money.of("BHD", "3.333"), Money.of("BHD", "3.333")), parts);
        assertEquals(whole, parts.get(0).add(parts.get(1)).add(parts.get(2)));
    }

    @Test
    void spreadsASpareOfTwoAcrossTheFirstTwoParts() {
        assertEquals(
                List.of(Money.of("AED", "0.02"), Money.of("AED", "0.02"), Money.of("AED", "0.01")),
                Money.of("AED", "0.05").allocate(3));
    }

    @Test
    void refusesToSplitANegativeAmountOrIntoNoParts() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("BHD", "-10.000").allocate(3));
        assertThrows(IllegalArgumentException.class, () -> Money.of("BHD", "10.000").allocate(0));
    }
}
