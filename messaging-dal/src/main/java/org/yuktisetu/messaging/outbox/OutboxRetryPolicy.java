package org.yuktisetu.messaging.outbox;

import java.time.Duration;

/**
 * How the outbox relays retry (decided 2026-11-01: Kafka being down must never
 * lose an event or halt a drive).
 *
 * A failure that can clear by itself -- broker down, timeout, leader election --
 * is retried for as long as it takes, backing off 2, 4, 8 ... seconds up to five
 * minutes between tries. The row stays PENDING the whole time, so it survives
 * restarts and is published the moment Kafka is back. Only a failure Kafka will
 * never accept (a record too large, a payload that cannot be serialised, an
 * invalid topic name) is marked FAILED, prefixed NON_RETRYABLE so startup
 * recovery leaves it alone.
 *
 * Pure, so both relays (admin-service and drive-service) share one rule.
 */
public final class OutboxRetryPolicy {

    private OutboxRetryPolicy() {}

    public static final String NON_RETRYABLE_PREFIX = "NON_RETRYABLE: ";

    /** Longest wait between tries. */
    public static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    /** Log at ERROR on these attempt counts so a long outage is visible without flooding. */
    public static boolean worthAnError(int attempts) {
        return attempts == 5 || attempts % 20 == 0;
    }

    public static Duration backoff(int attempts) {
        long seconds = attempts >= 9 ? MAX_BACKOFF.toSeconds() : Math.min(1L << attempts, MAX_BACKOFF.toSeconds());
        return Duration.ofSeconds(seconds);
    }

    /**
     * Matched by class name so this module needs no Kafka dependency. Walks the
     * cause chain: the send future wraps the real error in ExecutionException.
     */
    public static boolean isRetryable(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName();
            if (name.equals("RecordTooLargeException") || name.equals("SerializationException")
                    || name.equals("InvalidTopicException") || name.equals("RecordBatchTooLargeException")) {
                return false;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return true;
    }
}
