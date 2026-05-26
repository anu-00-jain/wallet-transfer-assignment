package com.wallet.db;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * JDBC-backed {@link Transaction} implementation.
 * Obtains a connection from the pool, disables auto-commit, and rolls back on any exception.
 */
public class JdbcTransaction implements Transaction {

    private final DataSource dataSource;

    /**
     * @param dataSource connection pool to borrow connections from
     */
    public JdbcTransaction(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes {@code work} inside a single JDBC transaction.
     * Commits on success; rolls back and rethrows on any exception.
     *
     * @param <T>  return type of the unit of work
     * @param work the database operations to perform
     * @return result produced by {@code work}
     * @throws Exception propagated from {@code work} after rollback
     */
    @Override
    public <T> T run(Work<T> work) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
