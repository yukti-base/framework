package org.yuktisetu.profile.model;

/**
 * How much we actually know about a certificate a student claims.
 *
 * This exists because Certifications is one of the five scored factors, and a
 * certificate nobody checked must not score the same as one an issuer confirmed
 * -- otherwise verifying was pointless. The scoring engine reads this field, and
 * only the three CONFIRMED-equivalent states contribute.
 *
 * Deliberately records WHICH tier confirmed it rather than a single boolean: a
 * student disputing their score is owed the specific evidence, and "verified" with
 * no provenance is not an answer the TnP can give.
 */
public enum CertificationVerificationStatus {

    /** Not looked at yet. The state every row starts in. */
    PENDING,

    /**
     * Tier 1 -- the issuer itself confirmed it: an Open Badges 3.0 signature
     * verified, or a Credly public verification endpoint. The strongest evidence
     * available and the only genuinely cryptographic one.
     */
    VERIFIED_ISSUER,

    /**
     * Tier 2 -- the credential URL was fetched and the issuer's own page shows the
     * claimed name and course. Weaker than a signature, since it proves a page
     * exists rather than that it is cryptographically sound, but it is the issuer
     * speaking.
     */
    VERIFIED_URL,

    /**
     * Tier 3, awaiting a human. The LLM traced the certificate through the web and
     * proposed what it found, with its sources.
     *
     * Scores NOTHING in this state, and that is the entire safety property of the
     * tier: an LLM cannot verify a certificate, it can only find corroborating
     * evidence, and "something on the web looked like it" is not defensible against
     * a student who disputes their rank. It follows the JD parser's settled
     * contract -- propose into human review, never write authoritative data.
     */
    LLM_PROPOSED,

    /** Tier 3, confirmed by a TnP member who looked at the LLM's evidence. Scores fully. */
    TNP_CONFIRMED,

    /** Tier 3, rejected by a TnP member. Kept for the audit trail; scores nothing. */
    TNP_REJECTED,

    /**
     * Tier 4 -- nothing resolved it. Stored and shown to the TnP so the claim is
     * not lost, but it contributes nothing to a score.
     */
    UNRESOLVED;

    /** The only states the scoring engine counts. */
    public boolean countsTowardScore() {
        return this == VERIFIED_ISSUER || this == VERIFIED_URL || this == TNP_CONFIRMED;
    }

    /** Needs a human before it can count either way. */
    public boolean awaitsTnpReview() {
        return this == LLM_PROPOSED;
    }
}
