package org.esonov.clean_architecture.adapter.out.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import org.esonov.clean_architecture.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.domain.user.EmailAddress;
import org.esonov.clean_architecture.domain.user.UserAccount;
import org.esonov.clean_architecture.domain.user.UserId;

/**
 * Implements the application's output port with Spring Data JPA.
 * All JPA/Spring types stop here — nothing framework-specific crosses back inward.
 */
@Component
class UserAccountPersistenceAdapter implements UserAccountRepository {

    static final String EMAIL_UNIQUE_CONSTRAINT = "uk_user_account_email";

    private final SpringDataUserAccountRepository jpaRepository;

    UserAccountPersistenceAdapter(SpringDataUserAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(UserAccountPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public void save(UserAccount account) {
        try {
            jpaRepository.saveAndFlush(toJpa(account));
        } catch (DataIntegrityViolationException e) {
            if (violates(e, EMAIL_UNIQUE_CONSTRAINT)) {
                throw new EmailAlreadyRegisteredException(account.email().value());
            }
            throw e;
        }
    }

    private static boolean violates(DataIntegrityViolationException e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && cve.getConstraintName() != null
                    && cve.getConstraintName().equalsIgnoreCase(constraint)) {
                return true;
            }
        }
        return false;
    }

    private static UserAccountJpaEntity toJpa(UserAccount account) {
        return new UserAccountJpaEntity(
                account.id().value(),
                account.email().value(),
                account.displayName(),
                account.registeredAt());
    }

    private static UserAccount toDomain(UserAccountJpaEntity entity) {
        return UserAccount.restore(
                new UserId(entity.getId()),
                new EmailAddress(entity.getEmail()),
                entity.getDisplayName(),
                entity.getRegisteredAt());
    }
}
