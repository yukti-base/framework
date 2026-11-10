package org.yuktisetu.messaging.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxRetryPolicyTest {

    static class RecordTooLargeException extends RuntimeException {}

    @Test
    @DisplayName("backoff doubles from 2 s and stops at five minutes, never giving up")
    void backoffIsCapped() {
        assertEquals(Duration.ofSeconds(2), OutboxRetryPolicy.backoff(1));
        assertEquals(Duration.ofSeconds(256), OutboxRetryPolicy.backoff(8));
        assertEquals(Duration.ofMinutes(5), OutboxRetryPolicy.backoff(9));
        assertEquals(Duration.ofMinutes(5), OutboxRetryPolicy.backoff(500));
    }

    @Test
    @DisplayName("a broker outage or timeout is retryable, even wrapped by the send future")
    void outagesAreRetryable() {
        assertTrue(OutboxRetryPolicy.isRetryable(new ExecutionException(new TimeoutException("broker down"))));
        assertTrue(OutboxRetryPolicy.isRetryable(new IllegalStateException("anything else")));
    }

    @Test
    @DisplayName("a record Kafka will never accept is not, however deeply wrapped")
    void permanentRejectionsAreNot() {
        assertFalse(OutboxRetryPolicy.isRetryable(new ExecutionException(new RecordTooLargeException())));
    }
}
