package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ManagerPermission;

class CompanyRoleAssignmentTest {

    private static final int COMPANY_ID = 1;
    private static final String APPOINTED_BY = "m1";

    @Test
    void ownerRole_isRecognizedCorrectly() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.OWNER,
                Set.of());

        assertTrue(assignment.isOwner());
        assertFalse(assignment.isManager());
        assertEquals(COMPANY_ID, assignment.getCompanyId());
        assertEquals(CompanyRoleType.OWNER, assignment.getRoleType());
        assertEquals(APPOINTED_BY, assignment.getAppointedByMemberId());
    }

    @Test
    void managerRole_isRecognizedCorrectly() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.MANAGER,
                Set.of(ManagerPermission.ADD_PRODUCT));

        assertFalse(assignment.isOwner());
        assertTrue(assignment.isManager());
        assertTrue(assignment.hasPermission(ManagerPermission.ADD_PRODUCT));
    }

    @Test
    void addPermission_toManager_addsPermission() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.MANAGER,
                Set.of());

        assignment.addPermission(ManagerPermission.REMOVE_PRODUCT);

        assertTrue(assignment.hasPermission(ManagerPermission.REMOVE_PRODUCT));
    }

    @Test
    void addPermission_toNonManager_throwsException() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.OWNER,
                Set.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> assignment.addPermission(ManagerPermission.ADD_PRODUCT));

        assertEquals("Only managers can have manager permissions", ex.getMessage());
    }

    @Test
    void removePermission_removesExistingPermission() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.MANAGER,
                Set.of(ManagerPermission.ADD_PRODUCT, ManagerPermission.REMOVE_PRODUCT));

        assignment.removePermission(ManagerPermission.ADD_PRODUCT);

        assertFalse(assignment.hasPermission(ManagerPermission.ADD_PRODUCT));
        assertTrue(assignment.hasPermission(ManagerPermission.REMOVE_PRODUCT));
    }

    @Test
    void getPermissions_returnsUnmodifiableSet() {
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, CompanyRoleType.MANAGER,
                Set.of());

        assertThrows(UnsupportedOperationException.class,
                () -> assignment.getPermissions().add(ManagerPermission.ADD_POLICY));
    }

    @Test
    void constructor_withNullRoleType_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new CompanyRoleAssignment(COMPANY_ID, APPOINTED_BY, null, Set.of()));
    }
}