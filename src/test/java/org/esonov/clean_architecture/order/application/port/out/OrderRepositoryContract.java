package org.esonov.clean_architecture.order.application.port.out;

import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.order.domain.Money;
import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderId;
import org.esonov.clean_architecture.order.domain.OrderLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural contract of the {@link OrderRepository} output port, run against every implementation
 * (in-memory test double and the real persistence adapter).
 */
public abstract class OrderRepositoryContract {

    // Microsecond precision: the finest resolution Postgres timestamptz keeps.
    protected static final Instant NOW = Instant.parse("2026-09-28T10:00:00.123456Z");
    protected static final Currency USD = Currency.getInstance("USD");
    protected static final CustomerId CUSTOMER = CustomerId.of("3f2b8c1e-1111-4222-8333-444455556666");

    protected abstract OrderRepository repository();

    protected static Order sampleOrder() {
        return Order.place(CUSTOMER, USD, List.of(
                new OrderLine("Pen", 3, new Money(new BigDecimal("1.25"), USD)),
                new OrderLine("Notebook", 1, new Money(new BigDecimal("4.00"), USD))), NOW);
    }

    @Test
    void saveThenFindByIdReturnsTheSameState() {
        Order order = sampleOrder();

        repository().save(order);

        Order loaded = repository().findById(order.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(order.id());
        assertThat(loaded.customerId()).isEqualTo(CUSTOMER);
        assertThat(loaded.currency()).isEqualTo(USD);
        assertThat(loaded.placedAt()).isEqualTo(NOW);
        assertThat(loaded.lines()).containsExactlyElementsOf(order.lines());
        assertThat(loaded.total()).isEqualTo(order.total());
    }

    @Test
    void lineOrderIsPreserved() {
        List<OrderLine> lines = List.of(
                new OrderLine("C", 1, Money.zero(USD)),
                new OrderLine("A", 1, Money.zero(USD)),
                new OrderLine("B", 1, Money.zero(USD)));
        Order order = Order.place(CUSTOMER, USD, lines, NOW);

        repository().save(order);

        assertThat(repository().findById(order.id()).orElseThrow().lines())
                .extracting(OrderLine::productName).containsExactly("C", "A", "B");
    }

    @Test
    void findByIdOfUnknownIdIsEmpty() {
        assertThat(repository().findById(OrderId.newId())).isEmpty();
    }

    @Test
    void deleteByIdRemovesTheOrder() {
        Order order = sampleOrder();
        repository().save(order);

        assertThat(repository().deleteById(order.id())).isTrue();

        assertThat(repository().findById(order.id())).isEmpty();
    }

    @Test
    void deleteByIdOfUnknownIdReportsNothingDeleted() {
        assertThat(repository().deleteById(OrderId.newId())).isFalse();
    }

    @Test
    void deleteLeavesOtherOrdersAlone() {
        Order keep = sampleOrder();
        Order remove = sampleOrder();
        repository().save(keep);
        repository().save(remove);

        repository().deleteById(remove.id());

        assertThat(repository().findById(keep.id())).isPresent();
    }
}
