package org.esonov.clean_architecture.order.adapter.out.persistence;

import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.order.domain.Money;
import org.esonov.clean_architecture.order.domain.Order;
import org.esonov.clean_architecture.order.domain.OrderId;
import org.esonov.clean_architecture.order.domain.OrderLine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Optional;

/**
 * Implements the order output port with Spring Data JPA.
 * All JPA/Spring types stop here — nothing framework-specific crosses back inward.
 */
@Component
class OrderPersistenceAdapter implements OrderRepository {

    private final SpringDataOrderRepository jpaRepository;

    OrderPersistenceAdapter(SpringDataOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public void save(Order order) {
        jpaRepository.saveAndFlush(toJpa(order));
    }

    @Override
    @Transactional
    public boolean deleteById(OrderId id) {
        if (!jpaRepository.existsById(id.value())) {
            return false;
        }
        jpaRepository.deleteById(id.value());
        return true;
    }

    private static OrderJpaEntity toJpa(Order order) {
        return new OrderJpaEntity(
                order.id().value(),
                order.customerId().value(),
                order.currency().getCurrencyCode(),
                order.placedAt(),
                order.lines().stream()
                        .map(l -> new OrderLineEmbeddable(l.productName(), l.quantity(), l.unitPrice().amount()))
                        .toList());
    }

    private static Order toDomain(OrderJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        return Order.restore(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                currency,
                entity.getLines().stream()
                        .map(l -> new OrderLine(l.getProductName(), l.getQuantity(), new Money(l.getUnitPrice(), currency)))
                        .toList(),
                entity.getPlacedAt());
    }
}
