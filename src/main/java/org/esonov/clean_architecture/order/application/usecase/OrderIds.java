package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.esonov.clean_architecture.order.domain.OrderId;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;

/** A malformed id can never match an order, so to a caller it is simply "not found". */
final class OrderIds {

    private OrderIds() {
    }

    static OrderId parseOrNotFound(String orderId) {
        try {
            return OrderId.of(orderId);
        } catch (DomainValidationException e) {
            throw new OrderNotFoundException(orderId);
        }
    }
}
