package org.yuktisetu.drive.model;

/**
 * TNP_ADDED covers the TnP adding a candidate after the fact -- which by
 * definition means already shortlisted -- and is what keeps that path auditable
 * instead of indistinguishable from a student's own application.
 */
public enum ApplicationSource {
    EASY_APPLY,
    TNP_ADDED,
    IMPORT
}
