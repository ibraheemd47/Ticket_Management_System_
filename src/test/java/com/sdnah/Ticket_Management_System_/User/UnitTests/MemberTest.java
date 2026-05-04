package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;

@DisplayName("Member — Domain Unit Tests")
class MemberTest {

    private static final int COMPANY_ID = 1;
    private static final int OTHER_COMPANY_ID = 2;

    @Test
    @DisplayName("Given valid data, when Member is constructed, then fields are initialized correctly")
    void givenValidData_WhenConstructed_ThenFieldsAreInitializedCorrectly() {
        Member member = new Member("m-001", "alice", "hash");

        assertEquals("m-001", member.getMemberId());
        assertEquals("alice", member.getUsername());
        assertEquals("hash", member.getPasswordHash());
        assertTrue(member.isActive());
        assertFalse(member.isLoggedin());
        assertFalse(member.isVerified());
        assertEquals(UserRole.MEMBER, member.getRole());
        assertTrue(member.getCompanyRoles().isEmpty());
    }

    @Test
    @DisplayName("Given null or empty memberId, when constructing Member, then IllegalArgumentException is thrown")
    void givenInvalidMemberId_WhenConstructed_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () -> new Member(null, "alice", "hash"));
        assertThrows(IllegalArgumentException.class, () -> new Member("", "alice", "hash"));
    }

    @Test
    @DisplayName("Given null or empty username, when constructing Member, then NullPointerException is thrown")
    void givenInvalidUsername_WhenConstructed_ThenNullPointerExceptionIsThrown() {
        assertThrows(NullPointerException.class, () -> new Member("m-001", null, "hash"));
        assertThrows(NullPointerException.class, () -> new Member("m-001", "", "hash"));
    }

    @Test
    @DisplayName("Given null or empty passwordHash, when constructing Member, then NullPointerException is thrown")
    void givenInvalidPasswordHash_WhenConstructed_ThenNullPointerExceptionIsThrown() {
        assertThrows(NullPointerException.class, () -> new Member("m-001", "alice", null));
        assertThrows(NullPointerException.class, () -> new Member("m-001", "alice", ""));
    }

    @Test
    @DisplayName("Given logged out member, when login is called, then member is logged in")
    void givenLoggedOutMember_WhenLoginCalled_ThenMemberIsLoggedIn() {
        Member member = new Member("m-001", "alice", "hash");

        member.login();

        assertTrue(member.isLoggedin());
    }

    @Test
    @DisplayName("Given logged in member, when logout is called, then member is logged out")
    void givenLoggedInMember_WhenLogoutCalled_ThenMemberIsLoggedOut() {
        Member member = new Member("m-001", "alice", "hash");
        member.login();

        member.logout();

        assertFalse(member.isLoggedin());
    }

    @Test
    @DisplayName("Given member role assignment, when role is added, then role can be found by company id")
    void givenMemberRoleAssignment_WhenRoleAdded_ThenRoleCanBeFoundByCompanyId() {
        Member member = new Member("m-001", "alice", "hash");
        CompanyRoleAssignment assignment = new CompanyRoleAssignment(COMPANY_ID, "owner-1", CompanyRoleType.OWNER,
                Set.of());

        member.addCompanyRole(assignment);

        Optional<CompanyRoleAssignment> result = member.getRoleInCompany(COMPANY_ID);

        assertTrue(result.isPresent());
        assertEquals(CompanyRoleType.OWNER, result.get().getRoleType());
    }

    @Test
    @DisplayName("Given member role for different company, when getRoleInCompany is called, then empty optional is returned")
    void givenRoleForDifferentCompany_WhenGetRoleInCompanyCalled_ThenEmptyOptionalIsReturned() {
        Member member = new Member("m-001", "alice", "hash");
        member.addCompanyRole(
                new CompanyRoleAssignment(COMPANY_ID, "owner-1", CompanyRoleType.OWNER, Set.of()));

        assertTrue(member.getRoleInCompany(OTHER_COMPANY_ID).isEmpty());
    }

    @Test
    @DisplayName("Given owner role, when isOwnerInCompany is called, then true is returned")
    void givenOwnerRole_WhenIsOwnerInCompanyCalled_ThenTrueIsReturned() {
        Member member = new Member("m-001", "alice", "hash");
        member.addCompanyRole(
                new CompanyRoleAssignment(COMPANY_ID, "owner-1", CompanyRoleType.OWNER, Set.of()));

        assertTrue(member.isOwnerInCompany(COMPANY_ID));
        assertFalse(member.isManagerInCompany(COMPANY_ID));
    }

    @Test
    @DisplayName("Given manager role, when isManagerInCompany is called, then true is returned")
    void givenManagerRole_WhenIsManagerInCompanyCalled_ThenTrueIsReturned() {
        Member member = new Member("m-001", "alice", "hash");
        member.addCompanyRole(
                new CompanyRoleAssignment(COMPANY_ID, "owner-1", CompanyRoleType.MANAGER, Set.of()));

        assertTrue(member.isManagerInCompany(COMPANY_ID));
        assertFalse(member.isOwnerInCompany(COMPANY_ID));
    }

    @Test
    @DisplayName("Given company roles view, when trying to modify it, then UnsupportedOperationException is thrown")
    void givenCompanyRolesView_WhenTryingToModify_ThenUnsupportedOperationExceptionIsThrown() {
        Member member = new Member("m-001", "alice", "hash");

        assertThrows(UnsupportedOperationException.class,
                () -> member.getCompanyRoles().add(
                        new CompanyRoleAssignment(COMPANY_ID, "owner-1", CompanyRoleType.OWNER, Set.of())));
    }

    @Test
    @DisplayName("Given system admin role, when isSystemAdmin is called, then true is returned")
    void givenSystemAdminRole_WhenIsSystemAdminCalled_ThenTrueIsReturned() {
        Member member = new Member("m-001", "alice", "hash");

        member.setRole(UserRole.SYSTEM_ADMIN);

        assertTrue(member.isSystemAdmin());
    }
}