package org.yuktisetu.drive.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * What each offer type actually GRANTS a student -- an internship, or a
 * placement.
 *
 * One definition, shared, because two things depend on it and they must never
 * disagree: OfferStatusTransitions computes the database-level internship lock
 * from it, and ReapplicationGuard decides eligibility from it. Were they to drift,
 * a student could be locked out of a drive by the guard while the database
 * happily accepted a second internship offer, or worse the reverse.
 *
 * The classification:
 *
 *  - INTERNSHIP_GRANTING -- anything that puts a live internship in the
 *    student's hands. COMPETITION_WITH_INTERNSHIP is included because a
 *    hackathon whose prize is an internship leaves the student just as
 *    interning as a normal internship drive would.
 *
 *  - PLACEMENT_GRANTING -- anything that amounts to a job. PPO is a
 *    pre-placement offer, which is a job offer. COMPETITION_WITH_PPO likewise.
 *    OTHER is deliberately counted here: an unclassified offer is most likely a
 *    placement, and the two ways of being wrong are not symmetric. Wrongly
 *    gating a student is recoverable through the audited
 *    DrivePlacedStudentDecision override; wrongly letting a placed student take
 *    a second offer damages a recruiter relationship the TnP cannot repair.
 *
 *  - Neither -- APPRENTICESHIP and plain COMPETITION. An apprenticeship is a
 *    training position rather than a placement, and a competition with no offer
 *    attached grants nothing, so neither constrains what a student may sit for.
 */
public final class OfferTypes {

    private OfferTypes() {}

    private static final Set<OfferType> INTERNSHIP_GRANTING = EnumSet.of(
            OfferType.INTERNSHIP,
            OfferType.INTERNSHIP_WITH_PPO,
            OfferType.COMPETITION_WITH_INTERNSHIP);

    private static final Set<OfferType> PLACEMENT_GRANTING = EnumSet.of(
            OfferType.FULL_TIME,
            OfferType.PPO,
            OfferType.INTERNSHIP_WITH_PPO,
            OfferType.COMPETITION_WITH_PPO,
            OfferType.OTHER);

    public static boolean grantsInternship(OfferType type) {
        return type != null && INTERNSHIP_GRANTING.contains(type);
    }

    public static boolean grantsPlacement(OfferType type) {
        return type != null && PLACEMENT_GRANTING.contains(type);
    }

    /**
     * The offer type a drive of this job type would produce.
     *
     * An explicit switch rather than OfferType.valueOf(jobType.name()): the two
     * enums happen to mirror each other today, and if either gains a value the
     * switch stops compiling instead of throwing at runtime on a real drive.
     */
    public static OfferType forJobType(JobType jobType) {
        return switch (jobType) {
            case FULL_TIME -> OfferType.FULL_TIME;
            case INTERNSHIP -> OfferType.INTERNSHIP;
            case INTERNSHIP_WITH_PPO -> OfferType.INTERNSHIP_WITH_PPO;
            case PPO -> OfferType.PPO;
            case APPRENTICESHIP -> OfferType.APPRENTICESHIP;
            case COMPETITION -> OfferType.COMPETITION;
            case COMPETITION_WITH_PPO -> OfferType.COMPETITION_WITH_PPO;
            case COMPETITION_WITH_INTERNSHIP -> OfferType.COMPETITION_WITH_INTERNSHIP;
            case OTHER -> OfferType.OTHER;
        };
    }

    /** Does a drive of this job type place a student under the internship lock? */
    public static boolean isInternshipDrive(JobType jobType) {
        return grantsInternship(forJobType(jobType));
    }
}
