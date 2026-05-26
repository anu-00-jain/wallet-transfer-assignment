package com.wallet.db;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database bootstrap utilities: creates the {@link DataSource} and initialises the schema.
 */
public class Database {

    /**
     * Creates a PostgreSQL {@link DataSource} from the supplied credentials.
     *
     * @param url      JDBC connection URL
     * @param user     database username
     * @param password database password
     * @return configured {@link DataSource}
     */
    public static DataSource create(String url, String user, String password) {
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(user);
        ds.setPassword(password);
        return ds;
    }

    /** Idempotent schema setup — runs on every startup. */
    public static void initSchema(DataSource ds) throws SQLException {
        try (Connection conn = ds.getConnection();
             var stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS wallets (
                    id         VARCHAR(50)    PRIMARY KEY,
                    balance    NUMERIC(19,4)  NOT NULL DEFAULT 0,
                    version    BIGINT         NOT NULL DEFAULT 0,
                    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transfers (
                    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                    idempotency_key VARCHAR(255)   NOT NULL UNIQUE,
                    from_wallet_id  VARCHAR(50)    NOT NULL REFERENCES wallets(id),
                    to_wallet_id    VARCHAR(50)    NOT NULL REFERENCES wallets(id),
                    amount          NUMERIC(19,4)  NOT NULL,
                    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
                    failure_reason  TEXT,
                    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ledger_entries (
                    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                    transfer_id UUID           NOT NULL REFERENCES transfers(id),
                    wallet_id   VARCHAR(50)    NOT NULL REFERENCES wallets(id),
                    entry_type  VARCHAR(10)    NOT NULL,
                    amount      NUMERIC(19,4)  NOT NULL,
                    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
                )
                """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transfers_idempotency ON transfers(idempotency_key)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_transfer        ON ledger_entries(transfer_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_wallet          ON ledger_entries(wallet_id)");

            // Seed wallets (no-op if already present)
            stmt.execute("""
                INSERT INTO wallets (id, balance) VALUES
                    ('wallet_1', 1000.0000),
                    ('wallet_2',  500.0000),
                    ('wallet_3',  250.0000)
                ON CONFLICT (id) DO NOTHING
                """);
        }
    }
}
