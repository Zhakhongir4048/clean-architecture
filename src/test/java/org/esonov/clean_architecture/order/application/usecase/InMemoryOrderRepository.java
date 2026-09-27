package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test double for the use case tests. {@link Order} is immutable, so storing references is safe.
 * Proven to behave like the real adapter by {@link InMemoryOrderRepositoryTest}.
 */
class InMemoryOrderRepository implements OrderRepository {

    final Map<OrderId, Order> store = new LinkedHashMap<>();

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Order order) {
        store.put(order.id(), order);
    }

    @Override
    public boolean deleteById(OrderId id) {
        return store.remove(id) != null;
    }
}
