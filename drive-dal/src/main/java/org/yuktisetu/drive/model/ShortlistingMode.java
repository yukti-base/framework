package org.yuktisetu.drive.model;

/**
 * How the shortlist gets decided once applications close.
 *
 * All three modes apply the posting's hard eligibility gates identically -- CGPA
 * floors, backlog caps, batch and branch targeting, the SSOT 5.9 profile gate.
 * They differ only in what happens to the survivors, which is why the mode is
 * recorded on the posting: it decides what a student can be told about their own
 * outcome afterwards.
 *
 * COMPOSITE_SCORE  the scoring engine ranks the sealed pool at deadline and the
 *                  student sees their factor breakdown and the weights used.
 * COMPANY_SIDE     no score is computed; the hard-gate survivors go to the
 *                  company as-is and HR decides. The student can be told they
 *                  cleared the gates, and nothing more -- we do not hold the
 *                  company's reasoning.
 * MANUAL           no score is computed; the hard-gate survivors stay with the
 *                  TnP, who picks the shortlist in the portal. Every pick is a
 *                  recorded human decision, so unlike COMPANY_SIDE there IS an
 *                  answerable reason when a student asks why they were not
 *                  called.
 *
 * The COMPANY_SIDE/MANUAL distinction is about who decides, not about how the
 * pool was filtered. Collapsing them would lose the only thing that makes a
 * student's "why not me?" answerable in one case and not the other.
 */
public enum ShortlistingMode {
    COMPOSITE_SCORE,
    COMPANY_SIDE,
    MANUAL
}
