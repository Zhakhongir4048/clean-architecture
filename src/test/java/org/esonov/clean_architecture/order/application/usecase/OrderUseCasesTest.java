package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.exception.CustomerNotFoundException;
import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderCommand;
import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.esonov.clean_architecture.order.application.port.out.CustomerLookup;
import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use cases run with in-memory doubles only: no Spring, no database, no user feature. */
class OrderUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final String CUSTOMER = "3f2b8c1e-1111-4222-8333-444455556666";

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final Set<CustomerId> knownCustomers = new HashSet<>(Set.of(CustomerId.of(CUSTOMER)));
    private final CustomerLookup customers = knownCustomers::contains;

    private final CreateOrderInteractor createOrder =
            new CreateOrderInteractor(orders, customers, Clock.fixed(NOW, ZoneOffset.UTC));
    private final GetOrderInteractor getOrder = new GetOrderInteractor(orders);
    private final DeleteOrderInteractor deleteOrder = new DeleteOrderInteractor(orders);

    private static CreateOrderCommand command(String customerId, String currency, CreateOrderCommand.Line... lines) {
        return new CreateOrderCommand(customerId, currency, List.of(lines));
    }

    private static CreateOrderCommand.Line line(String product, int quantity, String price) {
        return new CreateOrderCommand.Line(product, quantity, new BigDecimal(price));
    }

    @Test
    void createsOrderAndReturnsPlainView() {
        OrderView view = createOrder.create(command(CUSTOMER, "usd", line("Pen", 3, "1.25"), line("Notebook", 2, "4")));

        assertThat(view.customerId()).isEqualTo(CUSTOMER);
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.placedAt()).isEqualTo(NOW);
        assertThat(view.total()).isEqualByComparingTo("11.75");
        assertThat(view.lines()).extracting(OrderView.Line::lineTotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("3.75"), new BigDecimal("8.00"));
        assertThat(orders.store).hasSize(1);
        assertThat(getOrder.getById(view.id())).isEqualTo(view);
    }

    @Test
    void unknownCustomerIsRejectedAndNothingIsSaved() {
        String stranger = "00000000-0000-0000-0000-000000000000";

        assertThatThrownBy(() -> createOrder.create(command(stranger, "USD", line("Pen", 1, "1"))))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(stranger);
        assertThat(orders.store).isEmpty();
    }

    @Test
    void invalidDataIsRejectedBeforeAskingForTheCustomer() {
        CustomerLookup mustNotBeCalled = id -> {
            throw new AssertionError("customer lookup must not be called for invalid input");
        };
        CreateOrderInteractor strict = new CreateOrderInteractor(orders, mustNotBeCalled, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> strict.create(command(CUSTOMER, "USD")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> strict.create(new CreateOrderCommand(CUSTOMER, "USD", null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> strict.create(command(CUSTOMER, "XYZ", line("Pen", 1, "1"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> strict.create(command("not-a-uuid", "USD", line("Pen", 1, "1"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> strict.create(command(CUSTOMER, "USD", line("Pen", 0, "1"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> strict.create(command(CUSTOMER, "USD", line("Pen", 1, "1.001"))))
                .isInstanceOf(DomainValidationException.class);
        assertThat(orders.store).isEmpty();
    }

    @Test
    void deleteRemovesTheOrder() {
        OrderView view = createOrder.create(command(CUSTOMER, "USD", line("Pen", 1, "1")));

        deleteOrder.delete(view.id());

        assertThat(orders.store).isEmpty();
        assertThatThrownBy(() -> getOrder.getById(view.id())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deletingTwiceIsNotFoundTheSecondTime() {
        OrderView view = createOrder.create(command(CUSTOMER, "USD", line("Pen", 1, "1")));
        deleteOrder.delete(view.id());

        assertThatThrownBy(() -> deleteOrder.delete(view.id())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void unknownOrMalformedIdIsNotFound() {
        for (String id : List.of("00000000-0000-0000-0000-000000000000", "garbage")) {
            assertThatThrownBy(() -> getOrder.getById(id)).isInstanceOf(OrderNotFoundException.class);
            assertThatThrownBy(() -> deleteOrder.delete(id)).isInstanceOf(OrderNotFoundException.class);
        }
    }
}
