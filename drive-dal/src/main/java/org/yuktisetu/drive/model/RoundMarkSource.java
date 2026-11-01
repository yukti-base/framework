package org.yuktisetu.drive.model;

/**
 * Where a round mark came from. SHEET and RFID are reserved for the Google Sheet
 * fallback and the drive-day scanners; both write through the same marking path,
 * so a mark is a mark whatever produced it.
 */
public enum RoundMarkSource {
    /** Marked one by one, or in bulk, on the round screen. */
    PORTAL,
    /** From an uploaded Excel sheet or pasted list of emails. */
    EMAIL_LIST,
    /** Unmarked when the round was published, so counted as not advanced. */
    PUBLISH_DEFAULT,
    SHEET,
    RFID
}
