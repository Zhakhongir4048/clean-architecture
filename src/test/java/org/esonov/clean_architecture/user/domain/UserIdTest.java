package org.esonov.clean_architecture.user.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void parsesAndPrintsCanonicalUuid() {
        String raw = "3f2b8c1e-1111-4222-8333-444455556666";

        UserId id = UserId.of(raw);

        assertThat(id.value()).isEqualTo(UUID.fromString(raw));
        assertThat(id).hasToString(raw);
        assertThat(id).isEqualTo(UserId.of(raw));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "garbage", "3f2b8c1e"})
    void invalidInputIsADomainError(String raw) {
        assertThatThrownBy(() -> UserId.of(raw)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void valueIsRequired() {
        assertThatNullPointerException().isThrownBy(() -> new UserId(null));
    }

    @Test
    void newIdsAreUnique() {
        assertThat(UserId.newId()).isNotEqualTo(UserId.newId());
    }
}
