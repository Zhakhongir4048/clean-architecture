package org.esonov.clean_architecture.order;

import com.jayway.jsonpath.JsonPath;
import org.esonov.clean_architecture.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end across both features: a user is registered through the user API, then orders are placed
 * for them through the order API — exercising the cross-feature CustomerLookup → user input port path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    private String customerId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.sql("DELETE FROM orders").update();
        jdbc.sql("DELETE FROM user_account").update();
        String body = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ada@example.com\", \"displayName\": \"Ada\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        customerId = JsonPath.read(body, "$.id");
    }

    @Test
    void createGetDeleteLifecycle() throws Exception {
        String location = createOrder(customerId, "USD", """
                [{"productName": "Pen", "quantity": 3, "unitPrice": 1.25},
                 {"productName": "Notebook", "quantity": 2, "unitPrice": "4"}]""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value("11.75"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.lines[0].productName").value("Pen"))
                .andExpect(jsonPath("$.lines[1].lineTotal").value("8.00"))
                .andExpect(jsonPath("$.total").value("11.75"));

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(delete(location)).andExpect(status().isNotFound());

        assertThat(count("orders")).isZero();
        assertThat(count("order_line")).isZero();
    }

    @Test
    void orderForUnknownCustomerIsRejectedWith422() throws Exception {
        createOrder("00000000-0000-0000-0000-000000000000", "USD", """
                [{"productName": "Pen", "quantity": 1, "unitPrice": 1}]""")
                .andExpect(status().isUnprocessableContent());

        assertThat(count("orders")).isZero();
    }

    @Test
    void invalidOrdersAreRejectedWith400() throws Exception {
        createOrder(customerId, "USD", "[]").andExpect(status().isBadRequest());
        createOrder(customerId, "XYZ", """
                [{"productName": "Pen", "quantity": 1, "unitPrice": 1}]""").andExpect(status().isBadRequest());
        createOrder(customerId, "USD", """
                [{"productName": "Pen", "quantity": 0, "unitPrice": 1}]""").andExpect(status().isBadRequest());
        createOrder(customerId, "USD", """
                [{"productName": "Pen", "quantity": 1, "unitPrice": 1.001}]""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Amount 1.001 has more than 2 decimal places for USD"));
        createOrder("not-a-uuid", "USD", """
                [{"productName": "Pen", "quantity": 1, "unitPrice": 1}]""").andExpect(status().isBadRequest());

        assertThat(count("orders")).isZero();
    }

    @Test
    void unknownOrMalformedIdIs404() throws Exception {
        mvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/garbage")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/garbage")).andExpect(status().isNotFound());
    }

    private ResultActions createOrder(String customer, String currency, String linesJson) throws Exception {
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerId": "%s", "currency": "%s", "lines": %s}
                        """.formatted(customer, currency, linesJson)));
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }
}
