package org.esonov.clean_architecture.user.adapter.in.web;

import org.esonov.clean_architecture.user.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.user.application.exception.UserAccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps user-feature exceptions to HTTP semantics — HTTP knowledge stays in the web adapter. */
@RestControllerAdvice
class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleConflict(EmailAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    ProblemDetail handleNotFound(UserAccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
