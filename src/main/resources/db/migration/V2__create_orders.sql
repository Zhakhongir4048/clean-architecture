-- customer_id has no foreign key to user_account on purpose: the order feature does not own that table,
-- and features must stay independent (see src/ARCHITECTURE.md). Existence is checked by CreateOrderInteractor.
CREATE TABLE orders
(
    id          UUID        PRIMARY KEY,
    customer_id UUID        NOT NULL,
    currency    VARCHAR(3)  NOT NULL,
    placed_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_orders_customer_id ON orders (customer_id);

CREATE TABLE order_line
(
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    line_no      INT            NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(19, 4) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id, line_no)
);
