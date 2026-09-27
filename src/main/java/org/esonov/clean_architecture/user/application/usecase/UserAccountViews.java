package org.esonov.clean_architecture.user.application.usecase;

import org.esonov.clean_architecture.user.application.port.in.UserAccountView;
import org.esonov.clean_architecture.user.domain.UserAccount;

/** Maps the entity to the input boundary's response model. */
final class UserAccountViews {

    private UserAccountViews() {
    }

    static UserAccountView from(UserAccount account) {
        return new UserAccountView(
                account.id().toString(),
                account.email().value(),
                account.displayName(),
                account.registeredAt());
    }
}
