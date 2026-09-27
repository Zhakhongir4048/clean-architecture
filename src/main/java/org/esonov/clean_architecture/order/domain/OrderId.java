package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    /**
     * @throws DomainValidationException if {@code value} is null or not a UUID
     */
    public static OrderId of(String value) {
        if (value == null) {
            throw new DomainValidationException("Order id must not be null");
        }
        try {
            return new OrderId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Order id is not a valid UUID: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
