package com.wallet.domain;

/**
 * Direction of a double-entry ledger posting.
 *
 * <ul>
 *   <li>{@link #DEBIT}  — funds leaving a wallet</li>
 *   <li>{@link #CREDIT} — funds entering a wallet</li>
 * </ul>
 */
public enum EntryType { DEBIT, CREDIT }
