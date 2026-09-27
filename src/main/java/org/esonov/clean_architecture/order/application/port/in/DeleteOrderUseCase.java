package org.esonov.clean_architecture.order.application.port.in;

import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;

/** Input port (boundary interface, outer → inner). */
public interface DeleteOrderUseCase {

    /**
     * @throws OrderNotFoundException if no order has that id (including a malformed id)
     */
    void delete(String orderId);
}
