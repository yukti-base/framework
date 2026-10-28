package org.yuktisetu.messaging.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builders for every dispatch the fleet sends.
 *
 * They live here, beside the event record, rather than in each producing
 * service, because the three fields most easily got wrong -- notificationType,
 * templateKey and channels -- have to agree with each other and with the rows
 * TemplateSeeder installs. Assembling the record by hand at seven call sites is
 * how a drive blast ends up asking for an EMAIL template that was only ever
 * seeded for IN_APP, which fails at delivery time and not at the producer.
 *
 * The channel choice on each builder is the anti-spam policy, expressed once:
 *
 *   - A student gets EMAIL only for things they must act on or would dispute:
 *     a drive opening, a receipt for their own application, a disqualification,
 *     the activation of their account.
 *   - Staff get IN_APP for outcomes they will see when they next open the
 *     portal, and EMAIL only when the thing that happened means a drive is
 *     silently broken and nobody would otherwise find out.
 *
 * Changing a channel here changes it everywhere, which is the point.
 */
public final class Dispatches {

    private Dispatches() {}

    /**
     * Recipients per Kafka message. DispatchIngestionService hard-rejects
     * anything above 1000, and a 2000-student drive should be four messages --
     * not one oversized payload and not two thousand tiny ones.
     */
    public static final int RECIPIENT_CHUNK = 500;

    /**
     * Map.of rejects a null value with an NPE, and a dispatch that dies building
     * its variable map takes the business transaction's notification with it.
     * Every optional string goes through here.
     */
    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static final Set<String> EMAIL_AND_IN_APP = Set.of("EMAIL", "IN_APP");
    private static final Set<String> IN_APP_ONLY = Set.of("IN_APP");

