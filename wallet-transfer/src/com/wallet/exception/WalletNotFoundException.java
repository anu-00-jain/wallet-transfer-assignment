package com.wallet.exception;

/**
 * Thrown when a requested wallet does not exist in the database.
 */
public class WalletNotFoundException extends RuntimeException {
    /**
     * @param walletId the identifier that was not found
     */
    public WalletNotFoundException(String walletId) {
        super("Wallet not found: " + walletId);
    }
}
