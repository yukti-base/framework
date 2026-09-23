package org.yuktisetu.drive.model;

import org.yuktisetu.drive.db.DrivePlacedStudentDecision;
import org.yuktisetu.drive.db.JobPosting;
import org.yuktisetu.drive.db.PlacementOffer;
import org.yuktisetu.drive.db.PostingReapplicationPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The re-application half of the eligibility gate: what a student already holds
 * decides whether they may sit for this drive.
 *
 * Pure and static, taking one student's offers as an argument. The caller batch-
 * loads offers for the whole cohort in a single query and groups them into a map,
 * so evaluating 600 candidates issues one offer query rather than 600 -- and this
 * class never touches a repository, which is what lets both admin-service's bulk
 * resolution and drive-service's apply-time re-check share it verbatim.
 *
 * There is NO one-offer policy. The two halves are independent and both configured
 * per drive:
 *
 *  - INTERNSHIPS: a student holding a live internship is normally locked out of
 *    another internship drive. A full-time conversion drive can let them through.
 *  - FULL-TIME: a placed student MAY sit again, typically only where this drive
 *    pays above their current package by a configured uplift. On top of that the
 *    TnP can rule on individual placed students per drive.
 *
 * Order matters:
 *   1. live-offer count and cooling-off
 *   2. the internship lock
 *   3. the placed / pending stance and its uplift floor
 *   4. DrivePlacedStudentDecision, which overrides EVERYTHING above it in both
 *      directions -- a DENY blocks a student who cleared the uplift, an ALLOW
 *      admits one who did not.
 */
public final class ReapplicationRules {

    private ReapplicationRules() {}

    private static final List<OfferStatus> LIVE_STATUSES =
            List.of(OfferStatus.OFFERED, OfferStatus.ACCEPTED, OfferStatus.JOINED);

    /** The statuses the caller should filter its offer query by. */
    public static List<OfferStatus> liveStatuses() {
        return LIVE_STATUSES;
    }

