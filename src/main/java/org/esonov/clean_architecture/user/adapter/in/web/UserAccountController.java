package org.esonov.clean_architecture.user.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.esonov.clean_architecture.user.adapter.in.web.UserAccountPresenter.UserAccountViewModel;
import org.esonov.clean_architecture.user.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.user.application.port.in.RegisterUserCommand;
import org.esonov.clean_architecture.user.application.port.in.RegisterUserUseCase;

/**
 * Controller: translates the HTTP request into the use case request model and calls the input port.
 * <p>
 * No business rules (entities/interactors own them) and no response formatting ({@link UserAccountPresenter}
 * owns it). Depends only on input ports — never on interactors, entities, or persistence.
 */
@RestController
@RequestMapping(UserAccountPresenter.BASE_PATH)
class UserAccountController {

    private final RegisterUserUseCase registerUser;
    private final GetUserAccountQuery getUserAccount;
    private final UserAccountPresenter presenter;

    UserAccountController(RegisterUserUseCase registerUser, GetUserAccountQuery getUserAccount,
                          UserAccountPresenter presenter) {
        this.registerUser = registerUser;
        this.getUserAccount = getUserAccount;
        this.presenter = presenter;
    }

    @PostMapping
    ResponseEntity<UserAccountViewModel> register(@RequestBody RegisterUserHttpRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(request.email(), request.displayName());
        return presenter.created(registerUser.register(command));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserAccountViewModel> getById(@PathVariable String id) {
        return presenter.found(getUserAccount.getById(id));
    }

    /**
     * HTTP body exactly as it arrives. Kept separate from {@link RegisterUserCommand} so the API
     * field names can evolve independently of the use case. No business validation here — a missing
     * field arrives as {@code null} and the domain rejects it.
     */
    record RegisterUserHttpRequest(String email, String displayName) {
    }
}
