package org.esonov.clean_architecture.user.application.port.in;

import java.time.Instant;

/**
 * Response model crossing the input boundary back out.
 * <p>
 * Pure data with no dependency on the domain: callers of input ports never depend on
 * entities, not even transitively. Mapping from the entity lives in the use case layer.
 */
public record UserAccountView(String id, String email, String displayName, Instant registeredAt) {
}
