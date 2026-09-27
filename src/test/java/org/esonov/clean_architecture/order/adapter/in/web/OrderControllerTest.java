package org.esonov.clean_architecture.order.adapter.in.web;

import org.esonov.clean_architecture.order.application.exception.CustomerNotFoundException;
import org.esonov.clean_architecture.order.application.exception.OrderNotFoundException;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderCommand;
import org.esonov.clean_architecture.order.application.port.in.CreateOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.DeleteOrderUseCase;
import org.esonov.clean_architecture.order.application.port.in.GetOrderQuery;
import org.esonov.clean_architecture.order.application.port.in.OrderView;
import org.esonov.clean_architecture.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web adapter (controller + presenter + exception handlers) against mocked input ports. */
@WebMvcTest(OrderController.class)
@Import(OrderPresenter.class)
class OrderControllerTest {

    private static final String ORDER_ID = "7a1c2d3e-aaaa-4bbb-8ccc-ddddeeeeffff";
    private static final String CUSTOMER_ID = "3f2b8c1e-1111-4222-8333-444455556666";
    private static final OrderView ORDER = new OrderView(ORDER_ID, CUSTOMER_ID, "USD",
            List.of(new OrderView.Line("Pen", 3, new BigDecimal("1.25"), new BigDecimal("3.75"))),
            new BigDecimal("3.75"), Instant.parse("2026-09-28T10:00:00Z"));
    private static final String BODY = """
            {"customerId": "%s", "currency": "USD",
             "lines": [{"productName": "Pen", "quantity": 3, "unitPrice": 1.25}]}
            """.formatted(CUSTOMER_ID);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CreateOrderUseCase createOrder;

    @MockitoBean
    GetOrderQuery getOrder;

    @MockitoBean
    DeleteOrderUseCase deleteOrder;

    @Test
    void createTranslatesHttpToCommandAndReturns201() throws Exception {
        CreateOrderCommand expected = new CreateOrderCommand(CUSTOMER_ID, "USD",
                List.of(new CreateOrderCommand.Line("Pen", 3, new BigDecimal("1.25"))));
        given(createOrder.create(expected)).willReturn(ORDER);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + ORDER_ID))
                .andExpect(jsonPath("$.total").value("3.75"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value("1.25"))
                .andExpect(jsonPath("$.lines[0].lineTotal").value("3.75"))
                .andExpect(jsonPath("$.placedAt").value("2026-09-28T10:00:00Z"));
    }

    @Test
    void getReturnsOrder() throws Exception {
        given(getOrder.getById(ORDER_ID)).willReturn(ORDER);

        mvc.perform(get("/api/orders/" + ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/orders/" + ORDER_ID)).andExpect(status().isNoContent());

        verify(deleteOrder).delete(ORDER_ID);
    }

    @Test
    void mapsInnerExceptionsToHttpStatus() throws Exception {
        willThrow(new CustomerNotFoundException(CUSTOMER_ID)).given(createOrder).create(any());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableContent());

        willThrow(new DomainValidationException("An order must have at least one line")).given(createOrder).create(any());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An order must have at least one line"));

        given(getOrder.getById("nope")).willThrow(new OrderNotFoundException("nope"));
        mvc.perform(get("/api/orders/nope")).andExpect(status().isNotFound());

        willThrow(new OrderNotFoundException("nope")).given(deleteOrder).delete("nope");
        mvc.perform(delete("/api/orders/nope")).andExpect(status().isNotFound());
    }

    @Test
    void missingReferenceFieldsArePassedThroughAsNullSoTheDomainDecides() throws Exception {
        willThrow(new DomainValidationException("x")).given(createOrder).create(any());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": \"" + CUSTOMER_ID + "\", \"lines\": [{\"productName\": \"Pen\", \"quantity\": 1}]}"))
                .andExpect(status().isBadRequest());

        verify(createOrder).create(new CreateOrderCommand(CUSTOMER_ID, null,
                List.of(new CreateOrderCommand.Line("Pen", 1, null))));
    }

    @Test
    void missingQuantityIsRejectedByJsonBindingBeforeTheUseCase() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": \"" + CUSTOMER_ID + "\", \"currency\": \"USD\", \"lines\": [{\"productName\": \"Pen\", \"unitPrice\": 1}]}"))
                .andExpect(status().isBadRequest());

        verify(createOrder, never()).create(any());
    }

    @Test
    void malformedJsonNeverReachesTheUseCase() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());

        verify(createOrder, never()).create(any());
    }
}
