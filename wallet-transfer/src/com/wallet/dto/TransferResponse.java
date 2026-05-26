package com.wallet.dto;

import com.wallet.domain.Transfer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serialisable representation of a {@link com.wallet.domain.Transfer} returned by the API.
 *
 * @param transferId     UUID of the transfer record ({@code null} when not yet persisted)
 * @param idempotencyKey caller-supplied deduplication key echoed back in the response
 * @param fromWalletId   debited wallet
 * @param toWalletId     credited wallet
 * @param amount         monetary value of the transfer
 * @param status         string form of {@link com.wallet.domain.TransferStatus}
 * @param failureReason  non-null only when {@code status} is {@code FAILED}
 */
public record TransferResponse(
        UUID transferId,
        String idempotencyKey,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        String status,
        String failureReason
) {
    /**
     * Maps a domain {@link Transfer} to its API response representation.
     *
     * @param t source transfer
     * @return populated {@link TransferResponse}
     */
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.id(), t.idempotencyKey(), t.fromWalletId(), t.toWalletId(),
                t.amount(), t.status().name(), t.failureReason()
        );
    }

    /**
     * Converts this response to an ordered map suitable for JSON serialisation via
     * {@link com.wallet.util.Json#toJson}.
     *
     * @return insertion-ordered map of response fields
     */
    public Map<String, Object> toMap() {
        var m = new LinkedHashMap<String, Object>();
        m.put("transferId",     transferId != null ? transferId.toString() : null);
        m.put("idempotencyKey", idempotencyKey);
        m.put("fromWalletId",   fromWalletId);
        m.put("toWalletId",     toWalletId);
        m.put("amount",         amount);
        m.put("status",         status);
        m.put("failureReason",  failureReason);
        return m;
    }
}
