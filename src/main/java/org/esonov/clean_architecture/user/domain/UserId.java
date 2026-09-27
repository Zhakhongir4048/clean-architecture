package org.esonov.clean_architecture.user.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * @throws DomainValidationException if {@code value} is null or not a UUID
     */
    public static UserId of(String value) {
        if (value == null) {
            throw new DomainValidationException("User id must not be null");
        }
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("User id is not a valid UUID: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
