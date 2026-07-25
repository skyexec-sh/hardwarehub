package com.hardwarehub.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DatabaseUrlNormalizerTest {

    @Test
    void normalizesNeonStyleUri() {
        DatabaseUrlNormalizer.NormalizedDb result = DatabaseUrlNormalizer.normalize(
                "postgresql://neondb_owner:s3cret@ep-cool.ap-southeast-1.aws.neon.tech/neondb?sslmode=require");

        assertNotNull(result);
        assertEquals(
                "jdbc:postgresql://ep-cool.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require",
                result.jdbcUrl());
        assertEquals("neondb_owner", result.username());
        assertEquals("s3cret", result.password());
    }

    @Test
    void leavesJdbcUrlsAlone() {
        assertNull(DatabaseUrlNormalizer.normalize(
                "jdbc:postgresql://localhost:5432/hardwarehub"));
    }
}
