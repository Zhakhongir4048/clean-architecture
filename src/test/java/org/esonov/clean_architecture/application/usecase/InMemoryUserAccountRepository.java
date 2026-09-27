package org.esonov.clean_architecture.application.usecase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.esonov.clean_architecture.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.domain.user.EmailAddress;
import org.esonov.clean_architecture.domain.user.UserAccount;
import org.esonov.clean_architecture.domain.user.UserId;

/** Test double proving the use cases run without any database or framework. */
class InMemoryUserAccountRepository implements UserAccountRepository {

    final Map<UserId, UserAccount> store = new LinkedHashMap<>();

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return store.values().stream().anyMatch(a -> a.email().equals(email));
    }

    @Override
    public void save(UserAccount account) {
        store.put(account.id(), account);
    }
}
