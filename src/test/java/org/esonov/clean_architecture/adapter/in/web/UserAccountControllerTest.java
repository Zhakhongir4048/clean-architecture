package org.esonov.clean_architecture.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.esonov.clean_architecture.application.exception.EmailAlreadyRegisteredException;
import org.esonov.clean_architecture.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.application.port.in.RegisterUserCommand;
import org.esonov.clean_architecture.application.port.in.RegisterUserUseCase;
import org.esonov.clean_architecture.application.port.in.UserAccountView;
import org.esonov.clean_architecture.domain.DomainValidationException;

/** The web adapter (controller + presenter) is tested against mocked input ports — no interactors, no database. */
@WebMvcTest(UserAccountController.class)
@Import(UserAccountPresenter.class)
class UserAccountControllerTest {

    private static final UserAccountView ADA = new UserAccountView(
            "3f2b8c1e-1111-4222-8333-444455556666", "ada@example.com", "Ada", Instant.parse("2026-09-27T10:00:00Z"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RegisterUserUseCase registerUser;

    @MockitoBean
    GetUserAccountQuery getUserAccount;

    @Test
    void registerReturns201WithLocation() throws Exception {
        given(registerUser.register(new RegisterUserCommand("ada@example.com", "Ada"))).willReturn(ADA);

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"displayName\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + ADA.id()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.registeredAt").value("2026-09-27T10:00:00Z"));
    }

    @Test
    void mapsInnerExceptionsToHttpStatus() throws Exception {
        willThrow(new EmailAlreadyRegisteredException("ada@example.com")).given(registerUser).register(any());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ada@example.com\",\"displayName\":\"Ada\"}"))
                .andExpect(status().isConflict());

        willThrow(new DomainValidationException("Email is not a valid address")).given(registerUser).register(any());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x\",\"displayName\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email is not a valid address"));

        given(getUserAccount.getById("nope")).willThrow(new UserAccountNotFoundException("nope"));
        mvc.perform(get("/api/users/nope")).andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsView() throws Exception {
        given(getUserAccount.getById(ADA.id())).willReturn(ADA);

        mvc.perform(get("/api/users/" + ADA.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada"));
    }

    @Test
    void missingFieldIsPassedAsNullSoTheDomainDecides() throws Exception {
        willThrow(new DomainValidationException("Display name must not be blank")).given(registerUser).register(any());

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ada@example.com\"}"))
                .andExpect(status().isBadRequest());

        verify(registerUser).register(new RegisterUserCommand("ada@example.com", null));
    }

    @Test
    void malformedJsonIsRejectedBeforeReachingTheUseCase() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());

        verify(registerUser, never()).register(any());
    }
}
