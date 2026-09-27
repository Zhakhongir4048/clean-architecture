package org.esonov.clean_architecture.order.adapter.in.web;

import org.esonov.clean_architecture.order.adapter.in.web.OrderPresenter.OrderViewModel;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderCommand;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.DeleteOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.GetOrderQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller: translates HTTP into use case request models and calls input ports.
 * No business rules, no response formatting ({@link OrderPresenter}).
 */
@RestController
@RequestMapping(OrderPresenter.BASE_PATH)
class OrderController {

    private final CreateOrderUseCase createOrder;
    private final GetOrderQuery getOrder;
    private final DeleteOrderUseCase deleteOrder;
    private final OrderPresenter presenter;

    OrderController(CreateOrderUseCase createOrder, GetOrderQuery getOrder, DeleteOrderUseCase deleteOrder,
                    OrderPresenter presenter) {
        this.createOrder = createOrder;
        this.getOrder = getOrder;
        this.deleteOrder = deleteOrder;
        this.presenter = presenter;
    }

    @PostMapping
    ResponseEntity<OrderViewModel> create(@RequestBody CreateOrderHttpRequest request) {
        return presenter.created(createOrder.create(request.toCommand()));
    }

    @GetMapping("/{id}")
    ResponseEntity<OrderViewModel> getById(@PathVariable String id) {
        return presenter.found(getOrder.getById(id));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        deleteOrder.delete(id);
        return presenter.deleted();
    }

    /**
     * HTTP body exactly as it arrives. Missing text/number fields arrive as {@code null} and the domain rejects
     * them; a missing {@code quantity} (a primitive) is already rejected by Jackson with 400 before this point.
     */
    record CreateOrderHttpRequest(String customerId, String currency, List<Line> lines) {

        record Line(String productName, int quantity, BigDecimal unitPrice) {
        }

        CreateOrderCommand toCommand() {
            List<CreateOrderCommand.Line> commandLines = lines == null ? null : lines.stream()
                    .map(l -> new CreateOrderCommand.Line(l.productName(), l.quantity(), l.unitPrice()))
                    .toList();
            return new CreateOrderCommand(customerId, currency, commandLines);
        }
    }
}
