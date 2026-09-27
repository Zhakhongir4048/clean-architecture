package org.esonov.clean_architecture.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.user.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.user.application.port.in.RegisterUserCommand;
import org.esonov.clean_architecture.user.application.port.in.UserAccountView;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;

class UserAccountUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");

    private final InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
    private final RegisterUserInteractor registerUser =
            new RegisterUserInteractor(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    private final GetUserAccountInteractor getUserAccount = new GetUserAccountInteractor(repository);

    @Test
    void registersAndReturnsPlainView() {
        UserAccountView view = registerUser.register(new RegisterUserCommand("  Ada@Example.com ", " Ada "));

        assertThat(view.email()).isEqualTo("ada@example.com");
        assertThat(view.displayName()).isEqualTo("Ada");
        assertThat(view.registeredAt()).isEqualTo(NOW);
        assertThat(repository.store).hasSize(1);
        assertThat(getUserAccount.getById(view.id())).isEqualTo(view);
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitively() {
        registerUser.register(new RegisterUserCommand("ada@example.com", "Ada"));

        assertThatThrownBy(() -> registerUser.register(new RegisterUserCommand("ADA@example.com", "Other")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void rejectsInvalidData() {
        assertThatThrownBy(() -> registerUser.register(new RegisterUserCommand("not-an-email", "Ada")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> registerUser.register(new RegisterUserCommand("ada@example.com", "  ")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(repository.store).isEmpty();
    }

    @Test
    void unknownOrMalformedIdIsNotFound() {
        assertThatThrownBy(() -> getUserAccount.getById("00000000-0000-0000-0000-000000000000"))
                .isInstanceOf(UserAccountNotFoundException.class);
        assertThatThrownBy(() -> getUserAccount.getById("garbage"))
                .isInstanceOf(UserAccountNotFoundException.class);
    }
}
