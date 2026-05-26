package com.wallet.exception;

/**
 * Thrown when a transfer request specifies the same wallet as both source and destination.
 */
public class SelfTransferException extends RuntimeException {
    public SelfTransferException() {
        super("fromWalletId and toWalletId must be different");
    }
}
