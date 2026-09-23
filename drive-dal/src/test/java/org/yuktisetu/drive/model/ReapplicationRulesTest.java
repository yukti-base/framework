package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.yuktisetu.drive.db.DrivePlacedStudentDecision;
import org.yuktisetu.drive.db.JobPosting;
import org.yuktisetu.drive.db.PlacementOffer;
import org.yuktisetu.drive.db.PostingReapplicationPolicy;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the re-application policy: who, having already been offered something,
 * may sit for another drive.
 *
 * This is the most disputable logic in the system. A student told "you cannot
 * apply, you are already placed" will ask why, and the answer has to be the same
 * one the code actually applied. There is deliberately NO one-offer policy here:
 * interning students are locked out of other internship drives, placed students
 * may sit again above a configurable uplift, and the TnP's explicit per-drive
 * ruling overrides both.
 *
 * The migration touches this indirectly -- `PlacementOffer` loses its
 * @ManyToOne to User and College in Phase 2 -- so these assertions exist to
 * prove the rules read the same after the relationships become bare ids.
 */
class ReapplicationRulesTest {

    // ---------------------------------------------------------------- builders

    private static JobPosting posting(JobType jobType, BigDecimal ctcMax) {
        JobPosting p = new JobPosting();
        p.setJobType(jobType);
        p.setCtcMax(ctcMax);
        return p;
    }

    private static PostingReapplicationPolicy policy(ReapplicationStance placed,
                                                     ReapplicationStance interning,
                                                     ReapplicationStance pending) {
        PostingReapplicationPolicy p = new PostingReapplicationPolicy();
        p.setPlacedStudentStance(placed);
        p.setInterningStudentStance(interning);
        p.setPendingOfferStance(pending);
        p.setCountInternshipTowardsPlacement(false);
        p.setRequireAdminDecisionForPlaced(false);
        return p;
    }

    private static PlacementOffer offer(OfferType type, OfferStatus status, BigDecimal ctc) {
        PlacementOffer o = new PlacementOffer();
        o.setOfferType(type);
        o.setStatus(status);
        o.setCtcAmount(ctc);
        return o;
    }

    // ------------------------------------------- the TnP's explicit ruling

    @Nested
    @DisplayName("per-drive admin discretion overrides everything")
    class AdminDecision {

        /**
         * The SSOT override says the TnP Admin decides, per drive, which already
         * placed students may sit. That ruling is an audited row, not a computed
         * boolean -- so it must short-circuit the entire rule set in BOTH
         * directions.
         */
        @Test
        @DisplayName("an explicit DENY blocks a student who would otherwise qualify")
        void denyOverridesEverything() {
            DrivePlacedStudentDecision denied = new DrivePlacedStudentDecision();
            denied.setDecision(PlacedStudentDecision.DENY);

            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("1200000")),
                    policy(ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED),
                    List.of(),                      // no offers at all -- would normally pass
                    denied);

