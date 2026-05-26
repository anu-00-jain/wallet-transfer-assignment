package com.wallet.repository;

import com.wallet.domain.EntryType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * PostgreSQL implementation of {@link LedgerEntryRepository}.
 */
public class PostgresLedgerEntryRepository implements LedgerEntryRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(Connection conn, UUID transferId, String walletId, EntryType type, BigDecimal amount)
            throws SQLException {
        String sql = """
                INSERT INTO ledger_entries (transfer_id, wallet_id, entry_type, amount)
                VALUES (?, ?, ?, ?)
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, transferId);
            ps.setString(2, walletId);
            ps.setString(3, type.name());
            ps.setBigDecimal(4, amount);
            ps.executeUpdate();
        }
    }
}
