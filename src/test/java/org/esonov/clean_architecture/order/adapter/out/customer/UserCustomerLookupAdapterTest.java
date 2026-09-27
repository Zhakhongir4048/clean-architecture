package org.esonov.clean_architecture.order.adapter.out.customer;

import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.user.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.user.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.user.application.port.in.UserAccountView;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/** The cross-feature adapter is tested against a mocked user input port — the user feature is not started. */
class UserCustomerLookupAdapterTest {

    private static final String ID = "3f2b8c1e-1111-4222-8333-444455556666";

    private final GetUserAccountQuery getUserAccount = mock(GetUserAccountQuery.class);
    private final UserCustomerLookupAdapter adapter = new UserCustomerLookupAdapter(getUserAccount);

    @Test
    void existingUserIsAnExistingCustomer() {
        given(getUserAccount.getById(ID)).willReturn(new UserAccountView(ID, "ada@example.com", "Ada", Instant.now()));

        assertThat(adapter.exists(CustomerId.of(ID))).isTrue();
    }

    @Test
    void unknownUserIsNotACustomer() {
        given(getUserAccount.getById(ID)).willThrow(new UserAccountNotFoundException(ID));

        assertThat(adapter.exists(CustomerId.of(ID))).isFalse();
    }
}
