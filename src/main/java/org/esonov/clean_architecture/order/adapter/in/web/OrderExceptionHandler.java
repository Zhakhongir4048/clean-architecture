package org.esonov.clean_architecture.order.adapter.in.web;

import org.esonov.clean_architecture.order.application.exception.CustomerNotFoundException;
import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps order-feature exceptions to HTTP semantics. */
@RestControllerAdvice
class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** The request is well-formed but refers to a customer that does not exist. */
    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail handleUnknownCustomer(CustomerNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }
}
