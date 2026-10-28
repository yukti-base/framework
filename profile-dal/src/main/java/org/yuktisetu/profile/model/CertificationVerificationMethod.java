package org.yuktisetu.profile.model;

/**
 * Which adapter produced a certificate's verification status.
 *
 * Separate from the status because the same status can be reached different ways
 * and the difference matters when an adapter turns out to be wrong: if the Credly
 * endpoint starts returning stale data, this column is what lets the affected rows
 * be found and re-verified without touching the rest.
 */
public enum CertificationVerificationMethod {

    /** 1EdTech Open Badges 3.0 -- signature verified. Self-hosted validator. */
    OPEN_BADGES,

    /** Credly's unauthenticated public verification endpoint. */
    CREDLY,

    /** NPTEL's certificate-id / QR verification page. */
    NPTEL,

    /** Generic credential-URL fetch: the issuer's page confirms name and course. */
    CREDENTIAL_URL,

    /** The LLM web trace. Always lands on LLM_PROPOSED, never on a verified state. */
    LLM_TRACE,

    /** A TnP member decided, having looked at whatever evidence existed. */
    TNP_MANUAL,

    /** Nothing ran, or nothing applied. */
    NONE
}
