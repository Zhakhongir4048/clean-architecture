package org.esonov.clean_architecture.order.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/** Request model crossing the input boundary. Deliberately raw: validation belongs to the domain. */
public record CreateOrderCommand(String customerId, String currency, List<Line> lines) {

    public record Line(String productName, int quantity, BigDecimal unitPrice) {
    }
}
