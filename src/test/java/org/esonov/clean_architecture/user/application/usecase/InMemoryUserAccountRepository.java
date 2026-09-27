package org.esonov.clean_architecture.user.application.usecase;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.user.domain.EmailAddress;
import org.esonov.clean_architecture.user.domain.UserAccount;
import org.esonov.clean_architecture.user.domain.UserId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test double proving the use cases run without any database or framework.
 * <p>
 * Behaves like the real adapter (verified by {@link InMemoryUserAccountRepositoryTest}): stores
 * snapshots rather than live references, and enforces email uniqueness like the DB constraint.
 */
class InMemoryUserAccountRepository implements UserAccountRepository {

    final Map<UserId, UserAccount> store = new LinkedHashMap<>();

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryUserAccountRepository::copy);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return store.values().stream().anyMatch(a -> a.email().equals(email));
    }

    @Override
    public void save(UserAccount account) {
        boolean emailTakenByAnother = store.values().stream()
                .anyMatch(a -> a.email().equals(account.email()) && !a.id().equals(account.id()));
        if (emailTakenByAnother) {
            throw new EmailAlreadyRegisteredException(account.email().value());
        }
        store.put(account.id(), copy(account));
    }

    private static UserAccount copy(UserAccount a) {
        return UserAccount.restore(a.id(), a.email(), a.displayName(), a.registeredAt());
    }
}
