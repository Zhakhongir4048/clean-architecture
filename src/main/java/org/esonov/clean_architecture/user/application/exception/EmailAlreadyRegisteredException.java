package org.esonov.clean_architecture.user.application.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
