package com.wallet.repository;

import com.wallet.domain.Transfer;
import com.wallet.domain.TransferStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access contract for {@link Transfer} persistence.
 */
public interface TransferRepository {
    /**
     * Looks up a transfer by its idempotency key within the current transaction.
     *
     * @param conn active transactional connection
     * @param key  caller-supplied deduplication key
     * @return the matching transfer, or {@link Optional#empty()} if none exists
     * @throws SQLException on any database error
     */
    Optional<Transfer> findByIdempotencyKey(Connection conn, String key) throws SQLException;

    /**
     * Inserts a new transfer row and returns the record populated with the DB-assigned id
     * and {@code created_at} timestamp.
     *
     * @param conn     active transactional connection
     * @param transfer transfer to persist (id and createdAt may be {@code null})
     * @return persisted transfer with id and createdAt set
     * @throws SQLException on any database error, including unique-key violation (SQL state {@code 23505})
     */
    Transfer insert(Connection conn, Transfer transfer) throws SQLException;

    /**
     * Updates the status and optional failure reason of an existing transfer.
     *
     * @param conn          active transactional connection
     * @param id            UUID of the transfer to update
     * @param status        new {@link TransferStatus}
     * @param failureReason human-readable reason; {@code null} for non-failed statuses
     * @throws SQLException on any database error
     */
    void updateStatus(Connection conn, UUID id, TransferStatus status, String failureReason) throws SQLException;
}
