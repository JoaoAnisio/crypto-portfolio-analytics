CREATE TABLE portfolio_snapshots (
    id                  UUID           PRIMARY KEY,
    snapshot_date       DATE           NOT NULL,
    currency            VARCHAR(10)    NOT NULL,
    total_invested      NUMERIC(20, 2) NOT NULL,
    total_current_value NUMERIC(20, 2) NOT NULL,
    unrealized_pnl      NUMERIC(20, 2) NOT NULL,
    realized_pnl        NUMERIC(20, 2) NOT NULL,
    open_positions      INTEGER        NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_snapshots_date UNIQUE (snapshot_date)
);

CREATE INDEX idx_snapshots_date ON portfolio_snapshots (snapshot_date DESC);