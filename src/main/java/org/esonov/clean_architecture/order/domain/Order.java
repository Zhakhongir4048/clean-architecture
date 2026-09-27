package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise business rule: a customer's order. Immutable once placed.
 * <p>
 * Refers to its customer only by {@link CustomerId} — the order feature knows nothing about
 * the user feature's classes.
 */
public final class Order {

    private static final int MAX_LINES = 100;

    private final OrderId id;
    private final CustomerId customerId;
    private final Currency currency;
    private final List<OrderLine> lines;
    private final Instant placedAt;

    private Order(OrderId id, CustomerId customerId, Currency currency, List<OrderLine> lines, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.lines = validateLines(lines, currency);
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    /** Places a brand-new order. */
    public static Order place(CustomerId customerId, Currency currency, List<OrderLine> lines, Instant now) {
        return new Order(OrderId.newId(), customerId, currency, lines, now);
    }

    /** Rebuilds an order that already exists (e.g. loaded from storage). */
    public static Order restore(OrderId id, CustomerId customerId, Currency currency, List<OrderLine> lines, Instant placedAt) {
        return new Order(id, customerId, currency, lines, placedAt);
    }

    public Money total() {
        return lines.stream().map(OrderLine::lineTotal).reduce(Money.zero(currency), Money::plus);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Currency currency() {
        return currency;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public Instant placedAt() {
        return placedAt;
    }

    private static List<OrderLine> validateLines(List<OrderLine> lines, Currency currency) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainValidationException("An order must have at least one line");
        }
        if (lines.size() > MAX_LINES) {
            throw new DomainValidationException("An order can have at most " + MAX_LINES + " lines");
        }
        for (OrderLine line : lines) {
            Objects.requireNonNull(line, "line");
            if (!line.unitPrice().currency().equals(currency)) {
                throw new DomainValidationException(
                        "Line '" + line.productName() + "' is priced in " + line.unitPrice().currency() + ", order is in " + currency);
            }
        }
        return List.copyOf(lines);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Order other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
