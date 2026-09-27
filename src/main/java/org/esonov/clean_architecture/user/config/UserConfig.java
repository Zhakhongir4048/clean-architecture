package org.esonov.clean_architecture.user.config;

import org.esonov.clean_architecture.user.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.user.application.port.in.RegisterUserUseCase;
import org.esonov.clean_architecture.user.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.user.application.usecase.GetUserAccountInteractor;
import org.esonov.clean_architecture.user.application.usecase.RegisterUserInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Main component of the user feature: the one place that knows about both sides of its boundary.
 * It instantiates the framework-free interactors and exposes them as input-port beans,
 * so adapters can only ever inject the port interfaces.
 */
@Configuration(proxyBeanMethods = false)
class UserConfig {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserAccountRepository userAccounts, Clock clock) {
        return new RegisterUserInteractor(userAccounts, clock);
    }

    @Bean
    GetUserAccountQuery getUserAccountQuery(UserAccountRepository userAccounts) {
        return new GetUserAccountInteractor(userAccounts);
    }
}
