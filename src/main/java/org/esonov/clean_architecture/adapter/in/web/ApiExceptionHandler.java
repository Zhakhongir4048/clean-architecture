package org.esonov.clean_architecture.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.esonov.clean_architecture.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.domain.DomainValidationException;

/** Maps inner-layer exceptions to HTTP semantics — HTTP knowledge stays in the web adapter. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleValidation(DomainValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleConflict(EmailAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    ProblemDetail handleNotFound(UserAccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
