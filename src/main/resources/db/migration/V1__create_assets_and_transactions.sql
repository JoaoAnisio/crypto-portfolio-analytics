CREATE TABLE assets (
    id           UUID         PRIMARY KEY,
    symbol       VARCHAR(20)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    coingecko_id VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_assets_symbol       UNIQUE (symbol),
    CONSTRAINT uk_assets_coingecko_id UNIQUE (coingecko_id)
);

CREATE TABLE transactions (
    id          UUID            PRIMARY KEY,
    asset_id    UUID            NOT NULL,
    type        VARCHAR(10)     NOT NULL,
    quantity    NUMERIC(38, 18) NOT NULL,
    unit_price  NUMERIC(20, 8)  NOT NULL,
    executed_at TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT fk_transactions_asset      FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT ck_transactions_type       CHECK (type IN ('BUY', 'SELL')),
    CONSTRAINT ck_transactions_quantity   CHECK (quantity > 0),
    CONSTRAINT ck_transactions_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_transactions_asset_executed
    ON transactions (asset_id, executed_at DESC);