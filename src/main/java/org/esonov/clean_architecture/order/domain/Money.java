package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object: a non-negative amount in a currency, always at the currency's precision
 * (USD 12.50, JPY 1200). Arithmetic across currencies is rejected.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (amount == null) {
            throw new DomainValidationException("Amount is required");
        }
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new DomainValidationException("Amount must not be negative");
        }
        int digits = currency.getDefaultFractionDigits();
        if (digits < 0) {
            throw new DomainValidationException("Currency " + currency + " is not supported");
        }
        try {
            amount = amount.setScale(digits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new DomainValidationException(
                    "Amount " + amount.toPlainString() + " has more than " + digits + " decimal places for " + currency);
        }
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * @throws DomainValidationException if {@code code} is not an ISO 4217 currency code
     */
    public static Currency parseCurrency(String code) {
        if (code == null || code.isBlank()) {
            throw new DomainValidationException("Currency is required");
        }
        try {
            return Currency.getInstance(code.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Unknown currency: " + code);
        }
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainValidationException("Cannot combine " + currency + " and " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