    /**
     * @param liveOffers this student's live offers only; empty or null is fine
     * @param decision   the TnP's explicit ruling for this drive, or null
     * @return failure codes; empty means the student clears the policy
     */
    public static List<String> check(JobPosting posting,
                                     PostingReapplicationPolicy policy,
                                     List<PlacementOffer> liveOffers,
                                     DrivePlacedStudentDecision decision) {

        // An explicit TnP ruling short-circuits everything, in both directions.
        if (decision != null) {
            return decision.getDecision() == PlacedStudentDecision.DENY
                    ? List.of(EligibilityFailureCode.ADMIN_DENIED)
                    : List.of();
        }

        if (liveOffers == null || liveOffers.isEmpty()) {
            // Nothing held, so no re-application constraint can apply. A student
            // whose only offer was declined or revoked lands here, which gives the
            // allowIfLastOfferDeclined behaviour without a second branch.
            return List.of();
        }

        List<String> failures = new ArrayList<>(4);

        if (policy.getMaxLiveOffers() != null && liveOffers.size() >= policy.getMaxLiveOffers()) {
            failures.add(EligibilityFailureCode.MAX_LIVE_OFFERS_REACHED);
        }

        if (policy.getMinDaysSinceLastOffer() != null) {
            liveOffers.stream()
                    .map(PlacementOffer::getOfferDate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .ifPresent(latest -> {
                        long days = Duration.between(
                                latest.atStartOfDay().toInstant(ZoneOffset.UTC), Instant.now()).toDays();
                        if (days < policy.getMinDaysSinceLastOffer()) {
                            failures.add(EligibilityFailureCode.COOLING_OFF_ACTIVE);
                        }
                    });
        }

        // Classified through OfferTypes so this and the database-level internship
        // lock can never disagree about what counts as an internship.
        boolean holdsLiveInternship = false;
        boolean holdsAcceptedPlacement = false;
        boolean holdsPendingPlacement = false;
        for (PlacementOffer o : liveOffers) {
            if (OfferTypes.grantsInternship(o.getOfferType())) {
                holdsLiveInternship = true;
            }
            if (OfferTypes.grantsPlacement(o.getOfferType())) {
                if (o.getStatus() == OfferStatus.ACCEPTED || o.getStatus() == OfferStatus.JOINED) {
                    holdsAcceptedPlacement = true;
                } else if (o.getStatus() == OfferStatus.OFFERED) {
                    holdsPendingPlacement = true;
                }
            }
        }

        boolean targetIsInternship = OfferTypes.isInternshipDrive(posting.getJobType());

        // --- The internship lock ---
        if (holdsLiveInternship && targetIsInternship) {
            switch (policy.getInterningStudentStance()) {
                case BLOCKED -> failures.add(EligibilityFailureCode.LIVE_INTERNSHIP_HELD);
                case ALLOWED_WITH_UPLIFT -> {
                    if (!clearsStipendUplift(posting, policy, liveOffers)) {
                        failures.add(EligibilityFailureCode.INTERN_UPLIFT_NOT_MET);
                    }
                }
                case ALLOWED -> { /* conversion-style drive, deliberately open */ }
            }
        }

        // An interning student applying to a full-time drive is only constrained if
        // the TnP counts an internship as being placed -- the conversion case the
        // SSOT calls out. Otherwise it is exactly what internships are for.
        if (holdsLiveInternship && !targetIsInternship && policy.isCountInternshipTowardsPlacement()) {
            applyPlacedStance(posting, policy, liveOffers, failures);
        }

        if (holdsAcceptedPlacement) {
            applyPlacedStance(posting, policy, liveOffers, failures);
            // A placed student the TnP has not individually ruled on: this drive
            // says a human must decide, so the uplift alone is not enough.
            if (policy.isRequireAdminDecisionForPlaced()) {
                failures.add(EligibilityFailureCode.ADMIN_DECISION_PENDING);
            }
        }

        if (holdsPendingPlacement && !holdsAcceptedPlacement) {
            switch (policy.getPendingOfferStance()) {
                case BLOCKED -> failures.add(EligibilityFailureCode.PENDING_OFFER_HELD);
                case ALLOWED_WITH_UPLIFT -> {
                    if (!clearsPackageUplift(posting, policy, liveOffers)) {
                        failures.add(EligibilityFailureCode.PLACED_UPLIFT_NOT_MET);
                    }
                }
                case ALLOWED -> { /* free to keep interviewing */ }
            }
        }

        return failures;
    }

    private static void applyPlacedStance(JobPosting posting, PostingReapplicationPolicy policy,
                                          List<PlacementOffer> liveOffers, List<String> failures) {
        switch (policy.getPlacedStudentStance()) {
            case BLOCKED -> failures.add(EligibilityFailureCode.ALREADY_PLACED);
            case ALLOWED_WITH_UPLIFT -> {
                if (!clearsPackageUplift(posting, policy, liveOffers)) {
                    failures.add(EligibilityFailureCode.PLACED_UPLIFT_NOT_MET);
                }
            }
            case ALLOWED -> { /* open drive */ }
        }
    }

    /**
     * Does this drive pay enough above what the student already holds?
     *
     * Compared against their BEST live package, not the most recent: a student
     * holding 6 and 9 LPA offers must clear the uplift over 9, or the rule would
     * let them trade down. Where both a percentage uplift and an absolute floor are
     * configured, the drive must clear both.
     *
     * With no package recorded on the posting the uplift is unknowable, so the
     * student is NOT blocked -- refusing on data the TnP failed to enter would
     * punish the student for an admin omission.
     */
    private static boolean clearsPackageUplift(JobPosting posting, PostingReapplicationPolicy policy,
                                               List<PlacementOffer> liveOffers) {
        BigDecimal offered = posting.getCtcMax() != null ? posting.getCtcMax() : posting.getCtcMin();
        if (offered == null) {
            return true;
        }
        BigDecimal best = liveOffers.stream()
                .map(PlacementOffer::getCtcAmount)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (best == null) {
            return true;
        }
        if (policy.getMinAbsolutePackage() != null
                && offered.compareTo(policy.getMinAbsolutePackage()) < 0) {
            return false;
        }
        if (policy.getMinPackageUpliftPercent() != null) {
            BigDecimal required = best.multiply(BigDecimal.ONE.add(
                    policy.getMinPackageUpliftPercent()
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
            return offered.compareTo(required) >= 0;
        }
        return true;
    }

    private static boolean clearsStipendUplift(JobPosting posting, PostingReapplicationPolicy policy,
                                               List<PlacementOffer> liveOffers) {
        BigDecimal offered = posting.getStipendAmount();
        if (offered == null || policy.getMinStipendUpliftPercent() == null) {
            return true;
        }
        BigDecimal best = liveOffers.stream()
                .filter(o -> OfferTypes.grantsInternship(o.getOfferType()))
                .map(PlacementOffer::getStipendAmount)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (best == null) {
            return true;
        }
        BigDecimal required = best.multiply(BigDecimal.ONE.add(
                policy.getMinStipendUpliftPercent()
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        return offered.compareTo(required) >= 0;
    }
}
