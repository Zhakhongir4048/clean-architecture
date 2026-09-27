package org.esonov.clean_architecture.shared.adapter.in.web;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps shared-kernel exceptions, thrown by any feature, to HTTP semantics. */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleValidation(DomainValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
