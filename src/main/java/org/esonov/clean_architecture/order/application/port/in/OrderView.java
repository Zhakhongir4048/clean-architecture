package org.esonov.clean_architecture.order.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Response model crossing the input boundary back out. Pure data — no domain types. */
public record OrderView(String id, String customerId, String currency, List<Line> lines, BigDecimal total,
                        Instant placedAt) {

    public record Line(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
