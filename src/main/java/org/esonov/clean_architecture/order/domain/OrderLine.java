package org.esonov.clean_architecture.order.domain;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

import java.util.Objects;

/** Value object: one product line of an order. */
public record OrderLine(String productName, int quantity, Money unitPrice) {

    private static final int MAX_PRODUCT_NAME_LENGTH = 200;
    private static final int MAX_QUANTITY = 10_000;

    public OrderLine {
        if (productName == null || productName.isBlank()) {
            throw new DomainValidationException("Product name must not be blank");
        }
        productName = productName.strip();
        if (productName.length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new DomainValidationException("Product name must be at most " + MAX_PRODUCT_NAME_LENGTH + " characters");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new DomainValidationException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
