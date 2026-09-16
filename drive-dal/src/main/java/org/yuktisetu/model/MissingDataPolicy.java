package org.yuktisetu.model;

/**
 * What an enabled gate does when the student's value is null. Made explicit
 * per drive because the alternative is an accident: silently passing means an
 * incomplete profile clears every gate, silently failing means one un-synced
 * ERP column quietly removes a student from every drive.
 */
public enum MissingDataPolicy {
    INELIGIBLE,
    ELIGIBLE,
    NEEDS_REVIEW
}
