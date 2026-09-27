package org.esonov.clean_architecture.application.exception;

public class UserAccountNotFoundException extends RuntimeException {

    public UserAccountNotFoundException(String userId) {
        super("No user account with id '" + userId + "'");
    }
}
