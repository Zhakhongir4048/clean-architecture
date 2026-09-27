package org.esonov.clean_architecture.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import org.esonov.clean_architecture.shared.domain.DomainValidationException;

/** Value object. Normalised to lower case so uniqueness checks are case-insensitive. */
public record EmailAddress(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Email must not be blank");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("Email is not a valid address");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
