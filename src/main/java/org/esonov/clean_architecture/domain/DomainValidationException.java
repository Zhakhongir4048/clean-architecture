package org.esonov.clean_architecture.domain;

/** Thrown when an enterprise business rule rejects the given data. */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
