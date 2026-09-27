package org.esonov.clean_architecture.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.esonov.clean_architecture.application.port.in.GetUserAccountQuery;
import org.esonov.clean_architecture.application.port.in.RegisterUserUseCase;
import org.esonov.clean_architecture.application.port.out.UserAccountRepository;
import org.esonov.clean_architecture.application.usecase.GetUserAccountInteractor;
import org.esonov.clean_architecture.application.usecase.RegisterUserInteractor;

/**
 * Part of the Main component: the one place that knows about both sides of the boundary.
 * It instantiates the framework-free interactors and exposes them as input-port beans,
 * so adapters can only ever inject the port interfaces.
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserAccountRepository userAccounts, Clock clock) {
        return new RegisterUserInteractor(userAccounts, clock);
    }

    @Bean
    GetUserAccountQuery getUserAccountQuery(UserAccountRepository userAccounts) {
        return new GetUserAccountInteractor(userAccounts);
    }
}
