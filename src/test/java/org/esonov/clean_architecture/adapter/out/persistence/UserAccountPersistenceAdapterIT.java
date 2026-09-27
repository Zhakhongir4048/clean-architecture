package org.esonov.clean_architecture.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import org.esonov.clean_architecture.TestcontainersConfiguration;
import org.esonov.clean_architecture.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.domain.user.EmailAddress;
import org.esonov.clean_architecture.domain.user.UserAccount;
import org.esonov.clean_architecture.domain.user.UserId;

/**
 * Verifies the output-port contract against real Postgres with the Flyway schema:
 * mapping domain ⇄ JPA, and translation of the DB unique constraint into an application exception.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserAccountPersistenceAdapter.class})
class UserAccountPersistenceAdapterIT {

    // Postgres stores microseconds; truncate so round-trip equality is exact.
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    UserAccountPersistenceAdapter adapter;

    @Test
    void savesAndRestoresDomainObject() {
        UserAccount account = UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW);

        adapter.save(account);

        UserAccount loaded = adapter.findById(account.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(account.id());
        assertThat(loaded.email()).isEqualTo(new EmailAddress("ada@example.com"));
        assertThat(loaded.displayName()).isEqualTo("Ada");
        assertThat(loaded.registeredAt()).isEqualTo(NOW);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(adapter.findById(UserId.newId())).isEmpty();
    }

    @Test
    void existsByEmail() {
        adapter.save(UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW));

        assertThat(adapter.existsByEmail(new EmailAddress("ADA@example.com"))).isTrue();
        assertThat(adapter.existsByEmail(new EmailAddress("bob@example.com"))).isFalse();
    }

    @Test
    void uniqueConstraintIsTranslatedToApplicationException() {
        adapter.save(UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW));
        UserAccount duplicate = UserAccount.register(new EmailAddress("ada@example.com"), "Impostor", NOW);

        // Simulates the race where existsByEmail() passed for two concurrent registrations.
        assertThatThrownBy(() -> adapter.save(duplicate))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
