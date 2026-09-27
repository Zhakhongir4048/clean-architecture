package org.esonov.clean_architecture.user.application.port.out;

import java.util.Optional;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.user.domain.EmailAddress;
import org.esonov.clean_architecture.user.domain.UserAccount;
import org.esonov.clean_architecture.user.domain.UserId;

/**
 * Output port (boundary interface, inner → outer via dependency inversion).
 * <p>
 * The application layer declares what it needs from storage. The persistence adapter
 * implements it, so source code dependencies point inward while control flows outward.
 * Swapping JPA/Postgres for something else must not touch this interface's callers.
 */
public interface UserAccountRepository {

    Optional<UserAccount> findById(UserId id);

    boolean existsByEmail(EmailAddress email);

    /**
     * @throws EmailAlreadyRegisteredException if storage rejects a duplicate email
     *         (the final guard against a race between {@link #existsByEmail} and save)
     */
    void save(UserAccount account);
}
