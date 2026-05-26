package com.wallet.repository;

import com.wallet.domain.Transfer;
import com.wallet.domain.TransferStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL implementation of {@link TransferRepository}.
 */
public class PostgresTransferRepository implements TransferRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Transfer> findByIdempotencyKey(Connection conn, String key) throws SQLException {
        String sql = """
                SELECT id, idempotency_key, from_wallet_id, to_wallet_id,
                       amount, status, failure_reason, created_at
                FROM transfers WHERE idempotency_key = ?
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Transfer(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("idempotency_key"),
                        rs.getString("from_wallet_id"),
                        rs.getString("to_wallet_id"),
                        rs.getBigDecimal("amount"),
                        TransferStatus.valueOf(rs.getString("status")),
                        rs.getString("failure_reason"),
                        rs.getTimestamp("created_at").toInstant()
                ));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer insert(Connection conn, Transfer transfer) throws SQLException {
        String sql = """
                INSERT INTO transfers (idempotency_key, from_wallet_id, to_wallet_id, amount, status)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, transfer.idempotencyKey());
            ps.setString(2, transfer.fromWalletId());
            ps.setString(3, transfer.toWalletId());
            ps.setBigDecimal(4, transfer.amount());
            ps.setString(5, transfer.status().name());
            try (var rs = ps.executeQuery()) {
                rs.next();
                return transfer.withId(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toInstant()
                );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(Connection conn, UUID id, TransferStatus status, String failureReason) throws SQLException {
        String sql = "UPDATE transfers SET status = ?, failure_reason = ? WHERE id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, failureReason);
            ps.setObject(3, id);
            ps.executeUpdate();
        }
    }
}
