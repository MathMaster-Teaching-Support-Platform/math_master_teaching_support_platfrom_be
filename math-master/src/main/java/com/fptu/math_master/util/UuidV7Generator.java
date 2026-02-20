package com.fptu.math_master.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

/**
 * Custom Hibernate UUID v7 generator. UUIDv7 is time-ordered, making it ideal for database indexes
 * and sorting by creation time.
 *
 * <p>Changes from v1:
 *
 * <ul>
 *   <li>Fixed raw {@code EnumSet} → {@code EnumSet<EventType>}
 *   <li>Added monotonic counter to guarantee strict ordering within the same millisecond (RFC 9562
 *       §6.2)
 *   <li>Reuses a {@code ThreadLocal} byte buffer to reduce per-call heap allocation
 * </ul>
 */
public class UuidV7Generator implements BeforeExecutionGenerator {

  // -----------------------------------------------------------------------
  // Annotation
  // -----------------------------------------------------------------------

  @IdGeneratorType(UuidV7Generator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({FIELD, METHOD})
  public @interface UuidV7 {}

  // -----------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------

  private static final SecureRandom RANDOM = new SecureRandom();

  /** Reusable 10-byte buffer per thread — avoids allocating new byte[] on every call. */
  private static final ThreadLocal<byte[]> RANDOM_BUFFER =
      ThreadLocal.withInitial(() -> new byte[10]);

  /**
   * Monotonic counter, guarded by MONO_LOCK. Incremented when two UUIDs are generated in the same
   * millisecond. If counter overflows 12 bits, virtual timestamp is bumped by 1ms.
   */
  private static long lastTimestampMs = -1L;

  private static long monoCounter = 0L;
  private static final long MAX_COUNTER = 0xFFFL; // 12-bit max = 4095
  private static final Object MONO_LOCK = new Object();

  // -----------------------------------------------------------------------
  // BeforeExecutionGenerator contract
  // -----------------------------------------------------------------------

  @Override
  public Object generate(
      SharedSessionContractImplementor session,
      Object owner,
      Object currentValue,
      EventType eventType) {
    return generateUuidV7();
  }

  @Override
  public EnumSet<EventType> getEventTypes() { // FIX: was raw EnumSet
    return EnumSet.of(EventType.INSERT);
  }

  // -----------------------------------------------------------------------
  // Core generation logic
  // -----------------------------------------------------------------------

  /**
   * Generates a time-ordered, monotonic UUIDv7.
   *
   * <pre>
   * Bit layout (RFC 9562):
   *   Bits  0-47 : unix_ts_ms  (48 bits)
   *   Bits 48-51 : version = 0x7
   *   Bits 52-63 : rand_a = monotonic counter (12 bits)   ← NEW
   *   Bits 64-65 : variant = 0b10
   *   Bits 66-127: rand_b (62 random bits)
   * </pre>
   */
  public static UUID generateUuidV7() {
    final long tsMs;
    final long counter;

    synchronized (MONO_LOCK) {
      long now = Instant.now().toEpochMilli();

      if (now > lastTimestampMs) {
        lastTimestampMs = now;
        monoCounter = 0L;
      } else {
        monoCounter++;
        if (monoCounter > MAX_COUNTER) {
          // Counter overflow → bump virtual ts to preserve monotonicity
          lastTimestampMs++;
          monoCounter = 0L;
        }
      }

      tsMs = lastTimestampMs;
      counter = monoCounter;
    }

    byte[] rnd = RANDOM_BUFFER.get();
    RANDOM.nextBytes(rnd);

    // Most-significant 64 bits
    long msb =
        (tsMs << 16)
            | 0x7000L // version nibble
            | (counter & 0x0FFFL); // rand_a = monotonic counter

    // Least-significant 64 bits
    long lsb =
        ((long) (rnd[2] & 0x3F) | 0x80L) << 56 // variant bits
            | ((long) (rnd[3] & 0xFF)) << 48
            | ((long) (rnd[4] & 0xFF)) << 40
            | ((long) (rnd[5] & 0xFF)) << 32
            | ((long) (rnd[6] & 0xFF)) << 24
            | ((long) (rnd[7] & 0xFF)) << 16
            | ((long) (rnd[8] & 0xFF)) << 8
            | ((long) (rnd[9] & 0xFF));

    return new UUID(msb, lsb);
  }
}
