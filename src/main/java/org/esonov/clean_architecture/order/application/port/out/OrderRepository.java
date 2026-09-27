package org.esonov.clean_architecture.order.application.port.out;

import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderId;

import java.util.Optional;

/** Output port: what the order use cases need from storage. Implemented by the persistence adapter. */
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    void save(Order order);

    /**
     * @return {@code true} if an order was deleted, {@code false} if none had that id
     */
    boolean deleteById(OrderId id);
}
