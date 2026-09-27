package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderLine;

/** Maps the entity to the input boundary's response model. */
final class OrderViews {

    private OrderViews() {
    }

    static OrderView from(Order order) {
        return new OrderView(
                order.id().toString(),
                order.customerId().toString(),
                order.currency().getCurrencyCode(),
                order.lines().stream().map(OrderViews::line).toList(),
                order.total().amount(),
                order.placedAt());
    }

    private static OrderView.Line line(OrderLine line) {
        return new OrderView.Line(line.productName(), line.quantity(), line.unitPrice().amount(), line.lineTotal().amount());
    }
}
