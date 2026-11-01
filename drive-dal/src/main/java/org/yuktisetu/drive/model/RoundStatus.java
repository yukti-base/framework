package org.yuktisetu.drive.model;

/**
 * PLANNED rounds are editable and, if they are the drive's current round,
 * markable. A PUBLISHED round has told every candidate their result; from then
 * on only a coordinator's correction, with a reason, changes it.
 */
public enum RoundStatus {
    PLANNED,
    PUBLISHED
}
