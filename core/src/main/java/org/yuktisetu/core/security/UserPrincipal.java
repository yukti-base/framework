package org.yuktisetu.core.security;

import java.util.List;

/**
 * The reconstructed identity of the caller, built entirely from a verified
 * JWT's claims -- no database lookup, no call back to auth-service. Every
 * service that includes core gets this exact shape; do not let any
 * service-local copy of this class drift from what's here, since the JWT
 * claim structure (role/collegeId/deptId per row) is the actual contract
 * between auth-service (issuer) and everyone else (verifiers).
 */
public record UserPrincipal(
        Long userId,
        String email,
        List<RoleClaim> roles
) {
    // role is a raw String, deliberately NOT the RoleType enum from
    // user-dal -- core has zero DAL dependency (see core/build.gradle) and
    // this type is exactly why that matters. Role names are compared as
    // strings against "ROLE_" + name; keep them in sync with RoleType's
    // enum constant names by convention, not by compile-time reference.
    public record RoleClaim(String role, Long collegeId, Long deptId) {}

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(r -> r.role().equals(role));
    }

    // Trust-wide roles carry null collegeId in their claim -- any match on
    // role name is scope-sufficient for them. College-scoped roles must
    // match the specific collegeId too, since one person can hold a
    // college-scoped role across several (but not all) colleges.
    public boolean hasRoleForCollege(String role, Long collegeId) {
        return roles.stream().anyMatch(r ->
                r.role().equals(role) &&
                        (r.collegeId() == null || r.collegeId().equals(collegeId)));
    }

    public boolean hasRoleForDept(String role, Long deptId) {
        return roles.stream().anyMatch(r ->
                r.role().equals(role) &&
                        (r.deptId() == null || r.deptId().equals(deptId)));
    }
}
