package org.esonov.clean_architecture.order.application.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("No order with id '" + orderId + "'");
    }
}
