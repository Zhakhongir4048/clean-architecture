package org.esonov.clean_architecture.domain.user;

import org.esonov.clean_architecture.domain.DomainValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Enterprise business rule: a registered user account.
 * <p>
 * Plain Java — no JPA, Spring, or Jackson annotations. Persistence and web
 * representations live in the adapter layer and are mapped at the boundary.
 */
public final class UserAccount {

    private static final int MAX_DISPLAY_NAME_LENGTH = 100;

    private final UserId id;
    private final EmailAddress email;
    private String displayName;
    private final Instant registeredAt;

    private UserAccount(UserId id, EmailAddress email, String displayName, Instant registeredAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.displayName = validateDisplayName(displayName);
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
    }

    /**
     * Creates a brand-new account.
     */
    public static UserAccount register(EmailAddress email, String displayName, Instant now) {
        return new UserAccount(UserId.newId(), email, displayName, now);
    }

    /**
     * Rebuilds an account that already exists (e.g. loaded from storage).
     */
    public static UserAccount restore(UserId id, EmailAddress email, String displayName, Instant registeredAt) {
        return new UserAccount(id, email, displayName, registeredAt);
    }

    public void rename(String newDisplayName) {
        this.displayName = validateDisplayName(newDisplayName);
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    private static String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new DomainValidationException("Display name must not be blank");
        }
        String trimmed = displayName.strip();
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainValidationException("Display name must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UserAccount other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