    /**
     * Splits an audience into dispatch-sized batches.
     *
     * Returns views over the original list rather than copies: a 5000-student
     * cohort is ten sublists, not a second copy of five thousand records.
     */
    public static <T> List<List<T>> chunk(List<T> all, int size) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        List<List<T>> chunks = new ArrayList<>((all.size() / size) + 1);
        for (int start = 0; start < all.size(); start += size) {
            chunks.add(all.subList(start, Math.min(start + size, all.size())));
        }
        return chunks;
    }

    // ------------------------------------------------------------------
    // Student-facing
    // ------------------------------------------------------------------

    /**
     * The drive blast. Sent once per eligible student, when the eligibility
     * resolution has finished -- never at publish time, because at publish time
     * nobody is eligible yet and the audience would be empty.
     */
    public static NotificationDispatchEvent drivePublished(
            String companyName,
            String postingTitle,
            String deadlineText,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "JOB_POSTING_PUBLISHED",
                "INFO",
                NotificationTemplates.DRIVE_PUBLISHED,
                companyName + " is hiring: " + postingTitle,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "deadline", deadlineText,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /**
     * A live drive was edited and re-published. Sent to everyone who is eligible
     * under the NEW criteria, with the current deadline, because the deadline is
     * the detail most likely to have moved.
     */
    public static NotificationDispatchEvent driveUpdated(
            String companyName,
            String postingTitle,
            String deadlineText,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "JOB_POSTING_UPDATED",
                "INFO",
                NotificationTemplates.DRIVE_UPDATED,
                companyName + " drive updated: " + postingTitle,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "deadline", deadlineText,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** Who a reopen notice is for; each gets its own wording. */
    public enum ReopenAudience {
        /** Was shortlisted before the reopen: now under re-review. */
        PREVIOUSLY_SHORTLISTED,
        /** Applied (and was in the pool): the application stands. */
        APPLICANT,
        /** Eligible and has not applied: a new chance to. */
        ELIGIBLE
    }

    /**
     * The TnP extended the deadline of a sealed drive. The wording per audience is
     * decided here, because the renderer runs in TEXT mode with no conditionals.
     */
    public static NotificationDispatchEvent driveReopened(
            String companyName,
            String postingTitle,
            String deadlineText,
            ReopenAudience audience,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        String note = switch (audience) {
            case PREVIOUSLY_SHORTLISTED -> "You were shortlisted before. Because more students can now apply, "
                    + "the shortlist will be worked out again after the new deadline, and your application is "
                    + "under re-review until then. You do not need to apply again.";
            case APPLICANT -> "Your application still stands; you do not need to apply again. The shortlist "
                    + "will be worked out after the new deadline.";
            case ELIGIBLE -> "You are eligible and have not applied yet. You can apply until the new deadline.";
        };
        return new NotificationDispatchEvent(
                "JOB_POSTING_UPDATED",
                "INFO",
                NotificationTemplates.DRIVE_REOPENED,
                "Applications reopened: " + postingTitle + " at " + companyName,
                actionUrl,
                Map.of("companyName", nullSafe(companyName),
                        "postingTitle", nullSafe(postingTitle),
                        "deadline", nullSafe(deadlineText),
                        "note", note,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** Receipt for an Easy Apply. One student, but still list-shaped. */
    public static NotificationDispatchEvent applicationSubmitted(
            String companyName,
            String postingTitle,
            String deadlineText,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "APPLICATION_SUBMITTED",
                "SUCCESS",
                NotificationTemplates.APPLICATION_SUBMITTED,
                "Application received: " + postingTitle + " at " + companyName,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "deadline", deadlineText,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /**
     * Receipt for a withdrawal.
     *
     * Mailed rather than left in-app only because this is the notification a
     * student disputes ("I never withdrew from that drive") once the pool has
     * closed and it is too late to re-apply.
     */
    public static NotificationDispatchEvent applicationWithdrawn(
            String companyName,
            String postingTitle,
            String deadlineText,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "APPLICATION_WITHDRAWN",
                "WARNING",
                NotificationTemplates.APPLICATION_WITHDRAWN,
                "You withdrew from " + postingTitle + " at " + companyName,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "deadline", deadlineText,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** The TnP removed them from the pool. The reason travels with it. */
    public static NotificationDispatchEvent applicationDisqualified(
            String companyName,
            String postingTitle,
            String reason,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "APPLICATION_DISQUALIFIED",
                "CRITICAL",
                NotificationTemplates.APPLICATION_DISQUALIFIED,
                "Update on your application to " + companyName,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "actionUrl", nullSafe(actionUrl),
                        "reason", reason == null || reason.isBlank()
                                ? "No reason was recorded. Please contact the TnP Cell."
                                : reason),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /**
     * A coding handle could not be read when they applied. EMAIL: it asks them to
     * act before a deadline, and it is the record that they were asked.
     *
     * drives is the human list of affected drives, problem the plain-words cause.
     */
    public static NotificationDispatchEvent codingHandleCheckNeeded(
            String platformName,
            String handle,
            String problem,
            String drives,
            String deadlineText,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "WARNING",
                NotificationTemplates.CODING_HANDLE_CHECK_NEEDED,
                "Action needed: check your " + platformName + " username",
                actionUrl,
                Map.of("platformName", platformName,
                        "handle", nullSafe(handle),
                        "problem", nullSafe(problem),
                        "drives", nullSafe(drives),
                        "deadline", nullSafe(deadlineText),
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** The re-check failed on the handle too. EMAIL: this is the one they would dispute. */
    public static NotificationDispatchEvent codingHandleStillFailing(
            String platformName,
            String handle,
            String scoredOn,
            String drives,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "WARNING",
                NotificationTemplates.CODING_HANDLE_STILL_FAILING,
                platformName + " still could not be read",
                actionUrl,
                Map.of("platformName", platformName,
                        "handle", nullSafe(handle),
                        "scoredOn", nullSafe(scoredOn),
                        "drives", nullSafe(drives),
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /**
     * The shortlist for a drive is out. shortlisted picks the wording;
     * scoresVisible says whether to point the student at their score breakdown,
     * which they can only see while the platform switch is on.
     */
    public static NotificationDispatchEvent shortlistResult(
            String companyName,
            String postingTitle,
            boolean shortlisted,
            boolean scoresVisible,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        // The renderer runs in TEXT mode, so the wording is decided here rather
        // than with template conditionals.
        String outcome = shortlisted
                ? "You have been shortlisted."
                : "You have not been shortlisted this time.";
        String scoreNote = scoresVisible
                ? "Your standing among this drive's applicants, and how it was worked out, is on the drive's page."
                : "";
        return new NotificationDispatchEvent(
                "GENERIC",
                shortlisted ? "SUCCESS" : "INFO",
                NotificationTemplates.SHORTLIST_RESULT,
                (shortlisted ? "Shortlisted: " : "Shortlist result: ") + postingTitle + " at " + companyName,
                actionUrl,
                Map.of("companyName", nullSafe(companyName),
                        "postingTitle", nullSafe(postingTitle),
                        "outcome", outcome,
                        "scoreNote", scoreNote,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** Their application is with the TnP over coding data. reason is the plain-words why. */
    public static NotificationDispatchEvent codingCaptureFlagged(
            String platformName,
            String reason,
            String drives,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "INFO",
                NotificationTemplates.CODING_CAPTURE_FLAGGED,
                "Your application goes to TnP review",
                actionUrl,
                Map.of("platformName", platformName,
                        "reason", nullSafe(reason),
                        "drives", nullSafe(drives),
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** Sent once, when an invitee sets their password and the account goes ACTIVE. */
    public static NotificationDispatchEvent accountActivated(
            String portalUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "SUCCESS",
                NotificationTemplates.ACCOUNT_ACTIVATED,
                "Your YuktiSetu account is active",
                portalUrl,
                Map.of("portalUrl", nullSafe(portalUrl),
                        "actionUrl", nullSafe(portalUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    // ------------------------------------------------------------------
    // Staff-facing
    // ------------------------------------------------------------------

    /**
     * IN_APP only, on purpose. A coordinator who publishes six drives in an
     * afternoon should find six lines in the bell, not six emails.
     */
    public static NotificationDispatchEvent eligibilityRunComplete(
            String companyName,
            String postingTitle,
            int evaluated,
            int eligible,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "INFO",
                NotificationTemplates.ELIGIBILITY_RUN_COMPLETE,
                postingTitle + ": " + eligible + " students eligible",
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "evaluated", evaluated,
                        "eligible", eligible,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                IN_APP_ONLY);
    }

    /**
     * The one staff notification that earns an email.
     *
     * A failed resolution is invisible by construction: the drive shows as
     * PUBLISHED, no student becomes eligible, no student is notified, and the
     * screen just shows an empty pool. Without this the first person to notice
     * is a student asking why they cannot see a drive everyone mentioned.
     */
    /**
     * Some applications' coding data could not be captured cleanly at apply time,
     * so shortlisting is HELD until the TnP rules on them. Sent once per drive, by
     * the scoring gate, when every capture in the sealed pool has settled.
     *
     * platformName is the list of platforms involved ("CodeChef, LeetCode") and
     * reason the per-platform breakdown of what went wrong. oldestSnapshotAge is
     * optional and unused by apply-time capture, where nothing is ever re-read.
     *
     * notificationType stays GENERIC, following eligibilityRunFailed: a dedicated
     * NotificationType value would mean widening a Postgres CHECK constraint in
     * every database for no behaviour notification-service actually keys on.
     */
    public static NotificationDispatchEvent codingDataReviewRequired(
            String companyName,
            String postingTitle,
            String platformName,
            int affectedStudents,
            String oldestSnapshotAge,
            String reason,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "CRITICAL",
                NotificationTemplates.CODING_DATA_REVIEW_REQUIRED,
                "Shortlisting held: coding data to review for " + postingTitle,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "platformName", platformName,
                        "affectedStudents", String.valueOf(affectedStudents),
                        "oldestSnapshotAge", nullSafe(oldestSnapshotAge),
                        "reason", reason == null || reason.isBlank()
                                ? "The platform did not respond." : reason,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    public static NotificationDispatchEvent eligibilityRunFailed(
            String companyName,
            String postingTitle,
            String reason,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "GENERIC",
                "CRITICAL",
                NotificationTemplates.ELIGIBILITY_RUN_FAILED,
                "Action needed: eligibility failed for " + postingTitle,
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "actionUrl", nullSafe(actionUrl),
                        "reason", reason == null || reason.isBlank() ? "Unknown error" : reason),
                recipients,
                List.of(),
                EMAIL_AND_IN_APP);
    }

    /** The deadline passed. IN_APP: the coordinator acts on this inside the portal. */
    public static NotificationDispatchEvent applicantPoolSealed(
            String companyName,
            String postingTitle,
            int applicantCount,
            String actionUrl,
            List<NotificationDispatchEvent.Recipient> recipients) {

        return new NotificationDispatchEvent(
                "APPLICATIONS_CLOSED",
                "INFO",
                NotificationTemplates.APPLICANT_POOL_SEALED,
                postingTitle + " closed with " + applicantCount + " applicants",
                actionUrl,
                Map.of("companyName", companyName,
                        "postingTitle", postingTitle,
                        "applicantCount", applicantCount,
                        "actionUrl", nullSafe(actionUrl)),
                recipients,
                List.of(),
                IN_APP_ONLY);
    }
}
