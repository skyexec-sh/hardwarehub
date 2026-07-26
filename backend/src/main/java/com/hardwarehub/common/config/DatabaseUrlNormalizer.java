package com.hardwarehub.common.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Normalizes Neon / Heroku-style {@code postgres://} or {@code postgresql://} URLs
 * into JDBC settings Spring Boot expects.
 */
public final class DatabaseUrlNormalizer {

    public record NormalizedDb(String jdbcUrl, String username, String password) {
    }

    private DatabaseUrlNormalizer() {
    }

    /**
     * @return normalized JDBC settings, or {@code null} when {@code raw} is already JDBC / not a postgres URI
     */
    public static NormalizedDb normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("jdbc:")) {
            return null;
        }
        if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
            return null;
        }

        try {
            URI uri = URI.create(trimmed.replaceFirst("^postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            String user = null;
            String password = null;
            if (userInfo != null && !userInfo.isBlank()) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    user = decode(userInfo.substring(0, colon));
                    password = decode(userInfo.substring(colon + 1));
                } else {
                    user = decode(userInfo);
                }
            }

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() == null ? "" : uri.getPath();
            String database = path.startsWith("/") ? path.substring(1) : path;
            String query = uri.getRawQuery();
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(':')
                    .append(port)
                    .append('/')
                    .append(database);
            if (query == null || query.isBlank()) {
                jdbc.append("?sslmode=require");
            } else {
                jdbc.append('?').append(query);
                if (!query.contains("sslmode=")) {
                    jdbc.append("&sslmode=require");
                }
            }

            return new NormalizedDb(jdbc.toString(), user, password);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid DATABASE_URL for PostgreSQL: " + trimmed, ex);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
