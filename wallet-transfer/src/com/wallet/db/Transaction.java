package com.wallet.db;

import java.sql.Connection;

/**
 * Abstracts transaction lifecycle so TransferService can be tested without a real DataSource.
 */
public interface Transaction {

    /**
     * A unit of work that receives an open {@link Connection} and produces a result.
     *
     * @param <T> type of the result
     */
    @FunctionalInterface
    interface Work<T> {
        /**
         * @param conn active, transaction-scoped connection
         * @return computed result
         * @throws Exception on any database or business error
         */
        T execute(Connection conn) throws Exception;
    }

    /**
     * Runs {@code work} inside a single database transaction.
     *
     * @param <T>  return type
     * @param work operations to perform atomically
     * @return result of {@code work}
     * @throws Exception on failure (implementation is expected to roll back)
     */
    <T> T run(Work<T> work) throws Exception;
}
