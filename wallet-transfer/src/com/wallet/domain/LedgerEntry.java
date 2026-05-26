package com.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable double-entry ledger record that tracks a single debit or credit
 * against a wallet as part of a {@link Transfer}.
 *
 * @param id         unique ledger entry identifier (DB-assigned UUID)
 * @param transferId foreign key to the owning {@link Transfer}
 * @param walletId   wallet affected by this entry
 * @param entryType  {@link EntryType#DEBIT} or {@link EntryType#CREDIT}
 * @param amount     positive monetary value of the entry
 * @param createdAt  wall-clock time the row was inserted
 */
public record LedgerEntry(
        UUID id,
        UUID transferId,
        String walletId,
        EntryType entryType,
        BigDecimal amount,
        Instant createdAt
) {}
