package org.yuktisetu.messaging.events;

/**
 * Template keys, in one place, for the same reason DriveTopics exists: a typo in
 * a producer's key string is not a compile error and not a runtime exception at
 * the producer -- the dispatch is accepted, notification-service fails to find
 * the template, and the whole blast lands in notification_deliveries as FAILED
 * rows nobody is watching.
 *
 * Every key here MUST have a matching row in notification_templates for each
 * channel the producer asks for. TemplateSeeder in notification-service ships
 * one of each; the TnP can reword them afterwards.
 */
public final class NotificationTemplates {

    private NotificationTemplates() {}

    // --- Student-facing ---------------------------------------------------

    /** The one existing key, kept here so producers stop hard-coding it. */
    public static final String ROLE_INVITE = "invite-role-email";

    /** Account activated after the invite was accepted. Security confirmation. */
    public static final String ACCOUNT_ACTIVATED = "account-activated";

    /** A drive the student is eligible for has opened. The fan-out blast. */
    public static final String DRIVE_PUBLISHED = "drive-published";

    /** A live drive was edited and re-published; everyone currently eligible is told. */
    public static final String DRIVE_UPDATED = "drive-updated";

    /** Receipt for an Easy Apply. */
    public static final String APPLICATION_SUBMITTED = "application-submitted";

    /** Receipt for a withdrawal, so the student has a record they made it. */
    public static final String APPLICATION_WITHDRAWN = "application-withdrawn";

    /** The TnP removed them from the pool, and they are owed the reason. */
    public static final String APPLICATION_DISQUALIFIED = "application-disqualified";

    /**
     * Their coding handle could not be read at apply time. While the deadline is
     * open they can check it and re-check once, instead of going to the TnP.
     */
    public static final String CODING_HANDLE_CHECK_NEEDED = "coding-handle-check-needed";

    /** The re-check failed too; they are scored on their other platforms. */
    public static final String CODING_HANDLE_STILL_FAILING = "coding-handle-still-failing";

    /**
     * Their application goes to TnP review over coding data, and why: a server
     * error, no re-check by the deadline, or nothing left to score on.
     */
    public static final String CODING_CAPTURE_FLAGGED = "coding-capture-flagged";

    /**
     * The TnP published a drive's shortlist. One template for both outcomes; the
     * shortlisted flag in the variables decides the wording. Whether a score is
     * mentioned depends on the platform's score-visibility switch.
     */
    public static final String SHORTLIST_RESULT = "shortlist-result";

    /** A sealed drive's deadline was extended: applications are open again. */
    public static final String DRIVE_REOPENED = "drive-reopened";

    /**
     * A round's results were published. One template for every outcome --
     * advanced, not advanced, absent -- worded by the builder, with the next
     * round's details when the student goes on.
     */
    public static final String ROUND_RESULT = "round-result";

    /** The current round's date, venue or instructions were set or changed. */
    public static final String ROUND_SCHEDULED = "round-scheduled";

    /** A coordinator corrected a published round result, with their reason. */
    public static final String ROUND_RESULT_CORRECTED = "round-result-corrected";

    /**
     * The student cleared the final round. No offer letter -- the company sends
     * that outside the app -- just the congratulations and what was recorded.
     */
    public static final String DRIVE_OFFERED = "drive-offered";

    /** A drive the student applied to was cancelled by the TnP, with the reason given. */
    public static final String DRIVE_CANCELLED = "drive-cancelled";

    // --- Staff-facing -----------------------------------------------------
    //
    // Deliberately few. A notification per application would make a 400-student
    // drive unusable for the coordinator who has to read them, so staff hear
    // about outcomes and failures only -- never about individual student acts.

    /** An eligibility resolution finished: how many became eligible. */
    public static final String ELIGIBILITY_RUN_COMPLETE = "eligibility-run-complete";

    /** An eligibility resolution FAILED, which means the drive is silently dead. */
    public static final String ELIGIBILITY_RUN_FAILED = "eligibility-run-failed";

    /** The deadline passed and the applicant pool is sealed -- the handover moment. */
    public static final String APPLICANT_POOL_SEALED = "applicant-pool-sealed";

    /**
     * Some applications' coding data could not be captured cleanly when the
     * students applied, so shortlisting is HELD pending TnP review of them.
     *
     * The one notification in the system that gates a drive on a human. Until the
     * TnP decides, the shortlisting engine does not run -- so this is EMAIL as well
     * as in-app, because a drive waiting silently for an approval nobody saw is
     * indistinguishable from a drive that broke.
     */
    public static final String CODING_DATA_REVIEW_REQUIRED = "coding-data-review-required";

    /**
     * The drive was ended: each department's faculty coordinators get their own
     * department's students and how far each got. Per-recipient rows, one
     * batched dispatch.
     */
    public static final String DRIVE_DEPARTMENT_REPORT = "drive-department-report";
}
