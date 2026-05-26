package com.wallet.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validated, immutable representation of an inbound transfer API request.
 *
 * @param idempotencyKey caller-supplied key that makes the request safe to retry
 * @param fromWalletId   wallet to debit
 * @param toWalletId     wallet to credit
 * @param amount         positive monetary value to transfer
 */
public record TransferRequest(
        String idempotencyKey,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount
) {
    /**
     * Parses and validates a raw JSON map into a {@link TransferRequest}.
     * Collects all missing/blank field errors before throwing so callers receive
     * a complete error message in one pass.
     *
     * @param json flat map produced by {@link com.wallet.util.Json#parse}
     * @return validated request
     * @throws IllegalArgumentException if any required field is missing, blank, or {@code amount ≤ 0}
     */
    public static TransferRequest from(Map<String, Object> json) {
        var errors = new ArrayList<String>();

        String key  = stringField(json, "idempotencyKey", errors);
        String from = stringField(json, "fromWalletId",   errors);
        String to   = stringField(json, "toWalletId",     errors);

        Object rawAmt = json.get("amount");
        if (rawAmt == null) {
            errors.add("amount is required");
        }

        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));

        BigDecimal amount = rawAmt instanceof BigDecimal bd ? bd : new BigDecimal(rawAmt.toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new TransferRequest(key, from, to, amount);
    }

    private static String stringField(Map<String, Object> json, String field, List<String> errors) {
        Object v = json.get(field);
        if (v == null || v.toString().isBlank()) {
            errors.add(field + " is required");
            return null;
        }
        return v.toString();
    }
}