            assertEquals(List.of(EligibilityFailureCode.ADMIN_DENIED), failures);
        }

        @Test
        @DisplayName("an explicit ALLOW clears a student the policy would otherwise block")
        void allowOverridesEverything() {
            DrivePlacedStudentDecision allowed = new DrivePlacedStudentDecision();
            allowed.setDecision(PlacedStudentDecision.ALLOW);

            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("500000")),
                    policy(ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED),
                    List.of(offer(OfferType.FULL_TIME, OfferStatus.ACCEPTED, new BigDecimal("2000000"))),
                    allowed);

            assertTrue(failures.isEmpty(), "an explicit TnP allowance must beat a BLOCKED stance");
        }
    }

    // ------------------------------------------------------- the empty case

    @Test
    @DisplayName("a student holding nothing is never constrained")
    void noLiveOffersIsAlwaysClear() {
        assertTrue(ReapplicationRules.check(
                posting(JobType.FULL_TIME, new BigDecimal("900000")),
                policy(ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED),
                List.of(), null).isEmpty());
    }

    @Test
    @DisplayName("null offers is treated as none, not as an error")
    void nullOffersIsTolerated() {
        assertTrue(ReapplicationRules.check(
                posting(JobType.FULL_TIME, new BigDecimal("900000")),
                policy(ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED),
                null, null).isEmpty());
    }

    // ---------------------------------------------------- the internship lock

    @Nested
    @DisplayName("internship lock")
    class InternshipLock {

        @Test
        @DisplayName("a live intern is blocked from ANOTHER internship drive")
        void internBlockedFromInternshipDrive() {
            List<String> failures = ReapplicationRules.check(
                    posting(JobType.INTERNSHIP, null),
                    policy(ReapplicationStance.ALLOWED, ReapplicationStance.BLOCKED, ReapplicationStance.ALLOWED),
                    List.of(offer(OfferType.INTERNSHIP, OfferStatus.ACCEPTED, null)),
                    null);

            assertTrue(failures.contains(EligibilityFailureCode.LIVE_INTERNSHIP_HELD));
        }

        /**
         * The half people get wrong. Holding an internship must NOT block a
         * full-time drive by default -- converting an internship into a job is
         * the entire point of the internship. It only constrains when the TnP
         * has explicitly said an internship counts as being placed.
         */
        @Test
        @DisplayName("a live intern is NOT blocked from a full-time drive by default")
        void internNotBlockedFromFullTimeByDefault() {
            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("1000000")),
                    policy(ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED, ReapplicationStance.ALLOWED),
                    List.of(offer(OfferType.INTERNSHIP, OfferStatus.ACCEPTED, null)),
                    null);

            assertTrue(failures.isEmpty(),
                    "an internship must not count as placement unless the TnP says so");
        }

        @Test
        @DisplayName("...but it does block when the TnP counts internships towards placement")
        void internBlockedFromFullTimeWhenCounted() {
            PostingReapplicationPolicy p = policy(
                    ReapplicationStance.BLOCKED, ReapplicationStance.BLOCKED, ReapplicationStance.ALLOWED);
            p.setCountInternshipTowardsPlacement(true);

            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("1000000")),
                    p,
                    List.of(offer(OfferType.INTERNSHIP, OfferStatus.ACCEPTED, null)),
                    null);

            assertTrue(failures.contains(EligibilityFailureCode.ALREADY_PLACED));
        }

        @Test
        @DisplayName("a conversion drive (ALLOWED) lets an intern sit")
        void conversionDriveAllowsIntern() {
            List<String> failures = ReapplicationRules.check(
                    posting(JobType.INTERNSHIP, null),
                    policy(ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED),
                    List.of(offer(OfferType.INTERNSHIP, OfferStatus.ACCEPTED, null)),
                    null);

            assertTrue(failures.isEmpty());
        }
    }

    // ------------------------------------------------------- placed students

    @Nested
    @DisplayName("already-placed students")
    class Placed {

        @Test
        @DisplayName("BLOCKED stance rejects a placed student outright")
        void blockedRejectsPlaced() {
            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("1500000")),
                    policy(ReapplicationStance.BLOCKED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED),
                    List.of(offer(OfferType.FULL_TIME, OfferStatus.ACCEPTED, new BigDecimal("800000"))),
                    null);

            assertTrue(failures.contains(EligibilityFailureCode.ALREADY_PLACED));
        }

        @Test
        @DisplayName("ALLOWED stance lets a placed student sit for a better role")
        void allowedLetsPlacedSit() {
            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("1500000")),
                    policy(ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED),
                    List.of(offer(OfferType.FULL_TIME, OfferStatus.ACCEPTED, new BigDecimal("800000"))),
                    null);

            assertTrue(failures.isEmpty());
        }

        /**
         * The uplift is a floor, not the final word: even when the package
         * clears it, a drive that demands a human ruling must still stop and
         * wait for one.
         */
        @Test
        @DisplayName("requireAdminDecisionForPlaced stops a placed student even under an open stance")
        void adminDecisionPendingBlocksUntilRuled() {
            PostingReapplicationPolicy p = policy(
                    ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED);
            p.setRequireAdminDecisionForPlaced(true);

            List<String> failures = ReapplicationRules.check(
                    posting(JobType.FULL_TIME, new BigDecimal("2500000")),
                    p,
                    List.of(offer(OfferType.FULL_TIME, OfferStatus.ACCEPTED, new BigDecimal("800000"))),
                    null);

            assertTrue(failures.contains(EligibilityFailureCode.ADMIN_DECISION_PENDING));
        }

        @Test
        @DisplayName("a declined offer is not a live offer -- the student is free again")
        void declinedOfferDoesNotConstrain() {
            // liveStatuses() defines what counts; a DECLINED offer must not appear
            // in the caller's list, so the student lands in the "holds nothing" path.
            assertFalse(ReapplicationRules.liveStatuses().contains(OfferStatus.DECLINED),
                    "a declined offer must never count as held");
        }

        @Test
        @DisplayName("a revoked offer is not a live offer either")
        void revokedOfferDoesNotConstrain() {
            assertFalse(ReapplicationRules.liveStatuses().contains(OfferStatus.REVOKED));
        }
    }

    // ------------------------------------------------------- pending offers

    @Test
    @DisplayName("a pending (un-accepted) offer blocks under a BLOCKED pending stance")
    void pendingOfferBlocks() {
        List<String> failures = ReapplicationRules.check(
                posting(JobType.FULL_TIME, new BigDecimal("1500000")),
                policy(ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.BLOCKED),
                List.of(offer(OfferType.FULL_TIME, OfferStatus.OFFERED, new BigDecimal("900000"))),
                null);

        assertTrue(failures.contains(EligibilityFailureCode.PENDING_OFFER_HELD));
    }

    @Test
    @DisplayName("maxLiveOffers is enforced independently of stance")
    void maxLiveOffersEnforced() {
        PostingReapplicationPolicy p = policy(
                ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED, ReapplicationStance.ALLOWED);
        p.setMaxLiveOffers(1);

        List<String> failures = ReapplicationRules.check(
                posting(JobType.FULL_TIME, new BigDecimal("1500000")),
                p,
                List.of(offer(OfferType.FULL_TIME, OfferStatus.ACCEPTED, new BigDecimal("800000"))),
                null);

        assertTrue(failures.contains(EligibilityFailureCode.MAX_LIVE_OFFERS_REACHED));
    }
}
