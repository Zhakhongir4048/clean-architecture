package org.esonov.clean_architecture.application.port.in;

/** Request model crossing the input boundary. Deliberately raw: validation belongs to the domain. */
public record RegisterUserCommand(String email, String displayName) {
}
