package org.esonov.clean_architecture.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountJpaEntity, UUID> {

    boolean existsByEmail(String email);
}
