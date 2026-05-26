package com.wallet.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.wallet.dto.TransferRequest;
import com.wallet.exception.SelfTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.service.TransferService;
import com.wallet.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP handler for {@code POST /transfers}.
 *
 * <p>Accepts a JSON body, delegates to {@link TransferService#executeTransfer}, and maps
 * domain exceptions to appropriate HTTP status codes:
 * <ul>
 *   <li>400 — malformed or missing request fields</li>
 *   <li>404 — wallet not found</li>
 *   <li>405 — non-POST method</li>
 *   <li>422 — self-transfer attempt</li>
 *   <li>500 — unexpected internal error</li>
 * </ul>
 */
public class TransferHandler implements HttpHandler {

    private static final String ERROR_KEY = "error";

    private final TransferService service;

    /**
     * @param service business logic layer that executes transfers
     */
    public TransferHandler(TransferService service) {
        this.service = service;
    }

    /**
     * Handles an incoming HTTP exchange. Only {@code POST} is accepted.
     *
     * @param exchange the HTTP request/response pair
     * @throws IOException on I/O failure writing the response
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of(ERROR_KEY, "Method not allowed"));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        TransferRequest req;
        try {
            req = TransferRequest.from(Json.parse(body));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, Map.of(ERROR_KEY, e.getMessage()));
            return;
        }

        try {
            var response = service.executeTransfer(req);
            send(exchange, 200, response.toMap());
        } catch (SelfTransferException e) {
            send(exchange, 422, Map.of(ERROR_KEY, e.getMessage()));
        } catch (WalletNotFoundException e) {
            send(exchange, 404, Map.of(ERROR_KEY, e.getMessage()));
        } catch (Exception e) {
            send(exchange, 500, Map.of(ERROR_KEY, "Internal server error"));
        }
    }

    private void send(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
        byte[] bytes = Json.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
