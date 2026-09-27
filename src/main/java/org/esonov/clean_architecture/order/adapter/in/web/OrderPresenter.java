package org.esonov.clean_architecture.order.adapter.in.web;

import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/** Presenter: turns the order response model into the HTTP view model (status, headers, JSON shape). */
@Component
class OrderPresenter {

    static final String BASE_PATH = "/api/orders";

    ResponseEntity<OrderViewModel> created(OrderView view) {
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + view.id())).body(toViewModel(view));
    }

    ResponseEntity<OrderViewModel> found(OrderView view) {
        return ResponseEntity.ok(toViewModel(view));
    }

    ResponseEntity<Void> deleted() {
        return ResponseEntity.noContent().build();
    }

    private static OrderViewModel toViewModel(OrderView view) {
        return new OrderViewModel(
                view.id(),
                view.customerId(),
                view.currency(),
                view.lines().stream()
                        .map(l -> new OrderViewModel.Line(l.productName(), l.quantity(),
                                l.unitPrice().toPlainString(), l.lineTotal().toPlainString()))
                        .toList(),
                view.total().toPlainString(),
                view.placedAt().toString());
    }

    /** JSON shape of an order. Money is a string ("12.50") so clients never lose precision to floating point. */
    record OrderViewModel(String id, String customerId, String currency, List<Line> lines, String total,
                          String placedAt) {

        record Line(String productName, int quantity, String unitPrice, String lineTotal) {
        }
    }
}
