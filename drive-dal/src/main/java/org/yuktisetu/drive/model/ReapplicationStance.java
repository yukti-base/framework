package org.yuktisetu.drive.model;

/**
 * There is no one-offer policy. A placed student may sit again, and the stance
 * plus its uplift threshold is what the TnP configures per drive.
 */
public enum ReapplicationStance {
    BLOCKED,
    ALLOWED,
    ALLOWED_WITH_UPLIFT
}
