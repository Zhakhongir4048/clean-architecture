package org.esonov.clean_architecture.user.application.usecase;

import org.esonov.clean_architecture.user.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepositoryContract;

/** Proves the test double honours the same contract as the real persistence adapter. */
class InMemoryUserAccountRepositoryTest extends UserAccountRepositoryContract {

    private final InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();

    @Override
    protected UserAccountRepository repository() {
        return repository;
    }
}
