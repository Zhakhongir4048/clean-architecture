package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure entity tests: no mocks, no Spring, no database. */
class OrderTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final Currency USD = Currency.getInstance("USD");
    private static final CustomerId CUSTOMER = CustomerId.of("3f2b8c1e-1111-4222-8333-444455556666");

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private static OrderLine line(String product, int quantity, String price) {
        return new OrderLine(product, quantity, usd(price));
    }

    // ---- OrderLine ----------------------------------------------------------------------------

    @Test
    void lineTotalIsQuantityTimesUnitPrice() {
        assertThat(line("Pen", 3, "1.25").lineTotal()).isEqualTo(usd("3.75"));
    }

    @Test
    void productNameIsTrimmed() {
        assertThat(line("  Pen  ", 1, "1").productName()).isEqualTo("Pen");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankProductNameIsRejected(String name) {
        assertThatThrownBy(() -> line(name, 1, "1")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void productNameIsLimitedTo200Characters() {
        assertThat(line("a".repeat(200), 1, "1").productName()).hasSize(200);
        assertThatThrownBy(() -> line("a".repeat(201), 1, "1")).isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 10_001})
    void quantityOutOfRangeIsRejected(int quantity) {
        assertThatThrownBy(() -> line("Pen", quantity, "1"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Quantity must be between 1 and 10000");
    }

    @Test
    void quantityBoundsAreInclusive() {
        assertThat(line("Pen", 1, "1").quantity()).isEqualTo(1);
        assertThat(line("Pen", 10_000, "1").quantity()).isEqualTo(10_000);
    }

    // ---- Order --------------------------------------------------------------------------------

    @Test
    void placeCreatesOrderWithFreshIdentityAndTotal() {
        Order order = Order.place(CUSTOMER, USD, List.of(line("Pen", 3, "1.25"), line("Notebook", 2, "4.00")), NOW);

        assertThat(order.id()).isNotNull();
        assertThat(order.customerId()).isEqualTo(CUSTOMER);
        assertThat(order.currency()).isEqualTo(USD);
        assertThat(order.lines()).hasSize(2);
        assertThat(order.placedAt()).isEqualTo(NOW);
        assertThat(order.total()).isEqualTo(usd("11.75"));
    }

    @Test
    void freeItemsGiveZeroTotal() {
        assertThat(Order.place(CUSTOMER, USD, List.of(line("Sample", 1, "0")), NOW).total()).isEqualTo(Money.zero(USD));
    }

    @Test
    void anOrderNeedsAtLeastOneLine() {
        assertThatThrownBy(() -> Order.place(CUSTOMER, USD, List.of(), NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("An order must have at least one line");
        assertThatThrownBy(() -> Order.place(CUSTOMER, USD, null, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void anOrderHasAtMost100Lines() {
        assertThat(Order.place(CUSTOMER, USD, Collections.nCopies(100, line("Pen", 1, "1")), NOW).lines()).hasSize(100);
        assertThatThrownBy(() -> Order.place(CUSTOMER, USD, Collections.nCopies(101, line("Pen", 1, "1")), NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void everyLineMustBePricedInTheOrderCurrency() {
        OrderLine yen = new OrderLine("Tea", 1, new Money(new BigDecimal("500"), Currency.getInstance("JPY")));

        assertThatThrownBy(() -> Order.place(CUSTOMER, USD, List.of(line("Pen", 1, "1"), yen), NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("priced in JPY, order is in USD");
    }

    @Test
    void linesAreImmutableAndDetachedFromTheCallersList() {
        List<OrderLine> lines = new ArrayList<>(List.of(line("Pen", 1, "1")));
        Order order = Order.place(CUSTOMER, USD, lines, NOW);

        lines.add(line("Sneaked in", 1, "1"));

        assertThat(order.lines()).hasSize(1);
        assertThatThrownBy(() -> order.lines().add(line("Nope", 1, "1"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiredPartsMustBePresent() {
        List<OrderLine> lines = List.of(line("Pen", 1, "1"));

        assertThatNullPointerException().isThrownBy(() -> Order.place(null, USD, lines, NOW));
        assertThatNullPointerException().isThrownBy(() -> Order.place(CUSTOMER, null, lines, NOW));
        assertThatNullPointerException().isThrownBy(() -> Order.place(CUSTOMER, USD, lines, null));
    }

    @Test
    void restoreKeepsIdentityAndStillEnforcesInvariants() {
        OrderId id = OrderId.newId();

        assertThat(Order.restore(id, CUSTOMER, USD, List.of(line("Pen", 1, "1")), NOW).id()).isEqualTo(id);
        assertThatThrownBy(() -> Order.restore(id, CUSTOMER, USD, List.of(), NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void equalityIsByIdentityOnly() {
        OrderId id = OrderId.newId();
        Order a = Order.restore(id, CUSTOMER, USD, List.of(line("Pen", 1, "1")), NOW);
        Order sameId = Order.restore(id, CUSTOMER, USD, List.of(line("Other", 5, "9")), NOW.plusSeconds(1));

        assertThat(a).isEqualTo(sameId).hasSameHashCodeAs(sameId);
        assertThat(a).isNotEqualTo(Order.restore(OrderId.newId(), CUSTOMER, USD, List.of(line("Pen", 1, "1")), NOW));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "garbage"})
    void invalidIdsAreDomainErrors(String raw) {
        assertThatThrownBy(() -> OrderId.of(raw)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CustomerId.of(raw)).isInstanceOf(DomainValidationException.class);
    }
}
