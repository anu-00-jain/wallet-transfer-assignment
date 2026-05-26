package com.wallet.config;

/**
 * Immutable application configuration sourced from environment variables.
 *
 * @param dbUrl      JDBC URL for the PostgreSQL database
 * @param dbUser     database username
 * @param dbPassword database password
 * @param port       HTTP port the server listens on
 */
public record Config(String dbUrl, String dbUser, String dbPassword, int port) {

    /**
     * Builds a {@link Config} from environment variables, falling back to
     * development defaults when a variable is absent or blank.
     *
     * <ul>
     *   <li>{@code DB_URL}      — defaults to {@code jdbc:postgresql://localhost:5432/wallet}</li>
     *   <li>{@code DB_USER}     — defaults to {@code wallet}</li>
     *   <li>{@code DB_PASSWORD} — defaults to {@code secret}</li>
     *   <li>{@code PORT}        — defaults to {@code 8080}</li>
     * </ul>
     *
     * @return populated {@link Config}
     */
    public static Config fromEnv() {
        return new Config(
                env("DB_URL",      "jdbc:postgresql://localhost:5432/wallet"),
                env("DB_USER",     "wallet"),
                env("DB_PASSWORD", "secret"),
                Integer.parseInt(env("PORT", "8080"))
        );
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : fallback;
    }
}