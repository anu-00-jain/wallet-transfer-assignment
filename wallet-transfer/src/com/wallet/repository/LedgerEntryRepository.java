package com.wallet.repository;

import com.wallet.domain.EntryType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Data-access contract for {@link com.wallet.domain.LedgerEntry} persistence.
 */
public interface LedgerEntryRepository {
    /**
     * Inserts a new ledger entry within the supplied connection's transaction.
     *
     * @param conn       active transactional connection
     * @param transferId owning transfer UUID
     * @param walletId   affected wallet identifier
     * @param type       {@link EntryType#DEBIT} or {@link EntryType#CREDIT}
     * @param amount     positive monetary value
     * @throws SQLException on any database error
     */
    void insert(Connection conn, UUID transferId, String walletId, EntryType type, BigDecimal amount) throws SQLException;
}
