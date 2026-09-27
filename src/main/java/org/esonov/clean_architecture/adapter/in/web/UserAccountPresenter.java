package org.esonov.clean_architecture.adapter.in.web;

import org.esonov.clean_architecture.application.port.in.UserAccountView;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Presenter: turns the use case response model into the HTTP view model (status, headers, JSON shape).
 * <p>
 * Changes only when the <em>output</em> contract of the API changes — the controller changes only
 * when the <em>input</em> contract changes (SRP).
 */
@Component
class UserAccountPresenter {

    static final String BASE_PATH = "/api/users";

    ResponseEntity<UserAccountViewModel> created(UserAccountView view) {
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + view.id())).body(toViewModel(view));
    }

    ResponseEntity<UserAccountViewModel> found(UserAccountView view) {
        return ResponseEntity.ok(toViewModel(view));
    }

    private static UserAccountViewModel toViewModel(UserAccountView view) {
        return new UserAccountViewModel(view.id(), view.email(), view.displayName(), view.registeredAt().toString());
    }

    /** JSON shape of a user account in the HTTP API. All fields are display-ready strings. */
    record UserAccountViewModel(String id, String email, String displayName, String registeredAt) {
    }
}
