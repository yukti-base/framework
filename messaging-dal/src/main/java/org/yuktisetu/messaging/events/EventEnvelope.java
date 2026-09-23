package org.yuktisetu.messaging.events;

import java.time.Instant;

/**
 * The wrapper every Kafka message carries.
 *
 * Serialized as plain JSON with no Jackson type headers on purpose: the services
 * live in separate git repositories and must not depend on each other's class
 * names to deserialize a message. version is explicit so a payload change can be
 * rolled out without breaking a consumer mid-deploy.
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String version,
        Instant occurredAt,
        Long actorUserId,
        T payload
) {}
