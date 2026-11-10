package org.yuktisetu.identity.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.yuktisetu.identity.model.RoleType.*;

class RoleHierarchyTest {

    @Test
    void trustWideAdminsViewEveryRoleIncludingEachOther() {
        assertEquals(EnumSet.allOf(RoleType.class), RoleHierarchy.viewableBy(IT_ADMIN));
        assertEquals(EnumSet.allOf(RoleType.class), RoleHierarchy.viewableBy(TNP_SUPER_ADMIN));
        assertTrue(RoleHierarchy.canView(TNP_SUPER_ADMIN, STUDENT));
        assertTrue(RoleHierarchy.canView(TNP_SUPER_ADMIN, IT_ADMIN));
    }

    @Test
    void everyOtherRoleViewsEverythingBelowIt() {
        assertEquals(EnumSet.of(TNP_COORDINATOR, HOD, GROUND_VOLUNTEER, FACULTY_DEPT_COORDINATOR, STUDENT),
                RoleHierarchy.viewableBy(TNP_COLLEGE_ADMIN));
        assertEquals(EnumSet.of(HOD, GROUND_VOLUNTEER, FACULTY_DEPT_COORDINATOR, STUDENT),
                RoleHierarchy.viewableBy(TNP_COORDINATOR));
        assertEquals(EnumSet.of(FACULTY_DEPT_COORDINATOR, STUDENT), RoleHierarchy.viewableBy(HOD));
        assertEquals(EnumSet.of(STUDENT), RoleHierarchy.viewableBy(FACULTY_DEPT_COORDINATOR));
    }

    @Test
    void nobodyViewsPeersOrAboveAndLeavesSeeNobody() {
        assertFalse(RoleHierarchy.canView(TNP_COLLEGE_ADMIN, TNP_COLLEGE_ADMIN));
        assertFalse(RoleHierarchy.canView(HOD, TNP_COORDINATOR));
        assertTrue(RoleHierarchy.viewableBy(GROUND_VOLUNTEER).isEmpty());
        assertTrue(RoleHierarchy.viewableBy(STUDENT).isEmpty());
    }

    @Test
    void viewableByReturnsACopy() {
        RoleHierarchy.viewableBy(HOD).clear();
        assertTrue(RoleHierarchy.canView(HOD, STUDENT));
    }

    @Test
    void createTableIsUnchanged() {
        assertFalse(RoleHierarchy.canManage(TNP_SUPER_ADMIN, STUDENT));
    }
}
