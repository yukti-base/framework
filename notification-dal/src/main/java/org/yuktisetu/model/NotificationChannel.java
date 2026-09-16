package org.yuktisetu.model;

/**
 * IN_APP is the portal bell. EMAIL is the system of record. GOOGLE_GROUP is the
 * deliberate fail-safe -- the TnP's existing alias receives the mail like any
 * other recipient, so no Workspace domain-admin API access is ever needed.
 */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    GOOGLE_GROUP
}
