package com.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain model representing a money movement between two wallets.
 *
 * @param id             DB-assigned UUID; {@code null} before the record is persisted
 * @param idempotencyKey caller-supplied key used to deduplicate concurrent or retried requests
 * @param fromWalletId   wallet being debited
 * @param toWalletId     wallet being credited
 * @param amount         positive monetary value to transfer
 * @param status         current lifecycle state
 * @param failureReason  human-readable explanation when {@code status} is {@link TransferStatus#FAILED}; otherwise {@code null}
 * @param createdAt      wall-clock time the row was inserted; {@code null} before persistence
 */
public record Transfer(
        UUID id,
        String idempotencyKey,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        TransferStatus status,
        String failureReason,
        Instant createdAt
) {
    /**
     * Factory for a new, unsaved transfer in {@link TransferStatus#PENDING} state.
     *
     * @param idempotencyKey caller-supplied deduplication key
     * @param from           source wallet identifier
     * @param to             destination wallet identifier
     * @param amount         positive amount to transfer
     * @return pending {@link Transfer} with {@code null} id and createdAt
     */
    public static Transfer pending(String idempotencyKey, String from, String to, BigDecimal amount) {
        return new Transfer(null, idempotencyKey, from, to, amount, TransferStatus.PENDING, null, null);
    }

    /**
     * Returns a copy of this transfer populated with the database-assigned id and timestamp.
     *
     * @param id        UUID assigned by the database
     * @param createdAt insertion timestamp from the database
     * @return new {@link Transfer} instance with id and createdAt set
     */
    public Transfer withId(UUID id, Instant createdAt) {
        return new Transfer(id, idempotencyKey, fromWalletId, toWalletId, amount, status, failureReason, createdAt);
    }

    /**
     * Returns a copy of this transfer with an updated status and optional failure reason.
     *
     * @param newStatus replacement {@link TransferStatus}
     * @param reason    human-readable reason; pass {@code null} for {@link TransferStatus#PROCESSED}
     * @return new {@link Transfer} instance with the updated status
     */
    public Transfer withStatus(TransferStatus newStatus, String reason) {
        return new Transfer(id, idempotencyKey, fromWalletId, toWalletId, amount, newStatus, reason, createdAt);
    }
}
