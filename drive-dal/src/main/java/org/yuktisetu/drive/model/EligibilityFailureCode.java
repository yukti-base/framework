package org.yuktisetu.drive.model;


/**
 * Stable reasons a student failed a drive's eligibility gate.
 *
 * These are codes, not sentences, for three reasons: they aggregate into
 * EligibilityRun.failureSummary so the TnP can answer "why did only 40 of 300
 * qualify" from data; they survive rewording of the student-facing message; and
 * a student asking "why can't I see this drive" gets the same answer months
 * later that they would have got on the day.
 */
public final class EligibilityFailureCode {

    private EligibilityFailureCode() {}

    // Targeting
    public static final String NOT_TARGETED = "NOT_TARGETED";

    // Profile readiness (SSOT 5.9 -- gates every drive, not just ones checking the field)
    public static final String PROFILE_INCOMPLETE = "PROFILE_INCOMPLETE";

    // Academic gates
    public static final String CGPA_BELOW_MIN = "CGPA_BELOW_MIN";
    public static final String SEMESTER_GPA_BELOW_MIN = "SEMESTER_GPA_BELOW_MIN";
    public static final String TENTH_BELOW_MIN = "TENTH_BELOW_MIN";
    public static final String TWELFTH_BELOW_MIN = "TWELFTH_BELOW_MIN";
    public static final String DIPLOMA_BELOW_MIN = "DIPLOMA_BELOW_MIN";
    public static final String TWELFTH_OR_DIPLOMA_BELOW_MIN = "TWELFTH_OR_DIPLOMA_BELOW_MIN";
    public static final String ACTIVE_BACKLOGS_EXCEEDED = "ACTIVE_BACKLOGS_EXCEEDED";
    public static final String TOTAL_BACKLOGS_EXCEEDED = "TOTAL_BACKLOGS_EXCEEDED";
    public static final String GAP_YEARS_EXCEEDED = "GAP_YEARS_EXCEEDED";
    public static final String COCUBES_BELOW_MIN = "COCUBES_BELOW_MIN";

    // Re-application policy
    public static final String LIVE_INTERNSHIP_HELD = "LIVE_INTERNSHIP_HELD";
    public static final String ALREADY_PLACED = "ALREADY_PLACED";
    public static final String PLACED_UPLIFT_NOT_MET = "PLACED_UPLIFT_NOT_MET";
    public static final String INTERN_UPLIFT_NOT_MET = "INTERN_UPLIFT_NOT_MET";
    public static final String PENDING_OFFER_HELD = "PENDING_OFFER_HELD";
    public static final String MAX_LIVE_OFFERS_REACHED = "MAX_LIVE_OFFERS_REACHED";
    public static final String COOLING_OFF_ACTIVE = "COOLING_OFF_ACTIVE";
    public static final String ADMIN_DENIED = "ADMIN_DENIED";
    public static final String ADMIN_DECISION_PENDING = "ADMIN_DECISION_PENDING";
    public static final String DEBARRED = "DEBARRED";

    /** MISSING_DATA:cgpa -- an enabled gate whose student-side value is null. */
    public static String missingData(String field) {
        return "MISSING_DATA:" + field;
    }

    /** CUSTOM:aws_certified -- a company one-off the student failed. */
    public static String custom(String criterionKey) {
        return "CUSTOM:" + criterionKey;
    }

    /**
     * UNEVALUATED:AMCAT -- a criterion nobody can currently check, because the
     * value has nowhere to be read from. Recorded rather than silently passed:
     * a gate that quietly waves everyone through is worse than no gate, because
     * the TnP believes it is filtering.
     */
    public static String unevaluated(String what) {
        return "UNEVALUATED:" + what;
    }
}
