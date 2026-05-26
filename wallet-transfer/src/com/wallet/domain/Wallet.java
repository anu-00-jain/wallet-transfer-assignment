package com.wallet.domain;

import java.math.BigDecimal;

/**
 * Immutable snapshot of a wallet row, including its optimistic-lock version.
 *
 * @param id      unique wallet identifier
 * @param balance current available balance
 * @param version monotonically increasing counter incremented on every balance update
 */
public record Wallet(String id, BigDecimal balance, long version) {

    /**
     * Returns the new balance after debiting {@code amount}.
     *
     * @param amount positive value to subtract
     * @return resulting balance
     * @throws ArithmeticException if {@code balance} is less than {@code amount}
     */
    public BigDecimal debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new ArithmeticException("Insufficient funds in wallet: " + id);
        }
        return balance.subtract(amount);
    }

    /**
     * Returns the new balance after crediting {@code amount}.
     *
     * @param amount positive value to add
     * @return resulting balance
     */
    public BigDecimal credit(BigDecimal amount) {
        return balance.add(amount);
    }
}
