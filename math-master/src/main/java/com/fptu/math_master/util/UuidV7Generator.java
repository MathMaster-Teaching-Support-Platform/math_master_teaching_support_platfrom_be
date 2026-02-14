package com.fptu.math_master.util;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Custom Hibernate UUID v7 generator.
 * UUIDv7 is time-ordered, making it ideal for database indexes and sorting by creation time.
 */
public class UuidV7Generator implements BeforeExecutionGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Annotation to apply UUIDv7 generation to an entity field
     */
    @IdGeneratorType(UuidV7Generator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({FIELD, METHOD})
    public @interface UuidV7 {
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return generateUuidV7();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }

    /**
     * Generates a UUIDv7 based on the current timestamp.
     * Format: xxxxxxxx-xxxx-7xxx-yxxx-xxxxxxxxxxxx
     * where 'x' is random and 'y' is variant bits (10xx)
     */
    public static UUID generateUuidV7() {
        // Get current timestamp in milliseconds
        long unixTsMs = Instant.now().toEpochMilli();

        // Generate random bytes
        byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);

        // Set timestamp in first 48 bits (6 bytes)
        randomBytes[0] = (byte) ((unixTsMs >> 40) & 0xFF);
        randomBytes[1] = (byte) ((unixTsMs >> 32) & 0xFF);
        randomBytes[2] = (byte) ((unixTsMs >> 24) & 0xFF);
        randomBytes[3] = (byte) ((unixTsMs >> 16) & 0xFF);
        randomBytes[4] = (byte) ((unixTsMs >> 8) & 0xFF);
        randomBytes[5] = (byte) (unixTsMs & 0xFF);

        // Set version to 7 (0b0111) in bits 48-51
        randomBytes[6] = (byte) ((randomBytes[6] & 0x0F) | 0x70);

        // Set variant to 10xx in bits 64-65
        randomBytes[8] = (byte) ((randomBytes[8] & 0x3F) | 0x80);

        // Convert bytes to UUID
        long mostSigBits = 0;
        long leastSigBits = 0;

        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (randomBytes[i] & 0xFF);
        }

        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xFF);
        }

        return new UUID(mostSigBits, leastSigBits);
    }
}
