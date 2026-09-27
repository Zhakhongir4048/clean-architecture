package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
    }

    /**
     * @throws DomainValidationException if {@code value} is null or not a UUID
     */
    public static CustomerId of(String value) {
        if (value == null) {
            throw new DomainValidationException("Customer id must not be null");
        }
        try {
            return new CustomerId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Customer id is not a valid UUID: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
