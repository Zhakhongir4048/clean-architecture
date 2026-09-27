package org.esonov.clean_architecture.application.port.in;

import org.esonov.clean_architecture.application.exception.UserAccountNotFoundException;

/** Input port (boundary interface, outer → inner). */
public interface GetUserAccountQuery {

    /**
     * @throws UserAccountNotFoundException if no account has that id (including a malformed id)
     */
    UserAccountView getById(String userId);
}
