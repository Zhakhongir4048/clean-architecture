package org.esonov.clean_architecture.order.config;

import org.esonov.clean_architecture.order.application.port.in.CreateOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.DeleteOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.GetOrderQuery;
import org.esonov.clean_architecture.order.application.port.out.CustomerLookup;
import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.application.usecase.CreateOrderInteractor;
import org.esonov.clean_architecture.order.application.usecase.DeleteOrderInteractor;
import org.esonov.clean_architecture.order.application.usecase.GetOrderInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Main component of the order feature: wires the framework-free interactors as input-port beans. */
@Configuration(proxyBeanMethods = false)
class OrderConfig {

    @Bean
    CreateOrderUseCase createOrderUseCase(OrderRepository orders, CustomerLookup customers, Clock clock) {
        return new CreateOrderInteractor(orders, customers, clock);
    }

    @Bean
    GetOrderQuery getOrderQuery(OrderRepository orders) {
        return new GetOrderInteractor(orders);
    }

    @Bean
    DeleteOrderUseCase deleteOrderUseCase(OrderRepository orders) {
        return new DeleteOrderInteractor(orders);
    }
}
