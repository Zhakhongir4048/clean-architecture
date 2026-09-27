package org.esonov.clean_architecture.order.application.port.in;

import org.esonov.clean_architecture.order.application.exception.CustomerNotFoundException;

/** Input port (boundary interface, outer → inner). */
public interface CreateOrderUseCase {

    /**
     * @throws CustomerNotFoundException if the referenced customer does not exist
     * @throws org.esonov.clean_architecture.shared.domain.DomainValidationException if the data breaks a business rule
     */
    OrderView create(CreateOrderCommand command);
}
