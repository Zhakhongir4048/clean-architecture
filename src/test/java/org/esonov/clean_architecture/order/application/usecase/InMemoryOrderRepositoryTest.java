package org.esonov.clean_architecture.order.application.usecase;

import org.esonov.clean_architecture.order.application.port.out.OrderRepository;
import org.esonov.clean_architecture.order.application.port.out.OrderRepositoryContract;

/** Proves the test double honours the same contract as the real persistence adapter. */
class InMemoryOrderRepositoryTest extends OrderRepositoryContract {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    @Override
    protected OrderRepository repository() {
        return repository;
    }
}
