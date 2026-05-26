package com.wallet.repository;

import com.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link WalletRepository}.
 */
public class PostgresWalletRepository implements WalletRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Wallet> findByIdForUpdate(Connection conn, String id) throws SQLException {
        String sql = "SELECT id, balance, version FROM wallets WHERE id = ? FOR UPDATE";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Wallet(
                        rs.getString("id"),
                        rs.getBigDecimal("balance"),
                        rs.getLong("version")
                ));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBalance(Connection conn, String id, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE wallets SET balance = ?, version = version + 1 WHERE id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }
}
