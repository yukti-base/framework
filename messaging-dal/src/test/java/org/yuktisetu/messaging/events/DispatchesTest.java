package org.yuktisetu.messaging.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the notification policy.
 *
 * These are not tests of plumbing. Each one states a product decision that was
 * argued out once and should not be quietly reversed by a later edit:
 *
 *  - which notifications are worth an email and which stay in the bell
 *  - that a staff member is never emailed about an ordinary outcome
 *  - that a cohort-sized audience is split before it reaches Kafka
 *  - that a null optional string cannot blow up a dispatch
 *
 * The last one matters more than it reads: Map.of throws NullPointerException on
 * a null value, and these builders are called from inside business transactions
 * that must not be rolled back by a notification.
 */
class DispatchesTest {

    private static List<NotificationDispatchEvent.Recipient> recipients(int count) {
        List<NotificationDispatchEvent.Recipient> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            out.add(new NotificationDispatchEvent.Recipient(
                    (long) i, "student" + i + "@pccoe.org", "Student " + i, Map.of()));
        }
        return out;
    }

    @Nested
    @DisplayName("Chunking")
    class Chunking {

        @Test
        @DisplayName("splits an audience at the dispatch limit")
        void splitsAtLimit() {
            List<List<NotificationDispatchEvent.Recipient>> chunks =
                    Dispatches.chunk(recipients(1200), Dispatches.RECIPIENT_CHUNK);

            assertEquals(3, chunks.size());
            assertEquals(500, chunks.get(0).size());
            assertEquals(500, chunks.get(1).size());
            assertEquals(200, chunks.get(2).size());
        }

        @Test
        @DisplayName("every chunk stays under the ingestion service's hard cap of 1000")
        void neverExceedsIngestionCap() {
            for (List<NotificationDispatchEvent.Recipient> chunk :
                    Dispatches.chunk(recipients(5000), Dispatches.RECIPIENT_CHUNK)) {
                assertTrue(chunk.size() <= 1000,
                        "DispatchIngestionService rejects a dispatch above 1000 recipients outright");
            }
        }

        @Test
        @DisplayName("loses nobody")
        void losesNobody() {
            int total = 0;
            for (List<NotificationDispatchEvent.Recipient> chunk :
                    Dispatches.chunk(recipients(1201), Dispatches.RECIPIENT_CHUNK)) {
                total += chunk.size();
            }
            assertEquals(1201, total);
        }

        @Test
        @DisplayName("an exact multiple produces no trailing empty chunk")
        void exactMultiple() {
            assertEquals(2, Dispatches.chunk(recipients(1000), Dispatches.RECIPIENT_CHUNK).size());
        }

        @Test
        @DisplayName("an empty or null audience produces no messages at all")
        void emptyAudience() {
            assertTrue(Dispatches.chunk(List.of(), Dispatches.RECIPIENT_CHUNK).isEmpty());
            assertTrue(Dispatches.chunk(null, Dispatches.RECIPIENT_CHUNK).isEmpty());
        }
    }

    @Nested
    @DisplayName("Channel policy")
    class ChannelPolicy {

        @Test
        @DisplayName("students are emailed about a drive they are eligible for")
        void drivePublishedIsEmailed() {
            var event = Dispatches.drivePublished(
                    "Acme", "SDE Intern", "1 Oct 2026, 5:00 pm", "http://x/1", recipients(1));

            assertTrue(event.channels().contains("EMAIL"));
            assertTrue(event.channels().contains("IN_APP"));
            assertEquals("JOB_POSTING_PUBLISHED", event.notificationType());
            assertEquals(NotificationTemplates.DRIVE_PUBLISHED, event.templateKey());
        }

        @Test
        @DisplayName("apply and withdraw receipts are emailed, because both get disputed")
        void receiptsAreEmailed() {
            var applied = Dispatches.applicationSubmitted(
                    "Acme", "SDE Intern", "1 Oct 2026", "http://x/1", recipients(1));
            var withdrawn = Dispatches.applicationWithdrawn(
                    "Acme", "SDE Intern", "1 Oct 2026", "http://x/1", recipients(1));

            assertTrue(applied.channels().contains("EMAIL"));
            assertTrue(withdrawn.channels().contains("EMAIL"));
        }

        @Test
        @DisplayName("a disqualification is CRITICAL and emailed")
        void disqualificationIsCritical() {
            var event = Dispatches.applicationDisqualified(
                    "Acme", "SDE Intern", "Backlog on record", "http://x/1", recipients(1));

            assertEquals("CRITICAL", event.severity());
            assertTrue(event.channels().contains("EMAIL"));
        }

        @Test
        @DisplayName("an ordinary staff outcome NEVER reaches email")
        void staffOutcomesStayInApp() {
            var complete = Dispatches.eligibilityRunComplete(
                    "Acme", "SDE Intern", 1200, 412, "http://x/1", recipients(3));
            var sealed = Dispatches.applicantPoolSealed(
                    "Acme", "SDE Intern", 312, "http://x/1", recipients(3));

            assertFalse(complete.channels().contains("EMAIL"),
                    "A coordinator publishing six drives must not receive six emails");
            assertFalse(sealed.channels().contains("EMAIL"));
            assertTrue(complete.channels().contains("IN_APP"));
            assertTrue(sealed.channels().contains("IN_APP"));
        }

        @Test
        @DisplayName("a FAILED eligibility run is the one staff notice that is emailed")
        void failureEscalatesToEmail() {
            var event = Dispatches.eligibilityRunFailed(
                    "Acme", "SDE Intern", "NullPointerException", "http://x/1", recipients(2));

            assertTrue(event.channels().contains("EMAIL"),
                    "A failed run has no other visible symptom -- the drive just stays empty");
            assertEquals("CRITICAL", event.severity());
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("a missing action url does not throw")
        void nullActionUrl() {
            var event = Dispatches.drivePublished("Acme", "SDE", "soon", null, recipients(1));
            assertEquals("", event.commonVariables().get("actionUrl"));
        }

        @Test
        @DisplayName("a blank disqualification reason becomes something a student can act on")
        void blankReasonIsReplaced() {
            var event = Dispatches.applicationDisqualified("Acme", "SDE", "   ", "http://x/1", recipients(1));
            String reason = (String) event.commonVariables().get("reason");

            assertNotNull(reason);
            assertFalse(reason.isBlank(), "A student told only 'you were removed' has nothing to appeal against");
            assertTrue(reason.contains("TnP"));
        }

        @Test
        @DisplayName("a missing failure reason still names something")
        void nullFailureReason() {
            var event = Dispatches.eligibilityRunFailed("Acme", "SDE", null, "http://x/1", recipients(1));
            assertEquals("Unknown error", event.commonVariables().get("reason"));
        }

        @Test
        @DisplayName("a null portal url does not throw on activation")
        void nullPortalUrl() {
            var event = Dispatches.accountActivated(null, recipients(1));
            assertEquals("", event.commonVariables().get("portalUrl"));
        }
    }

    @Test
    @DisplayName("every builder names a template key that NotificationTemplates declares")
    void templateKeysAreDeclared() {
        List<NotificationDispatchEvent> all = List.of(
                Dispatches.drivePublished("A", "B", "C", "u", recipients(1)),
                Dispatches.driveUpdated("A", "B", "C", "u", recipients(1)),
                Dispatches.applicationSubmitted("A", "B", "C", "u", recipients(1)),
                Dispatches.applicationWithdrawn("A", "B", "C", "u", recipients(1)),
                Dispatches.applicationDisqualified("A", "B", "why", "u", recipients(1)),
                Dispatches.accountActivated("u", recipients(1)),
                Dispatches.eligibilityRunComplete("A", "B", 10, 5, "u", recipients(1)),
                Dispatches.eligibilityRunFailed("A", "B", "boom", "u", recipients(1)),
                Dispatches.applicantPoolSealed("A", "B", 7, "u", recipients(1)));

        List<String> declared = List.of(
                NotificationTemplates.DRIVE_PUBLISHED,
                NotificationTemplates.DRIVE_UPDATED,
                NotificationTemplates.APPLICATION_SUBMITTED,
                NotificationTemplates.APPLICATION_WITHDRAWN,
                NotificationTemplates.APPLICATION_DISQUALIFIED,
                NotificationTemplates.ACCOUNT_ACTIVATED,
                NotificationTemplates.ELIGIBILITY_RUN_COMPLETE,
                NotificationTemplates.ELIGIBILITY_RUN_FAILED,
                NotificationTemplates.APPLICANT_POOL_SEALED);

        for (NotificationDispatchEvent event : all) {
            assertTrue(declared.contains(event.templateKey()),
                    "Unknown template key " + event.templateKey()
                            + " -- notification-service would reject this dispatch at ingestion");
            assertFalse(event.channels().isEmpty());
            assertNotNull(event.subject());
        }
    }
}
