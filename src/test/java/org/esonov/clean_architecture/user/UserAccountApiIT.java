package org.esonov.clean_architecture.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * End-to-end across every boundary: HTTP → web adapter → input port → interactor
 * → output port → persistence adapter → Postgres (Testcontainers, Flyway schema).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserAccountApiIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("TRUNCATE TABLE user_account").update();
    }

    @Test
    void registerThenFetch() throws Exception {
        String location = register("  Ada@Example.com ", "Ada Lovelace")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.registeredAt").isNotEmpty());
    }

    @Test
    void registrationIsPersisted() throws Exception {
        register("ada@example.com", "Ada").andExpect(status().isCreated());

        Integer rows = jdbc.sql("SELECT count(*) FROM user_account WHERE email = 'ada@example.com'")
                .query(Integer.class).single();
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register("ada@example.com", "Ada").andExpect(status().isCreated());

        register("ADA@example.com", "Someone else")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with email 'ada@example.com' already exists"));
    }

    @Test
    void invalidDataReturns400AndPersistsNothing() throws Exception {
        register("not-an-email", "Ada").andExpect(status().isBadRequest());
        register("ada@example.com", "   ").andExpect(status().isBadRequest());

        Integer rows = jdbc.sql("SELECT count(*) FROM user_account").query(Integer.class).single();
        assertThat(rows).isZero();
    }

    @Test
    void unknownOrMalformedIdReturns404() throws Exception {
        mvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/users/garbage")).andExpect(status().isNotFound());
    }

    @Test
    void locationHeaderPointsToCreatedResource() throws Exception {
        register("bob@example.com", "Bob")
                .andExpect(header().string("Location", matchesPattern("/api/users/[0-9a-f-]{36}")));
    }

    private ResultActions register(String email, String displayName) throws Exception {
        return mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "displayName": "%s"}
                        """.formatted(email, displayName)));
    }
}
