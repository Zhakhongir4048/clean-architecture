package org.esonov.clean_architecture.user.application.port.out;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.user.domain.EmailAddress;
import org.esonov.clean_architecture.user.domain.UserAccount;
import org.esonov.clean_architecture.user.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioural contract of the {@link UserAccountRepository} output port.
 * <p>
 * Every implementation — the real persistence adapter and the in-memory test double — extends this
 * class, so the fake used by use case tests is proven to behave like the database.
 */
public abstract class UserAccountRepositoryContract {

    // Microsecond precision: the finest resolution Postgres timestamptz keeps.
    protected static final Instant NOW = Instant.parse("2026-09-28T10:00:00.123456Z");

    protected abstract UserAccountRepository repository();

    @Test
    void saveThenFindByIdReturnsTheSameState() {
        UserAccount account = UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW);

        repository().save(account);

        UserAccount loaded = repository().findById(account.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(account.id());
        assertThat(loaded.email()).isEqualTo(account.email());
        assertThat(loaded.displayName()).isEqualTo("Ada");
        assertThat(loaded.registeredAt()).isEqualTo(NOW);
    }

    @Test
    void findByIdOfUnknownIdIsEmpty() {
        assertThat(repository().findById(UserId.newId())).isEmpty();
    }

    @Test
    void existsByEmailReflectsSavedAccounts() {
        repository().save(UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW));

        assertThat(repository().existsByEmail(new EmailAddress("ADA@example.com"))).isTrue();
        assertThat(repository().existsByEmail(new EmailAddress("bob@example.com"))).isFalse();
    }

    @Test
    void savingAnExistingAccountUpdatesIt() {
        UserAccount account = UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW);
        repository().save(account);

        account.rename("Countess of Lovelace");
        repository().save(account);

        assertThat(repository().findById(account.id()).orElseThrow().displayName()).isEqualTo("Countess of Lovelace");
    }

    @Test
    void unsavedChangesAreNotVisible() {
        UserAccount account = UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW);
        repository().save(account);

        account.rename("Changed but not saved");

        assertThat(repository().findById(account.id()).orElseThrow().displayName()).isEqualTo("Ada");
    }

    @Test
    void loadedObjectsAreIndependentCopies() {
        UserAccount account = UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW);
        repository().save(account);

        repository().findById(account.id()).orElseThrow().rename("Mutated copy");

        assertThat(repository().findById(account.id()).orElseThrow().displayName()).isEqualTo("Ada");
    }

    /** Must stay the last operation in the test: a real DB may abort the transaction afterwards. */
    @Test
    void duplicateEmailOnADifferentAccountIsRejected() {
        repository().save(UserAccount.register(new EmailAddress("ada@example.com"), "Ada", NOW));
        UserAccount duplicate = UserAccount.register(new EmailAddress("ada@example.com"), "Impostor", NOW);

        assertThatThrownBy(() -> repository().save(duplicate))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
