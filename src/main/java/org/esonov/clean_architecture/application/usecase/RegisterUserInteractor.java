package org.esonov.clean_architecture.application.usecase;

import java.time.Clock;
import java.util.Objects;

import org.esonov.clean_architecture.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.application.port.in.RegisterUserCommand;
import org.esonov.clean_architecture.application.port.in.RegisterUserUseCase;
import org.esonov.clean_architecture.application.port.in.UserAccountView;
import org.esonov.clean_architecture.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.domain.user.EmailAddress;
import org.esonov.clean_architecture.domain.user.UserAccount;

/**
 * Application business rule. Framework-free: no Spring annotations — it is wired
 * as a bean by {@code config.UseCaseConfig} (the Main component).
 */
public class RegisterUserInteractor implements RegisterUserUseCase {

    private final UserAccountRepository userAccounts;
    private final Clock clock;

    public RegisterUserInteractor(UserAccountRepository userAccounts, Clock clock) {
        this.userAccounts = Objects.requireNonNull(userAccounts, "userAccounts");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public UserAccountView register(RegisterUserCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        if (userAccounts.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email.value());
        }
        UserAccount account = UserAccount.register(email, command.displayName(), clock.instant());
        userAccounts.save(account);
        return UserAccountViews.from(account);
    }
}
