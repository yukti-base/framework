package org.yuktisetu.messaging.events;

/**
 * Topic names, in one place, because a typo in a producer's topic string is
 * silent -- the message goes nowhere and nothing errors.
 */
public final class DriveTopics {

    private DriveTopics() {}

    /** admin-service publishes on publish; drive-service resolves eligibility. */
    public static final String POSTING_PUBLISHED = "yuktisetu.drive.posting.published.v1";

    /** drive-service publishes when the deadline seals the pool; the future scoring engine consumes. */
    public static final String APPLICANT_POOL_SEALED = "yuktisetu.drive.applicant-pool.sealed.v1";

    /**
     * A command topic, not an event topic: "send this to these people".
     * notification-service consumes exactly this one, which is why it never has
     * to understand drive semantics -- and why the auth-service invite email is
     * just another dispatch message rather than a special case.
     */
    public static final String NOTIFICATION_DISPATCH = "yuktisetu.notification.dispatch.v1";

    public static String dlqFor(String topic) {
        return topic + ".dlq";
    }
}
