package io.mal.ledger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public record Money(Currency currency, BigDecimal amount) {

    public Money {
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
    }

    public static Money of(String currencyCode, String amount) {
        return new Money(Currency.getInstance(currencyCode), new BigDecimal(amount));
    }

    public static Money zero(Currency currency) {
        return new Money(currency, BigDecimal.ZERO);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(currency, amount.add(other.amount));
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(currency, amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(currency, amount.negate());
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money multiply(BigDecimal rate) {
        return new Money(currency, amount.multiply(rate).setScale(amount.scale(), RoundingMode.HALF_UP));
    }

    public List<Money> allocate(int parts) {
        if (parts < 1) {
            throw new IllegalArgumentException("Cannot split into " + parts + " parts");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Cannot split a negative amount: " + amount);
        }
        BigInteger[] split = amount.unscaledValue().divideAndRemainder(BigInteger.valueOf(parts));
        int spare = split[1].intValueExact();
        List<Money> result = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            BigInteger units = i < spare ? split[0].add(BigInteger.ONE) : split[0];
            result.add(new Money(currency, new BigDecimal(units, amount.scale())));
        }
        return List.copyOf(result);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot combine " + currency + " with " + other.currency);
        }
    }
}
