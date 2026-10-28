package org.yuktisetu.drive.model;

/**
 * The five composite-scoring factors.
 *
 * Five, not the SSOT's six: Profile Completeness was removed at the TnP's
 * instruction. The Section 5.9 non-negotiable-field gate still exists, but as a
 * hard eligibility gate rather than as something scored -- a student either
 * cleared it or is not in the pool, so scoring it again would double-count.
 *
 * There are no default weights, by deliberate decision. The TnP enters them per
 * drive at publish time, because weighting is a property of the company: a
 * product company wants coding and relevant experience weighted heavily, a
 * service company wants a balanced spread. Any code that hardcodes a weight here
 * has misunderstood the design.
 */
public enum ScoringFactor {

    /** Aggregate CGPA, percentiled over the applicant pool. */
    ACADEMIC_PERFORMANCE,

    /**
     * Built from the snapshots captured at apply time. Each platform's figures
     * reduce to one strength (CodingActivityPolicy), percentiled within that
     * platform's cohort, and the student is judged on their STRONGEST platform by
     * percentile, with cohort removal -- see CodingProficiency. Never an average.
     */
    CODING_PROFICIENCY,

    /** Overlap between the JD's required skills and the student's. */
    SKILL_MATCH,

    /** Months of relevant experience, weighted by domain match. */
    EXPERIENCE_RELEVANCE,

    /** Verified certifications only, weighted by issuer tier. */
    CERTIFICATIONS;

    /**
     * Whether the engine can measure this factor yet. Only academics and coding
     * in v1 (decided 2026-10-20); the rest are shown in the Drive Builder and
     * locked at 0 until their rules exist. A weight above zero on one of them
     * would hold every drive for TnP review over data nobody collects.
     */
    public boolean isScorable() {
        return this == ACADEMIC_PERFORMANCE || this == CODING_PROFICIENCY;
    }
}
