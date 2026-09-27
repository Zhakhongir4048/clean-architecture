package org.esonov.clean_architecture.user.adapter.out.persistence;

import org.esonov.clean_architecture.TestcontainersConfiguration;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepositoryContract;
import org.esonov.clean_architecture.user.domain.EmailAddress;
import org.esonov.clean_architecture.user.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the output-port contract against real Postgres with the Flyway schema, plus checks of the
 * physical mapping that only make sense for this adapter.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserAccountPersistenceAdapter.class})
class UserAccountPersistenceAdapterIT extends UserAccountRepositoryContract {

    @Autowired
    UserAccountPersistenceAdapter adapter;

    @Autowired
    JdbcClient jdbc;

    /**
     * Explicit cleanup instead of relying on @DataJpaTest rollback: contract tests are inherited, and
     * Spring resolves @Transactional from the declaring class, so they run without the test transaction.
     */
    @BeforeEach
    void cleanTable() {
        jdbc.sql("DELETE FROM user_account").update();
    }

    @Override
    protected UserAccountRepository repository() {
        return adapter;
    }

    @Test
    void domainFieldsMapToExpectedColumns() {
        UserAccount account = UserAccount.register(new EmailAddress("Ada@Example.com"), "Ada", NOW);

        adapter.save(account);

        Map<String, Object> row = jdbc.sql("SELECT id, email, display_name, registered_at FROM user_account WHERE id = ?")
                .param(account.id().value())
                .query().singleRow();
        assertThat(row.get("id")).isEqualTo(account.id().value());
        assertThat(row.get("email")).isEqualTo("ada@example.com");
        assertThat(row.get("display_name")).isEqualTo("Ada");
        assertThat(toInstant(row.get("registered_at"))).isEqualTo(NOW);
    }

    private static Instant toInstant(Object value) {
        return switch (value) {
            case java.sql.Timestamp ts -> ts.toInstant();
            case java.time.OffsetDateTime odt -> odt.toInstant();
            default -> throw new IllegalStateException("Unexpected timestamp type: " + value.getClass());
        };
    }
}
