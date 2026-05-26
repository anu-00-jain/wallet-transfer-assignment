package com.wallet.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void debit_sufficientBalance_returnsReducedBalance() {
        var wallet = new Wallet("w1", BigDecimal.valueOf(100), 0);
        var result = wallet.debit(BigDecimal.valueOf(40));
        assertEquals(0, result.compareTo(BigDecimal.valueOf(60)));
    }

    @Test
    void debit_insufficientBalance_throws() {
        var wallet = new Wallet("w1", BigDecimal.valueOf(30), 0);
        assertThrows(ArithmeticException.class, () -> wallet.debit(BigDecimal.valueOf(50)));
    }

    @Test
    void debit_exactBalance_returnsZero() {
        var wallet = new Wallet("w1", BigDecimal.valueOf(100), 0);
        var result = wallet.debit(BigDecimal.valueOf(100));
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void credit_addsToBalance() {
        var wallet = new Wallet("w1", BigDecimal.valueOf(50), 0);
        var result = wallet.credit(BigDecimal.valueOf(25));
        assertEquals(0, result.compareTo(BigDecimal.valueOf(75)));
    }
}
