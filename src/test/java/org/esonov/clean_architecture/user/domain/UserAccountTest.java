package org.esonov.clean_architecture.user.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure entity tests: no mocks, no Spring, no database. */
class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final EmailAddress EMAIL = new EmailAddress("ada@example.com");

    @Test
    void registerCreatesAccountWithFreshIdentity() {
        UserAccount account = UserAccount.register(EMAIL, "Ada", NOW);

        assertThat(account.id()).isNotNull();
        assertThat(account.email()).isEqualTo(EMAIL);
        assertThat(account.displayName()).isEqualTo("Ada");
        assertThat(account.registeredAt()).isEqualTo(NOW);
    }

    @Test
    void everyRegistrationGetsADistinctId() {
        UserAccount first = UserAccount.register(EMAIL, "Ada", NOW);
        UserAccount second = UserAccount.register(EMAIL, "Ada", NOW);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void displayNameIsTrimmed() {
        assertThat(UserAccount.register(EMAIL, "  Ada Lovelace \t", NOW).displayName()).isEqualTo("Ada Lovelace");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void blankDisplayNameIsRejected(String displayName) {
        assertThatThrownBy(() -> UserAccount.register(EMAIL, displayName, NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Display name must not be blank");
    }

    @Test
    void displayNameLengthLimitIsInclusiveAt100AfterTrimming() {
        String max = "a".repeat(100);

        assertThat(UserAccount.register(EMAIL, "  " + max + "  ", NOW).displayName()).isEqualTo(max);
        assertThatThrownBy(() -> UserAccount.register(EMAIL, max + "a", NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at most 100");
    }

    @Test
    void requiredPartsMustBePresent() {
        UserId id = UserId.newId();

        assertThatNullPointerException().isThrownBy(() -> UserAccount.register(null, "Ada", NOW));
        assertThatNullPointerException().isThrownBy(() -> UserAccount.register(EMAIL, "Ada", null));
        assertThatNullPointerException().isThrownBy(() -> UserAccount.restore(null, EMAIL, "Ada", NOW));
        assertThatNullPointerException().isThrownBy(() -> UserAccount.restore(id, null, "Ada", NOW));
    }

    @Test
    void restoreKeepsGivenIdentityAndState() {
        UserId id = new UserId(UUID.fromString("3f2b8c1e-1111-4222-8333-444455556666"));

        UserAccount account = UserAccount.restore(id, EMAIL, "Ada", NOW);

        assertThat(account.id()).isEqualTo(id);
        assertThat(account.registeredAt()).isEqualTo(NOW);
    }

    @Test
    void restoreStillEnforcesInvariants() {
        assertThatThrownBy(() -> UserAccount.restore(UserId.newId(), EMAIL, " ", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void renameAppliesTheSameRulesAndKeepsStateOnFailure() {
        UserAccount account = UserAccount.register(EMAIL, "Ada", NOW);

        account.rename("  Countess of Lovelace ");
        assertThat(account.displayName()).isEqualTo("Countess of Lovelace");

        assertThatThrownBy(() -> account.rename(""))
                .isInstanceOf(DomainValidationException.class);
        assertThat(account.displayName()).isEqualTo("Countess of Lovelace");
    }

    @Test
    void equalityIsByIdentityOnly() {
        UserId id = UserId.newId();
        UserAccount a = UserAccount.restore(id, EMAIL, "Ada", NOW);
        UserAccount sameIdDifferentState = UserAccount.restore(id, new EmailAddress("other@example.com"), "Other", NOW.plusSeconds(60));
        UserAccount sameStateDifferentId = UserAccount.restore(UserId.newId(), EMAIL, "Ada", NOW);

        assertThat(a).isEqualTo(sameIdDifferentState).hasSameHashCodeAs(sameIdDifferentState);
        assertThat(a).isNotEqualTo(sameStateDifferentId);
        assertThat(a).isNotEqualTo(null);
    }
}
