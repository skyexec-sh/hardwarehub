package com.hardwarehub.common.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Ensures Neon / Heroku {@code postgres(ql)://} URLs win over the raw env value
 * by installing a high-priority property source with JDBC settings.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                System.getenv("DATABASE_URL"));
        if (raw == null) {
            return;
        }

        DatabaseUrlNormalizer.NormalizedDb normalized = DatabaseUrlNormalizer.normalize(raw);
        if (normalized == null) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", normalized.jdbcUrl());
        props.put("spring.datasource.url", normalized.jdbcUrl());
        if (normalized.username() != null && blank(environment.getProperty("DATABASE_USERNAME"))) {
            props.put("DATABASE_USERNAME", normalized.username());
            props.put("spring.datasource.username", normalized.username());
        }
        if (normalized.password() != null && blank(environment.getProperty("DATABASE_PASSWORD"))) {
            props.put("DATABASE_PASSWORD", normalized.password());
            props.put("spring.datasource.password", normalized.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource("hardwarehubDatabaseUrl", props));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
