package org.yuktisetu.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.model.RoleType.*;

/**
 * Single source of truth for the role-to-role hierarchy, shared by every
 * service that needs to know who outranks whom: auth-service (role
 * create/deactivate authority) and user-service (profile-visibility --
 * a role may view the profile of any role it could have created). Lives
 * here rather than in core because core deliberately has zero dependency
 * on RoleType / user-dal (see core/build.gradle and UserPrincipal) -- this
 * class only needs to be visible to services that already depend on
 * user-dal directly.
 */
public final class RoleHierarchy {

    private RoleHierarchy() {}

    // Who is allowed to CREATE which target role.
    private static final Map<RoleType, Set<RoleType>> CAN_CREATE = new EnumMap<>(RoleType.class);
    static {
        CAN_CREATE.put(TNP_SUPER_ADMIN, EnumSet.of(TNP_SUPER_ADMIN, IT_ADMIN, TNP_COLLEGE_ADMIN, HOD));
        CAN_CREATE.put(IT_ADMIN, EnumSet.of(TNP_SUPER_ADMIN, IT_ADMIN, TNP_COLLEGE_ADMIN, HOD, TNP_COORDINATOR, STUDENT, GROUND_VOLUNTEER, FACULTY_DEPT_COORDINATOR));
        CAN_CREATE.put(TNP_COLLEGE_ADMIN, EnumSet.of(TNP_COORDINATOR, HOD));
        CAN_CREATE.put(TNP_COORDINATOR, EnumSet.of(HOD, GROUND_VOLUNTEER));
        CAN_CREATE.put(HOD, EnumSet.of(FACULTY_DEPT_COORDINATOR, STUDENT));
        CAN_CREATE.put(FACULTY_DEPT_COORDINATOR, EnumSet.of(STUDENT)); // students come via bulk upload, not this flow
    }

    // Who is allowed to DEACTIVATE which target role. (Hard-delete is separate — IT_ADMIN only, everywhere.)
    private static final Map<RoleType, Set<RoleType>> CAN_DEACTIVATE = new EnumMap<>(RoleType.class);
    static {
        CAN_DEACTIVATE.put(STUDENT, EnumSet.of(FACULTY_DEPT_COORDINATOR, HOD, TNP_COORDINATOR, TNP_COLLEGE_ADMIN, TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(FACULTY_DEPT_COORDINATOR, EnumSet.of(HOD, TNP_COORDINATOR, TNP_COLLEGE_ADMIN, TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(GROUND_VOLUNTEER, EnumSet.of(TNP_COORDINATOR, TNP_COLLEGE_ADMIN, TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(TNP_COORDINATOR, EnumSet.of(TNP_COLLEGE_ADMIN, TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(TNP_COLLEGE_ADMIN, EnumSet.of(TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(TNP_SUPER_ADMIN, EnumSet.of(IT_ADMIN));
        CAN_DEACTIVATE.put(IT_ADMIN, EnumSet.of(TNP_SUPER_ADMIN));
        CAN_DEACTIVATE.put(HOD, EnumSet.of(TNP_COORDINATOR, TNP_COLLEGE_ADMIN, TNP_SUPER_ADMIN));
    }

    // Immediate child role(s) below a given role — used only for the
    // "last active holder with live subordinates" deactivation guard.
    private static final Map<RoleType, Set<RoleType>> CHILDREN = new EnumMap<>(RoleType.class);
    static {
        CHILDREN.put(TNP_SUPER_ADMIN, EnumSet.of(TNP_COLLEGE_ADMIN));
        CHILDREN.put(TNP_COLLEGE_ADMIN, EnumSet.of(TNP_COORDINATOR));
        CHILDREN.put(TNP_COORDINATOR, EnumSet.of(HOD, GROUND_VOLUNTEER));
        CHILDREN.put(HOD, EnumSet.of(FACULTY_DEPT_COORDINATOR));
        CHILDREN.put(FACULTY_DEPT_COORDINATOR, EnumSet.of(STUDENT));
        CHILDREN.put(GROUND_VOLUNTEER, EnumSet.noneOf(RoleType.class));
        CHILDREN.put(IT_ADMIN, EnumSet.noneOf(RoleType.class));
        CHILDREN.put(STUDENT, EnumSet.noneOf(RoleType.class));
    }

    // Total order over roles, highest authority first. Exists so that a person
    // holding more than one role has their acting role decided by an explicit,
    // deterministic rule rather than by whatever order the JWT claim list
    // happened to arrive in. IT_ADMIN sits above TNP_SUPER_ADMIN deliberately:
    // it can create and delete every role, so ranking it lower would let the
    // weaker claim win for someone holding both.
    private static final Map<RoleType, Integer> AUTHORITY_RANK = new EnumMap<>(RoleType.class);
    static {
        AUTHORITY_RANK.put(IT_ADMIN, 70);
        AUTHORITY_RANK.put(TNP_SUPER_ADMIN, 60);
        AUTHORITY_RANK.put(TNP_COLLEGE_ADMIN, 50);
        AUTHORITY_RANK.put(TNP_COORDINATOR, 40);
        AUTHORITY_RANK.put(HOD, 30);
        AUTHORITY_RANK.put(FACULTY_DEPT_COORDINATOR, 20);
        AUTHORITY_RANK.put(GROUND_VOLUNTEER, 10);
        AUTHORITY_RANK.put(STUDENT, 0);
    }

    // Roles scoped by college_id (everything except the two trust-wide roles).
    private static final Set<RoleType> COLLEGE_SCOPED = EnumSet.of(
            STUDENT, FACULTY_DEPT_COORDINATOR, HOD, GROUND_VOLUNTEER, TNP_COORDINATOR, TNP_COLLEGE_ADMIN);

    // Roles that ALSO require dept_id on top of college_id.
    private static final Set<RoleType> DEPT_SCOPED = EnumSet.of(STUDENT, FACULTY_DEPT_COORDINATOR, HOD);

    public static boolean canCreate(RoleType actor, RoleType target) {
        return CAN_CREATE.getOrDefault(actor, Set.of()).contains(target);
    }

    // Alias for canCreate, used where the check is conceptually "can actor
    // manage/view target's role" rather than literally creating a new user
    // (e.g. user-service's profile-visibility policy) — same rule, different
    // call-site vocabulary.
    public static boolean canManage(RoleType actor, RoleType target) {
        return canCreate(actor, target);
    }

    public static boolean canDeactivate(RoleType actor, RoleType target) {
        return CAN_DEACTIVATE.getOrDefault(actor, Set.of()).contains(target);
    }

    public static Set<RoleType> childrenOf(RoleType role) {
        return CHILDREN.getOrDefault(role, Set.of());
    }

    public static boolean isCollegeScoped(RoleType role) {
        return COLLEGE_SCOPED.contains(role);
    }

    public static boolean isDeptScoped(RoleType role) {
        return DEPT_SCOPED.contains(role);
    }

    public static boolean isTrustWide(RoleType role) {
        return !isCollegeScoped(role);
    }

    /**
     * Where this role sits in the authority order. Higher wins. Used only to
     * pick the acting role when one person legitimately holds several -- never
     * as a permission check in its own right, since CAN_CREATE / CAN_DEACTIVATE
     * are not simple rank comparisons (a TnP Coordinator outranks a Ground
     * Volunteer but still cannot create a College Admin).
     */
    public static int authorityRank(RoleType role) {
        return AUTHORITY_RANK.getOrDefault(role, Integer.MIN_VALUE);
    }
}
