package org.esonov.clean_architecture.user.application.usecase;

import java.util.Objects;

import org.esonov.clean_architecture.user.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.user.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.user.application.port.in.UserAccountView;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.esonov.clean_architecture.user.domain.UserId;

public class GetUserAccountInteractor implements GetUserAccountQuery {

    private final UserAccountRepository userAccounts;

    public GetUserAccountInteractor(UserAccountRepository userAccounts) {
        this.userAccounts = Objects.requireNonNull(userAccounts, "userAccounts");
    }

    @Override
    public UserAccountView getById(String userId) {
        UserId id;
        try {
            id = UserId.of(userId);
        } catch (DomainValidationException e) {
            throw new UserAccountNotFoundException(userId);
        }
        return userAccounts.findById(id)
                .map(UserAccountViews::from)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
    }
}
