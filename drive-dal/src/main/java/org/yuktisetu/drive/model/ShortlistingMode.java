package org.yuktisetu.drive.model;

/**
 * How the shortlist gets decided once applications close.
 *
 * Every mode applies the posting's hard eligibility gates identically -- CGPA
 * floors, backlog caps, batch and branch targeting, the SSOT 5.9 profile gate.
 * They differ only in what happens to the survivors, which is why the mode is
 * recorded on the posting: it decides what a student can be told about their own
 * outcome afterwards.
 *
 * Nothing in this platform ever contacts a company. The TnP Cell is the only
 * party that talks to a recruiter, and it does so outside the application. So a
 * mode never means "the portal sends the list somewhere" -- it means "this is who
 * owns the decision, and this is therefore what we can answer for."
 *
 * A student never sees the shortlist. They see their OWN outcome and nothing about
 * anyone else, in every mode.
 */
public enum ShortlistingMode {

    /**
     * The scoring engine ranks the sealed pool at the deadline, and the student
     * sees their own factor breakdown and the weights that were applied.
     */
    COMPOSITE_SCORE,

    /**
     * No score is computed and the engine is skipped entirely. The hard-gate
     * survivors go straight to TnP review, and the TnP forwards the list to the
     * company themselves, outside this platform. The company's reasoning never
     * enters the system, so the student can be told they cleared the gates and
     * nothing more.
     */
    COMPANY_SIDE,

    /**
     * No score is computed. The hard-gate survivors stay with the TnP, who picks
     * the shortlist inside the portal. Every pick is a recorded human decision, so
     * unlike COMPANY_SIDE there IS an answerable reason when a student asks why
     * they were not called.
     */
    MANUAL,

    /**
     * First come, first served. There is no shortlisting step at all.
     *
     * Eligibility is still checked exactly as in every other mode, but the seats
     * ARE the allocation: applications are capped at maxApplications when the
     * student applies, under an exclusive lock on the posting, and whoever fills
     * the seats is in. For a 40-seat mock interview or a pre-placement workshop,
     * not for a recruitment drive.
     *
     * This is the one mode that changes the apply path's behaviour, so picking it
     * must be an informed choice -- see notice().
     */
    FCFS;

    /**
     * What the TnP must be shown before committing to this mode, in the drive
     * builder. Lives here rather than in the portal so the one authoritative
     * wording is the one the API serves, and a second consumer cannot drift.
     *
     * Empty for the modes whose behaviour holds no surprise.
     */
    public String notice() {
        return switch (this) {
            case FCFS -> "First come, first served: there will be NO shortlisting of any kind. "
                    + "Eligibility is still checked, and nothing else. The seat count is a hard "
                    + "limit applied the moment a student applies, so the students who apply "
                    + "first take the seats and later applicants are refused -- regardless of "
                    + "CGPA, coding profile or any other merit. Use this only for drives where "
                    + "the seats themselves are the allocation.";
            case COMPANY_SIDE -> "The scoring engine is skipped. Everyone clearing the eligibility "
                    + "gates goes to your review, and you forward the list to the company "
                    + "yourself. Because the company's reasoning never reaches this platform, a "
                    + "student who is not called can only be told that they cleared the gates.";
            case MANUAL -> "The scoring engine is skipped. Everyone clearing the eligibility gates "
                    + "comes to you and you pick the shortlist here. Every pick is recorded "
                    + "against your name, so a student who asks why they were not called has an "
                    + "answer.";
            case COMPOSITE_SCORE -> "";
        };
    }

    /** True when the shortlisting engine should not run for this posting at all. */
    public boolean skipsShortlistingEngine() {
        return this != COMPOSITE_SCORE;
    }

    /** True when maxApplications is a hard cap enforced at apply time. */
    public boolean capsApplicationsAtApplyTime() {
        return this == FCFS;
    }
}
