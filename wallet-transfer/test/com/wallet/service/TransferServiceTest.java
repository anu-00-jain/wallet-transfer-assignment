package com.wallet.service;

import com.wallet.db.Transaction;
import com.wallet.domain.*;
import com.wallet.dto.TransferRequest;
import com.wallet.exception.SelfTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    // --- Fakes ---

    /** Transaction that passes null as Connection; fakes ignore it. */
    static final Transaction NO_OP_TX = new Transaction() {
        @Override public <T> T run(Work<T> work) throws Exception { return work.execute(null); }
    };

    static class FakeWalletRepository implements WalletRepository {
        final Map<String, Wallet> store;
        final List<String> lockOrder = new ArrayList<>();

        FakeWalletRepository(Map<String, Wallet> initial) { this.store = new HashMap<>(initial); }

        @Override
        public Optional<Wallet> findByIdForUpdate(Connection c, String id) {
            lockOrder.add(id);
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void updateBalance(Connection c, String id, BigDecimal balance) {
            store.computeIfPresent(id, (k, w) -> new Wallet(k, balance, w.version() + 1));
        }
    }

    static class FakeTransferRepository implements TransferRepository {
        final Map<String, Transfer> store = new LinkedHashMap<>();

        @Override
        public Optional<Transfer> findByIdempotencyKey(Connection c, String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public Transfer insert(Connection c, Transfer t) {
            Transfer saved = t.withId(UUID.randomUUID(), Instant.now());
            store.put(saved.idempotencyKey(), saved);
            return saved;
        }

        @Override
        public void updateStatus(Connection c, UUID id, TransferStatus status, String reason) {
            store.replaceAll((k, v) -> v.id().equals(id) ? v.withStatus(status, reason) : v);
        }
    }

    static class FakeLedgerRepository implements LedgerEntryRepository {
        final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public void insert(Connection c, UUID transferId, String walletId, EntryType type, BigDecimal amount) {
            entries.add(new LedgerEntry(UUID.randomUUID(), transferId, walletId, type, amount, Instant.now()));
        }
    }

    // --- Setup ---

    FakeWalletRepository walletRepo;
    FakeTransferRepository transferRepo;
    FakeLedgerRepository ledgerRepo;
    TransferService service;

    @BeforeEach
    void setUp() {
        walletRepo   = new FakeWalletRepository(Map.of(
                "wallet_a", new Wallet("wallet_a", BigDecimal.valueOf(100), 0),
                "wallet_b", new Wallet("wallet_b", BigDecimal.valueOf(0),   0)
        ));
        transferRepo = new FakeTransferRepository();
        ledgerRepo   = new FakeLedgerRepository();
        service      = new TransferService(NO_OP_TX, walletRepo, transferRepo, ledgerRepo);
    }

    // --- Tests ---

    @Test
    void happyPath_processesTransferAndCreatesLedgerEntries() {
        var req = new TransferRequest("k1", "wallet_a", "wallet_b", BigDecimal.valueOf(50));
        var res = service.executeTransfer(req);

        assertEquals("PROCESSED", res.status());
        assertEquals(0, walletRepo.store.get("wallet_a").balance().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, walletRepo.store.get("wallet_b").balance().compareTo(BigDecimal.valueOf(50)));
        assertEquals(2, ledgerRepo.entries.size());
        assertTrue(ledgerRepo.entries.stream().anyMatch(e -> e.entryType() == EntryType.DEBIT  && e.walletId().equals("wallet_a")));
        assertTrue(ledgerRepo.entries.stream().anyMatch(e -> e.entryType() == EntryType.CREDIT && e.walletId().equals("wallet_b")));
    }

    @Test
    void idempotency_duplicateKeyReturnsOriginalResultWithoutSideEffects() {
        var req = new TransferRequest("k-dup", "wallet_a", "wallet_b", BigDecimal.valueOf(30));
        var first  = service.executeTransfer(req);
        var second = service.executeTransfer(req);

        assertEquals(first.transferId(), second.transferId());
        assertEquals("PROCESSED", second.status());
        // Balance changed only once
        assertEquals(0, walletRepo.store.get("wallet_a").balance().compareTo(BigDecimal.valueOf(70)));
        assertEquals(1, transferRepo.store.size());
        assertEquals(2, ledgerRepo.entries.size());
    }

    @Test
    void insufficientFunds_marksFailedNoBalanceChangeNoLedgerEntries() {
        var req = new TransferRequest("k2", "wallet_a", "wallet_b", BigDecimal.valueOf(999));
        var res = service.executeTransfer(req);

        assertEquals("FAILED", res.status());
        assertNotNull(res.failureReason());
        assertEquals(0, walletRepo.store.get("wallet_a").balance().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, walletRepo.store.get("wallet_b").balance().compareTo(BigDecimal.valueOf(0)));
        assertTrue(ledgerRepo.entries.isEmpty());
    }

    @Test
    void walletNotFound_throwsWalletNotFoundException() {
        var req = new TransferRequest("k3", "wallet_missing", "wallet_b", BigDecimal.valueOf(10));
        assertThrows(WalletNotFoundException.class, () -> service.executeTransfer(req));
        assertTrue(ledgerRepo.entries.isEmpty());
    }

    @Test
    void selfTransfer_throwsSelfTransferException() {
        var req = new TransferRequest("k4", "wallet_a", "wallet_a", BigDecimal.valueOf(10));
        assertThrows(SelfTransferException.class, () -> service.executeTransfer(req));
    }

    @Test
    void lockOrder_walletsLockedAlphabeticallyRegardlessOfRequestOrder() {
        // fromWalletId=wallet_b, toWalletId=wallet_a → wallet_a must be locked first
        walletRepo.store.put("wallet_a", new Wallet("wallet_a", BigDecimal.valueOf(0),  0));
        walletRepo.store.put("wallet_b", new Wallet("wallet_b", BigDecimal.valueOf(100), 0));
        var req = new TransferRequest("k5", "wallet_b", "wallet_a", BigDecimal.valueOf(10));
        service.executeTransfer(req);

        assertEquals(List.of("wallet_a", "wallet_b"), walletRepo.lockOrder);
    }

    @Test
    void insufficientFunds_idempotentRetryReturnsSameFailedResult() {
        var req = new TransferRequest("k-fail", "wallet_a", "wallet_b", BigDecimal.valueOf(999));
        var first  = service.executeTransfer(req);
        var second = service.executeTransfer(req);

        assertEquals(first.transferId(), second.transferId());
        assertEquals("FAILED", second.status());
    }
}