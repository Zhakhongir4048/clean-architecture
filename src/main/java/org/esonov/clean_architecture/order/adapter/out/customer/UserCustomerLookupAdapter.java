package org.esonov.clean_architecture.order.adapter.out.customer;

import org.esonov.clean_architecture.order.application.port.out.CustomerLookup;
import org.esonov.clean_architecture.order.domain.CustomerId;
import org.esonov.clean_architecture.user.application.exception.UserAccountNotFoundException;
import org.esonov.clean_architecture.user.application.port.in.GetUserAccountQuery;
import org.springframework.stereotype.Component;

/**
 * Cross-feature adapter: satisfies the order feature's {@link CustomerLookup} port by calling the user
 * feature's <em>input port</em>. This class is the only place in {@code order} that knows {@code user} exists;
 * the order domain and use cases see only {@code CustomerLookup}.
 */
@Component
class UserCustomerLookupAdapter implements CustomerLookup {

    private final GetUserAccountQuery getUserAccount;

    UserCustomerLookupAdapter(GetUserAccountQuery getUserAccount) {
        this.getUserAccount = getUserAccount;
    }

    @Override
    public boolean exists(CustomerId id) {
        try {
            getUserAccount.getById(id.toString());
            return true;
        } catch (UserAccountNotFoundException e) {
            return false;
        }
    }
}
