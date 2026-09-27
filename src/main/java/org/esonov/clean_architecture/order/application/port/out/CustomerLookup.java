package org.esonov.clean_architecture.order.application.port.out;

import org.esonov.clean_architecture.order.domain.CustomerId;

/**
 * Output port: the order feature's own view of "customers". It says what orders need to know,
 * in order vocabulary, and nothing about who answers it.
 * <p>
 * Implemented by {@code order.adapter.out.customer}, which asks the user feature through its input port —
 * the only sanctioned way for one feature to use another.
 */
public interface CustomerLookup {

    boolean exists(CustomerId id);
}
