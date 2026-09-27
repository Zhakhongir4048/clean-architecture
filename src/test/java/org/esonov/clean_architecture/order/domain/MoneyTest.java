package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Test
    void isNormalisedToCurrencyPrecision() {
        assertThat(new Money(new BigDecimal("12.5"), USD).amount()).isEqualByComparingTo("12.50").hasToString("12.50");
        assertThat(new Money(new BigDecimal("1200.0000"), JPY).amount()).hasToString("1200");
    }

    @Test
    void equalityIgnoresInputScale() {
        assertThat(new Money(new BigDecimal("12.5"), USD)).isEqualTo(new Money(new BigDecimal("12.5000"), USD));
    }

    @Test
    void rejectsMorePrecisionThanTheCurrencyHas() {
        assertThatThrownBy(() -> new Money(new BigDecimal("12.505"), USD))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("more than 2 decimal places");
        assertThatThrownBy(() -> new Money(new BigDecimal("1.5"), JPY))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNegativeAndMissingAmounts() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), USD)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new Money(null, USD)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void zeroIsAllowed() {
        assertThat(Money.zero(USD).amount()).hasToString("0.00");
    }

    @Test
    void arithmetic() {
        Money price = new Money(new BigDecimal("2.50"), USD);

        assertThat(price.times(3)).isEqualTo(new Money(new BigDecimal("7.50"), USD));
        assertThat(price.plus(price)).isEqualTo(new Money(new BigDecimal("5.00"), USD));
    }

    @Test
    void cannotCombineCurrencies() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, USD).plus(new Money(BigDecimal.ONE, JPY)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Cannot combine USD and JPY");
    }

    @Test
    void parsesCurrencyCodesCaseInsensitively() {
        assertThat(Money.parseCurrency(" usd ")).isEqualTo(USD);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "XYZ", "dollars"})
    void rejectsUnknownCurrencies(String code) {
        assertThatThrownBy(() -> Money.parseCurrency(code)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsCurrenciesWithoutDecimalPrecision() {
        // XAU (gold) has no defined fraction digits, so "an amount of gold" is not money in this domain.
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, Currency.getInstance("XAU")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not supported");
    }
}
