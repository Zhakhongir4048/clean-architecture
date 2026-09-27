package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.esonov.clean_architecture.order.application.port.in.DeleteOrderUseCase;
import org.esonov.clean_architecture.order.application.port.out.OrderRepository;

import java.util.Objects;

public class DeleteOrderInteractor implements DeleteOrderUseCase {

    private final OrderRepository orders;

    public DeleteOrderInteractor(OrderRepository orders) {
        this.orders = Objects.requireNonNull(orders, "orders");
    }

    @Override
    public void delete(String orderId) {
        if (!orders.deleteById(OrderIds.parseOrNotFound(orderId))) {
            throw new OrderNotFoundException(orderId);
        }
    }
}
