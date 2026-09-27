package org.esonov.clean_architecture.order.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persistence representation of an order. The domain {@code Order} knows nothing about it. */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class OrderJpaEntity {

    @Id
    private UUID id;

    // Plain value, no foreign key: the order feature does not own the user_account table.
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<OrderLineEmbeddable> lines;
}
