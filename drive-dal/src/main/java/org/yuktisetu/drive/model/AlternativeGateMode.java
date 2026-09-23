package org.yuktisetu.drive.model;

/**
 * How the 12th-percentage and diploma-percentage gates combine when both are
 * enabled.
 *
 * Only EITHER_SUFFICIENT exists, and that is the point: diploma-lateral students
 * have no 12th marks at all, so requiring both would silently exclude that entire
 * cohort from any drive setting a 12th cut-off. Kept as an enum rather than
 * collapsed away so a stricter combination can be added later without reshaping
 * the criteria table.
 */
public enum AlternativeGateMode {
    EITHER_SUFFICIENT
}
