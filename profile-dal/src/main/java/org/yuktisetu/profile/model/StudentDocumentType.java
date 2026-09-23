package org.yuktisetu.profile.model;

/**
 * What a student document is, which decides where it may travel.
 *
 * RESUME and COVER_LETTER are the employment-facing ones: they are attached to
 * applications and exported to companies. The identity and academic proofs exist
 * for verification inside the TnP Cell and are deliberately NOT included in the
 * applicant export.
 */
public enum StudentDocumentType {
    RESUME,
    COVER_LETTER,
    AADHAAR,
    PAN,
    MARKSHEET_10,
    MARKSHEET_12,
    DEGREE_CERTIFICATE,
    CERTIFICATE,
    OTHER
}
