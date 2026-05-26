package com.wallet.domain;

/**
 * Lifecycle state of a {@link Transfer}.
 *
 * <ul>
 *   <li>{@link #PENDING}   — inserted but not yet settled</li>
 *   <li>{@link #PROCESSED} — balances updated and ledger entries written</li>
 *   <li>{@link #FAILED}    — rejected (e.g. insufficient funds); persisted so retries return the same result</li>
 * </ul>
 */
public enum TransferStatus { PENDING, PROCESSED, FAILED }
