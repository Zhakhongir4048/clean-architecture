CREATE TABLE user_account
(
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    registered_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_user_account_email UNIQUE (email)
);
