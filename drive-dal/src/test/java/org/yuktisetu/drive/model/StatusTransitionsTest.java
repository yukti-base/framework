package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two state machines that decide what may legally happen to an
 * application and to a posting.
 *
 * These matter to the migration for a specific reason: the apply path and the
 * deadline sweep both consult them while holding a Postgres row lock, and
 * Phase 5 moves the posting lifecycle out of admin-service into drive-service.
 * A transition quietly gained or lost in that move would either strand
 * applications outside a sealed pool or let a cancelled drive accept students.
 *
 * The terminal-state assertions are deliberately exhaustive rather than
 * example-based: a terminal state that silently acquires an outgoing edge is
 * exactly the kind of regression nobody notices until a student disputes it.
 */
class StatusTransitionsTest {

    @Nested
    @DisplayName("application status")
    class Applications {

        @Test
        @DisplayName("the apply path: APPLIED may withdraw, be disqualified, or be sealed into review")
        void appliedOutgoing() {
            assertTrue(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN));
            assertTrue(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.APPLIED, ApplicationStatus.DISQUALIFIED));
            assertTrue(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.APPLIED, ApplicationStatus.IN_REVIEW),
                    "the deadline sweep moves APPLIED -> IN_REVIEW; losing this strands the whole pool");
        }

        @Test
        @DisplayName("a withdrawn application may be re-applied -- the row is revived, not duplicated")
        void withdrawnCanReapply() {
            assertTrue(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.WITHDRAWN, ApplicationStatus.APPLIED));
        }

        @Test
        @DisplayName("APPLIED cannot jump straight to SHORTLISTED, skipping review")
        void noSkippingReview() {
            assertFalse(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED));
            assertFalse(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.APPLIED, ApplicationStatus.OFFERED));
        }

        @Test
        @DisplayName("terminal states have no outgoing transitions at all")
        void terminalStatesAreTerminal() {
            for (ApplicationStatus terminal : new ApplicationStatus[]{
                    ApplicationStatus.REJECTED, ApplicationStatus.DISQUALIFIED,
                    ApplicationStatus.OFFER_ACCEPTED, ApplicationStatus.OFFER_DECLINED}) {
                for (ApplicationStatus to : ApplicationStatus.values()) {
                    assertFalse(ApplicationStatusTransitions.canTransition(terminal, to),
                            terminal + " must be terminal but allows -> " + to);
                }
            }
        }

        @Test
        @DisplayName("a disqualified student cannot be quietly restored to APPLIED")
        void disqualifiedIsNotReversible() {
            assertFalse(ApplicationStatusTransitions.canTransition(
                    ApplicationStatus.DISQUALIFIED, ApplicationStatus.APPLIED));
        }

        @Test
        @DisplayName("only the four statuses this phase implements are reachable now")
        void reachableNowIsTheBuiltSubset() {
            assertTrue(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.APPLIED));
            assertTrue(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.WITHDRAWN));
            assertTrue(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.IN_REVIEW));
            assertTrue(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.DISQUALIFIED));

            // The shortlisting engine is not built; these must stay unreachable
            // so nothing sets a status the rest of the system cannot interpret.
            assertFalse(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.SHORTLISTED));
            assertFalse(ApplicationStatusTransitions.isReachableNow(ApplicationStatus.OFFERED));
        }
    }

    @Nested
    @DisplayName("posting status")
    class Postings {

        @Test
        @DisplayName("the publish path: DRAFT -> PUBLISHED")
        void draftPublishes() {
            assertTrue(PostingStatusTransitions.canTransition(
                    PostingStatus.DRAFT, PostingStatus.PUBLISHED));
        }

        @Test
        @DisplayName("the sealing path: PUBLISHED -> APPLICATIONS_CLOSED")
        void publishedSeals() {
            assertTrue(PostingStatusTransitions.canTransition(
                    PostingStatus.PUBLISHED, PostingStatus.APPLICATIONS_CLOSED),
                    "DeadlineSealingService depends on this edge existing");
        }

        @Test
        @DisplayName("a sealed posting cannot reopen for applications")
        void sealedCannotReopen() {
            assertFalse(PostingStatusTransitions.canTransition(
                    PostingStatus.APPLICATIONS_CLOSED, PostingStatus.PUBLISHED),
                    "reopening a sealed pool would admit students after the deadline");
        }

        @Test
        @DisplayName("cancellation is reachable from every live state, and is terminal")
        void cancellationIsAlwaysAvailableAndFinal() {
            for (PostingStatus live : new PostingStatus[]{
                    PostingStatus.DRAFT, PostingStatus.PENDING_APPROVAL, PostingStatus.PUBLISHED,
                    PostingStatus.APPLICATIONS_CLOSED, PostingStatus.IN_PROGRESS}) {
                assertTrue(PostingStatusTransitions.canTransition(live, PostingStatus.CANCELLED),
                        live + " must be cancellable");
            }
            for (PostingStatus to : PostingStatus.values()) {
                assertFalse(PostingStatusTransitions.canTransition(PostingStatus.CANCELLED, to));
                assertFalse(PostingStatusTransitions.canTransition(PostingStatus.COMPLETED, to));
            }
        }

        @Test
        @DisplayName("only a draft or pending posting is editable -- a published JD is frozen")
        void editabilityIsLimitedToPrePublish() {
            assertTrue(PostingStatusTransitions.isEditable(PostingStatus.DRAFT));
            assertTrue(PostingStatusTransitions.isEditable(PostingStatus.PENDING_APPROVAL));

            assertFalse(PostingStatusTransitions.isEditable(PostingStatus.PUBLISHED),
                    "editing criteria after publish would change eligibility under students' feet");
            assertFalse(PostingStatusTransitions.isEditable(PostingStatus.APPLICATIONS_CLOSED));
            assertFalse(PostingStatusTransitions.isEditable(PostingStatus.COMPLETED));
        }

        @Test
        @DisplayName("applications are accepted ONLY while PUBLISHED")
        void onlyPublishedAcceptsApplications() {
            assertTrue(PostingStatusTransitions.isAcceptingApplications(PostingStatus.PUBLISHED));

            Arrays.stream(PostingStatus.values())
                    .filter(s -> s != PostingStatus.PUBLISHED)
                    .forEach(s -> assertFalse(PostingStatusTransitions.isAcceptingApplications(s),
                            s + " must not accept applications"));
        }
    }
}
