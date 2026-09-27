package org.esonov.clean_architecture.user.application.port.in;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;

/**
 * Input port (boundary interface, outer → inner).
 * <p>
 * Delivery mechanisms (REST controller, CLI, message listener…) call this; they never
 * see the interactor that implements it. Data crosses as plain records only.
 */
public interface RegisterUserUseCase {

    /**
     * @throws EmailAlreadyRegisteredException if an account with that email already exists
     * @throws org.esonov.clean_architecture.shared.domain.DomainValidationException if the data breaks a business rule
     */
    UserAccountView register(RegisterUserCommand command);
}
