package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.drive.model.OfferStatus.*;

/**
 * Legal offer lifecycle moves, plus the definition of "live".
 *
 * LIVE is the set the re-application policy cares about: a student holding any
 * live offer is placed or interning for eligibility purposes. It is also what
 * decides whether PlacementOffer.activeInternshipLock is set or nulled, so both
 * must be recomputed together inside the same transaction as any status change.
 */
public final class OfferStatusTransitions {

    private OfferStatusTransitions() {}

    private static final Map<OfferStatus, Set<OfferStatus>> ALLOWED = new EnumMap<>(OfferStatus.class);
    static {
        ALLOWED.put(OFFERED, EnumSet.of(ACCEPTED, DECLINED, REVOKED, LAPSED));
        ALLOWED.put(ACCEPTED, EnumSet.of(JOINED, DECLINED, REVOKED));
        ALLOWED.put(JOINED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(DECLINED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(REVOKED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(LAPSED, EnumSet.noneOf(OfferStatus.class));
    }

    private static final Set<OfferStatus> LIVE = EnumSet.of(OFFERED, ACCEPTED, JOINED);


    public static boolean canTransition(OfferStatus from, OfferStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isLive(OfferStatus status) {
        return LIVE.contains(status);
    }

    /**
     * The value PlacementOffer.activeInternshipLock must hold for this offer.
     * Returns the student id while a live internship stands, null otherwise --
     * which is what makes uk_offer_active_internship_lock mean "at most one live
     * internship per student".
     */
    public static Long internshipLockValue(OfferType type, OfferStatus status, Long studentUserId) {
        return OfferTypes.grantsInternship(type) && isLive(status) ? studentUserId : null;
    }
}
