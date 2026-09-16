package org.yuktisetu.events;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipients carry their own email and display name.
 *
 * That denormalisation is deliberate: notification-service has no user-dal
 * dependency, so it can keep delivering mail while the academic model changes
 * underneath it, and it never joins to a student row to find an address.
 *
 * Producers chunk large audiences (~500 recipients per message) rather than
 * sending one message per drive -- a 2000-student blast is four messages, not
 * one oversized one and not two thousand tiny ones.
 */
public record NotificationDispatchEvent(
        String notificationType,
        String severity,
        String templateKey,
        String subject,
        String actionUrl,
        Map<String, Object> commonVariables,
        List<Recipient> recipients,
        List<String> broadcastGroupAliases,
        Set<String> channels
) {
    public record Recipient(
            Long userId,
            String email,
            String displayName,
            Map<String, Object> variables
    ) {}
}
