package org.esonov.clean_architecture.order.application.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("No customer with id '" + customerId + "'");
    }
}
