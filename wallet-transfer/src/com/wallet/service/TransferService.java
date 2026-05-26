package com.wallet.service;

import com.wallet.db.Transaction;
import com.wallet.domain.EntryType;
import com.wallet.domain.Transfer;
import com.wallet.domain.TransferStatus;
import com.wallet.dto.TransferRequest;
import com.wallet.dto.TransferResponse;
import com.wallet.exception.SelfTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.WalletRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Core business logic for executing wallet-to-wallet transfers.
 *
 * <p>Transfer flow:
 * <ol>
 *   <li>Reject self-transfers immediately.</li>
 *   <li>Return the cached result for duplicate idempotency keys (fast path).</li>
 *   <li>Lock both wallets in alphabetical id order to prevent deadlocks under concurrency.</li>
 *   <li>Insert a {@link TransferStatus#PENDING} transfer row; handle concurrent duplicate keys
 *       via SQL state {@code 23505}.</li>
 *   <li>Validate the source wallet has sufficient funds; persist {@link TransferStatus#FAILED}
 *       if not so that retries receive the same deterministic result.</li>
 *   <li>Atomically update both balances and write double-entry ledger records.</li>
 *   <li>Mark the transfer {@link TransferStatus#PROCESSED}.</li>
 * </ol>
 */
public class TransferService {

    private final Transaction tx;
    private final WalletRepository walletRepo;
    private final TransferRepository transferRepo;
    private final LedgerEntryRepository ledgerRepo;

    /**
     * @param tx           transaction manager used to wrap all DB operations atomically
     * @param walletRepo   wallet read/write access
     * @param transferRepo transfer record persistence
     * @param ledgerRepo   double-entry ledger persistence
     */
    public TransferService(Transaction tx,
                           WalletRepository walletRepo,
                           TransferRepository transferRepo,
                           LedgerEntryRepository ledgerRepo) {
        this.tx          = tx;
        this.walletRepo  = walletRepo;
        this.transferRepo = transferRepo;
        this.ledgerRepo  = ledgerRepo;
    }

    /**
     * Validates and executes a transfer request inside a single database transaction.
     *
     * @param req validated transfer request
     * @return response reflecting the final transfer state
     * @throws SelfTransferException    if {@code fromWalletId} equals {@code toWalletId}
     * @throws WalletNotFoundException  if either wallet does not exist
     * @throws RuntimeException         wrapping any unexpected checked exception from the DB layer
     */
    public TransferResponse executeTransfer(TransferRequest req) {
        if (req.fromWalletId().equals(req.toWalletId())) throw new SelfTransferException();
        try {
            return tx.run(conn -> process(conn, req));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TransferResponse process(Connection conn, TransferRequest req) throws Exception {
        // Fast path: return stored result for duplicate idempotency keys
        var existing = transferRepo.findByIdempotencyKey(conn, req.idempotencyKey());
        if (existing.isPresent()) return TransferResponse.from(existing.get());

        // Lock wallets in deterministic alphabetical order to prevent deadlocks
        var ids = new String[]{req.fromWalletId(), req.toWalletId()};
        Arrays.sort(ids);

        var first  = walletRepo.findByIdForUpdate(conn, ids[0]).orElseThrow(() -> new WalletNotFoundException(ids[0]));
        var second = walletRepo.findByIdForUpdate(conn, ids[1]).orElseThrow(() -> new WalletNotFoundException(ids[1]));

        var fromWallet = first.id().equals(req.fromWalletId()) ? first : second;
        var toWallet   = first.id().equals(req.toWalletId())   ? first : second;

        // Insert PENDING transfer; unique constraint guards against concurrent duplicate keys
        Transfer transfer;
        try {
            transfer = transferRepo.insert(conn, Transfer.pending(
                    req.idempotencyKey(), req.fromWalletId(), req.toWalletId(), req.amount()));
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                // Concurrent request with same key already committed — return that result
                return TransferResponse.from(
                        transferRepo.findByIdempotencyKey(conn, req.idempotencyKey()).orElseThrow(() -> e));
            }
            throw e;
        }

        // Check balance — persist FAILED state so retries get the same result
        if (fromWallet.balance().compareTo(req.amount()) < 0) {
            String reason = "Insufficient funds in wallet: " + fromWallet.id();
            transferRepo.updateStatus(conn, transfer.id(), TransferStatus.FAILED, reason);
            return TransferResponse.from(transfer.withStatus(TransferStatus.FAILED, reason));
        }

        // Atomically update balances and write double-entry ledger
        walletRepo.updateBalance(conn, fromWallet.id(), fromWallet.debit(req.amount()));
        walletRepo.updateBalance(conn, toWallet.id(),   toWallet.credit(req.amount()));
        ledgerRepo.insert(conn, transfer.id(), fromWallet.id(), EntryType.DEBIT,  req.amount());
        ledgerRepo.insert(conn, transfer.id(), toWallet.id(),   EntryType.CREDIT, req.amount());

        transferRepo.updateStatus(conn, transfer.id(), TransferStatus.PROCESSED, null);
        return TransferResponse.from(transfer.withStatus(TransferStatus.PROCESSED, null));
    }
}
