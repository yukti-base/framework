package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.drive.model.PostingStatus.*;

/**
 * Legal posting lifecycle moves, in one place, shared by admin-service (which
 * authors and publishes) and drive-service (whose deadline sweep closes).
 *
 * Follows the RoleHierarchy precedent in user-dal: Spring-free domain rules live
 * in the DAL that owns the entity, so two services can never drift apart on what
 * a legal transition is.
 *
 * APPLICATIONS_CLOSED is the sealed applicant pool -- the handover point to the
 * future shortlisting engine, which is what moves a posting to IN_PROGRESS.
 */
public final class PostingStatusTransitions {

    private PostingStatusTransitions() {}

    private static final Map<PostingStatus, Set<PostingStatus>> ALLOWED = new EnumMap<>(PostingStatus.class);
    static {
        ALLOWED.put(DRAFT, EnumSet.of(PENDING_APPROVAL, PUBLISHED, CANCELLED));
        ALLOWED.put(PENDING_APPROVAL, EnumSet.of(DRAFT, PUBLISHED, CANCELLED));
        ALLOWED.put(PUBLISHED, EnumSet.of(APPLICATIONS_CLOSED, CANCELLED));
        // -> PUBLISHED is the TnP extending the deadline of a sealed drive
        // (2026-10-27): applications reopen and the pool is sealed again later.
        ALLOWED.put(APPLICATIONS_CLOSED, EnumSet.of(PUBLISHED, IN_PROGRESS, CANCELLED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(PostingStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(PostingStatus.class));
    }

    /** Statuses in which the JD, criteria, targets and policy may still be edited. */
    private static final Set<PostingStatus> EDITABLE = EnumSet.of(DRAFT, PENDING_APPROVAL);

    /** Statuses in which a student may still submit an application. */
    private static final Set<PostingStatus> ACCEPTING_APPLICATIONS = EnumSet.of(PUBLISHED);

    public static boolean canTransition(PostingStatus from, PostingStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isEditable(PostingStatus status) {
        return EDITABLE.contains(status);
    }

    public static boolean isAcceptingApplications(PostingStatus status) {
        return ACCEPTING_APPLICATIONS.contains(status);
    }

    public static boolean isTerminal(PostingStatus status) {
        return ALLOWED.getOrDefault(status, Set.of()).isEmpty();
    }
}
