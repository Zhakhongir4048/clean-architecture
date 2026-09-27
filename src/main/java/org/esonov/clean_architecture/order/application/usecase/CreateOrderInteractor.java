package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.exception.CustomerNotFoundException;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderCommand;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.esonov.clean_architecture.order.application.port.out.CustomerLookup;
import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.order.domain.Money;
import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderLine;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.time.Clock;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Application business rule: an order can only be placed for an existing customer.
 * Framework-free; wired by {@code order.config.OrderConfig}.
 */
public class CreateOrderInteractor implements CreateOrderUseCase {

    private final OrderRepository orders;
    private final CustomerLookup customers;
    private final Clock clock;

    public CreateOrderInteractor(OrderRepository orders, CustomerLookup customers, Clock clock) {
        this.orders = Objects.requireNonNull(orders, "orders");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public OrderView create(CreateOrderCommand command) {
        CustomerId customerId = CustomerId.of(command.customerId());
        Currency currency = Money.parseCurrency(command.currency());
        // Build (and so validate) the order before the cross-feature call: bad input fails fast and cheap.
        Order order = Order.place(customerId, currency, toLines(command.lines(), currency), clock.instant());

        if (!customers.exists(customerId)) {
            throw new CustomerNotFoundException(customerId.toString());
        }
        orders.save(order);
        return OrderViews.from(order);
    }

    private static List<OrderLine> toLines(List<CreateOrderCommand.Line> lines, Currency currency) {
        if (lines == null) {
            throw new DomainValidationException("An order must have at least one line");
        }
        return lines.stream()
                .map(l -> new OrderLine(l.productName(), l.quantity(), new Money(l.unitPrice(), currency)))
                .toList();
    }
}
