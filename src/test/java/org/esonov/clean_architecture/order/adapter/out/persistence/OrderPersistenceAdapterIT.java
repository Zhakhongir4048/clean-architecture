package org.esonov.clean_architecture.order.adapter.out.persistence;

import org.esonov.clean_architecture.TestcontainersConfiguration;
import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.application.port.out.OrderRepositoryContract;
import org.esonov.clean_architecture.order.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the order output-port contract against real Postgres with the Flyway schema, plus mapping checks. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrderPersistenceAdapter.class})
class OrderPersistenceAdapterIT extends OrderRepositoryContract {

    @Autowired
    OrderPersistenceAdapter adapter;

    @Autowired
    JdbcClient jdbc;

    /** Inherited contract tests run without the @DataJpaTest transaction, so clean up explicitly. */
    @BeforeEach
    void cleanTables() {
        jdbc.sql("DELETE FROM orders").update();
    }

    @Override
    protected OrderRepository repository() {
        return adapter;
    }

    @Test
    void domainMapsToOrdersAndOrderLineTables() {
        Order order = sampleOrder();

        adapter.save(order);

        Map<String, Object> row = jdbc.sql("SELECT customer_id, currency FROM orders WHERE id = ?")
                .param(order.id().value()).query().singleRow();
        assertThat(row.get("customer_id")).isEqualTo(CUSTOMER.value());
        assertThat(row.get("currency")).isEqualTo("USD");

        List<Map<String, Object>> lines = jdbc.sql(
                        "SELECT line_no, product_name, quantity, unit_price FROM order_line WHERE order_id = ? ORDER BY line_no")
                .param(order.id().value()).query().listOfRows();
        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst()).containsEntry("line_no", 0).containsEntry("product_name", "Pen").containsEntry("quantity", 3);
        assertThat((BigDecimal) lines.getFirst().get("unit_price")).isEqualByComparingTo("1.25");
    }

    /** Runs outside the test transaction so the adapter's own transaction commits, exactly as in production. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletingAnOrderCascadesToItsLines() {
        Order order = sampleOrder();
        adapter.save(order);

        adapter.deleteById(order.id());

        Integer lines = jdbc.sql("SELECT count(*) FROM order_line WHERE order_id = ?")
                .param(order.id().value()).query(Integer.class).single();
        assertThat(lines).isZero();
    }
}
