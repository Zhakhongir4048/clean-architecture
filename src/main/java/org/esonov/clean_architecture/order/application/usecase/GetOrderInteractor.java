package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.esonov.clean_architecture.order.application.port.in.GetOrderQuery;
import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.esonov.clean_architecture.order.application.port.out.OrderRepository;

import java.util.Objects;

public class GetOrderInteractor implements GetOrderQuery {

    private final OrderRepository orders;

    public GetOrderInteractor(OrderRepository orders) {
        this.orders = Objects.requireNonNull(orders, "orders");
    }

    @Override
    public OrderView getById(String orderId) {
        return orders.findById(OrderIds.parseOrNotFound(orderId))
                .map(OrderViews::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
