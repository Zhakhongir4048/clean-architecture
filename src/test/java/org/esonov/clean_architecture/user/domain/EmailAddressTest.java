package org.esonov.clean_architecture.user.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void isNormalisedToTrimmedLowerCase() {
        assertThat(new EmailAddress("  Ada.Lovelace@Example.COM ").value()).isEqualTo("ada.lovelace@example.com");
    }

    @Test
    void equalityIsByNormalisedValue() {
        assertThat(new EmailAddress("ADA@example.com")).isEqualTo(new EmailAddress("ada@EXAMPLE.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ada@example.com", "a.b+tag@sub.example.org", "x@y.io"})
    void acceptsValidAddresses(String value) {
        assertThat(new EmailAddress(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> new EmailAddress(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Email must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@example.com", "ada@", "ada@example", "ada@@example.com", "ada lovelace@example.com"})
    void rejectsMalformed(String value) {
        assertThatThrownBy(() -> new EmailAddress(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Email is not a valid address");
    }

    @Test
    void lengthLimitIs254() {
        String domain = "@example.com";
        String max = "a".repeat(254 - domain.length()) + domain;

        assertThat(new EmailAddress(max).value()).hasSize(254);
        assertThatThrownBy(() -> new EmailAddress("a" + max))
                .isInstanceOf(DomainValidationException.class);
    }
}
