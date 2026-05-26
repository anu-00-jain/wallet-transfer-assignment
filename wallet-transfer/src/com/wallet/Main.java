package com.wallet;

import com.sun.net.httpserver.HttpServer;
import com.wallet.config.Config;
import com.wallet.db.Database;
import com.wallet.db.JdbcTransaction;
import com.wallet.handler.TransferHandler;
import com.wallet.repository.PostgresLedgerEntryRepository;
import com.wallet.repository.PostgresTransferRepository;
import com.wallet.repository.PostgresWalletRepository;
import com.wallet.service.TransferService;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Application entry point. Wires together the HTTP server, database, and service layer.
 */
public class Main {

    /**
     * Bootstraps the wallet service: loads config from environment, initialises the
     * database schema, builds the service layer, and starts the HTTP server.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if the server cannot bind or the database is unreachable
     */
    public static void main(String[] args) throws Exception {
        var config = Config.fromEnv();

        var dataSource = Database.create(config.dbUrl(), config.dbUser(), config.dbPassword());

        // Initialize some data points in the table wallets
        Database.initSchema(dataSource);

        var service = new TransferService(
                new JdbcTransaction(dataSource),
                new PostgresWalletRepository(),
                new PostgresTransferRepository(),
                new PostgresLedgerEntryRepository()
        );

        var server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/transfers", new TransferHandler(service));
        // Java 21 virtual threads: one per request, blocks cheaply on JDBC
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.printf("Wallet service started on port %d%n", config.port());
    }
}