package org.esonov.clean_architecture.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persistence representation of a user account. Lives only on the outer side of the
 * boundary; the domain {@code UserAccount} knows nothing about it.
 */
@Entity
@Table(name = "user_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class UserAccountJpaEntity {

    @Id
    private UUID id;

    // Uniqueness is enforced by the Flyway-managed constraint uk_user_account_email.
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;
}
