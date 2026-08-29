CREATE TABLE IF NOT EXISTS users (
    id       BINARY(16)   NOT NULL,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    age      INT          NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'USER',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT chk_users_age CHECK (age >= 0 AND age <= 150)
);
