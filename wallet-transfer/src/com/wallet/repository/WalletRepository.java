package com.wallet.repository;

import com.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data-access contract for {@link Wallet} persistence.
 */
public interface WalletRepository {
    /**
     * Fetches a wallet and acquires a pessimistic row lock ({@code SELECT … FOR UPDATE})
     * to prevent concurrent balance modifications within the same transaction.
     *
     * @param conn active transactional connection
     * @param id   wallet identifier
     * @return the locked wallet snapshot, or {@link Optional#empty()} if not found
     * @throws SQLException on any database error
     */
    Optional<Wallet> findByIdForUpdate(Connection conn, String id) throws SQLException;

    /**
     * Overwrites the balance and increments the optimistic-lock version counter.
     *
     * @param conn       active transactional connection
     * @param id         wallet identifier
     * @param newBalance replacement balance
     * @throws SQLException on any database error
     */
    void updateBalance(Connection conn, String id, BigDecimal newBalance) throws SQLException;
}
