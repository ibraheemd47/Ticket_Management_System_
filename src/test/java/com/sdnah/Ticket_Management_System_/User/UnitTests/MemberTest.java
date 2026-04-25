package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("Member — Domain Unit Tests")
class MemberTest {
    @Mock
    private CompanyRoleAssignment mockRole;

    @Mock
    private CompanyRoleAssignment anotherMockRole;

    @AfterEach
    void teardown() {
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then id is stored correctly")
    void givenValidData_WhenConstructed_ThenIdIsStoredCorrectly() {
        // Arrange
        String memberId = "m-001";

        // Act
        Member member = new Member(memberId, "alice", "hash");

        // Assert
        assertEquals(memberId, member.getMemberId());
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then username is stored correctly")
    void givenValidData_WhenConstructed_ThenUsernameIsStoredCorrectly() {
        // Arrange
        String username = "alice";

        // Act
        Member member = new Member("m-001", username, "hash");

        // Assert
        assertEquals(username, member.getUsername());
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then passwordHash is stored correctly")
    void givenValidData_WhenConstructed_ThenPasswordHashIsStoredCorrectly() {
        // Arrange
        String hash = "hashed_pw";

        // Act
        Member member = new Member("m-001", "alice", hash);

        // Assert
        assertEquals(hash, member.getPasswordHash());
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then member is active by default")
    void givenValidData_WhenConstructed_ThenMemberIsActiveByDefault() {
        // Arrange + Act
        Member member = new Member("m-001", "alice", "hash");

        // Assert
        assertTrue(member.isActive());
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then role is MEMBER")
    void givenValidData_WhenConstructed_ThenRoleIsMember() {
        // Arrange + Act
        Member member = new Member("m-001", "alice", "hash");

        // Assert
        assertEquals(UserRole.MEMBER, member.getRole());
    }

    @Test
    @DisplayName("Given valid data, when Member is constructed, then company roles are empty")
    void givenValidData_WhenConstructed_ThenCompanyRolesAreEmpty() {
        // Arrange + Act
        Member member = new Member("m-001", "alice", "hash");

        // Assert
        assertTrue(member.getCompanyRoles().isEmpty());
    }

    @Test
    @DisplayName("Given invalid data, when Member is constructed, then throws NullPointerException")
    void givenInvalidData_WhenConstructed_ThenThrowsException() {
        assertThrows(NullPointerException.class, () -> new Member(null, "alice", "hash"));
        assertThrows(NullPointerException.class, () -> new Member("m-001", null, "hash"));
        assertThrows(NullPointerException.class, () -> new Member("m-001", "alice", null));
        assertThrows(NullPointerException.class, () -> new Member("", "alice", "hash"));
    }

    // =========================================================================
    // 2. login / logout
    // =========================================================================

    @Test
    @DisplayName("Given a logged-in Member, when logout is called, then isLoggedin returns false")
    void givenLoggedInMember_WhenLogoutCalled_ThenIsLoggedinReturnsFalse() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        member.login();
        assertTrue(member.isLoggedin()); // precondition

        // Act
        member.logout();

        // Assert
        assertFalse(member.isLoggedin());
    }

    @Test
    @DisplayName("Given a logged-out Member, when login is called, then isLoggedin returns true")
    void givenLoggedOutMember_WhenLoginCalled_ThenIsLoggedinReturnsTrue() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        member.logout();
        assertFalse(member.isLoggedin()); // precondition

        // Act
        member.login();

        // Assert
        assertTrue(member.isLoggedin());
    }

    // =========================================================================
    // 3. addCompanyRole — using mocked CompanyRoleAssignment
    // =========================================================================

    @Test
    @DisplayName("Given a Member and a mocked role, when addCompanyRole is called, then role count becomes 1")
    void givenMemberAndMockedRole_WhenAddCompanyRoleCalled_ThenRoleCountBecomesOne() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        // Act
        member.addCompanyRole(mockRole);

        // Assert
        assertEquals(1, member.getCompanyRoles().size());
    }

    @Test
    @DisplayName("Given a Member with a mocked role, when getCompanyRoles is called, then set is unmodifiable")
    void givenMemberWithMockedRole_WhenGetCompanyRolesCalled_ThenSetIsUnmodifiable() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        member.addCompanyRole(mockRole);

        // Act & Assert
        assertThrows(UnsupportedOperationException.class,
                () -> member.getCompanyRoles().add(anotherMockRole));
    }

    // =========================================================================
    // 4. getRoleInCompany — using mocked CompanyRoleAssignment
    // =========================================================================

    @Test
    @DisplayName("Given a Member with a mocked role for company-1, when getRoleInCompany for company-1, then Optional is present")
    void givenMemberWithRoleForCompany1_WhenGetRoleInCompanyForCompany1_ThenOptionalIsPresent() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        member.addCompanyRole(mockRole);

        // Act
        Optional<CompanyRoleAssignment> result = member.getRoleInCompany("company-1");

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Given a Member with no roles, when getRoleInCompany is called, then Optional is empty")
    void givenMemberWithNoRoles_WhenGetRoleInCompanyCalled_ThenOptionalIsEmpty() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");

        // Act
        Optional<CompanyRoleAssignment> result = member.getRoleInCompany("company-1");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Given a Member with a role for company-1, when getRoleInCompany for company-2, then Optional is empty")
    void givenMemberWithRoleForCompany1_WhenGetRoleInCompanyForCompany2_ThenOptionalIsEmpty() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        member.addCompanyRole(mockRole);

        // Act
        Optional<CompanyRoleAssignment> result = member.getRoleInCompany("company-2");

        // Assert
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // 5. isOwnerInCompany — using mocked CompanyRoleAssignment
    // =========================================================================

    @Test
    @DisplayName("Given a Member with an OWNER mocked role, when isOwnerInCompany is called, then returns true")
    void givenMemberWithOwnerMockedRole_WhenIsOwnerInCompanyCalled_ThenReturnsTrue() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        when(mockRole.isOwner()).thenReturn(true);
        member.addCompanyRole(mockRole);

        // Act
        boolean result = member.isOwnerInCompany("company-1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given a Member with a non-OWNER mocked role, when isOwnerInCompany is called, then returns false")
    void givenMemberWithNonOwnerMockedRole_WhenIsOwnerInCompanyCalled_ThenReturnsFalse() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        when(mockRole.isOwner()).thenReturn(false);
        member.addCompanyRole(mockRole);

        // Act
        boolean result = member.isOwnerInCompany("company-1");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given a Member with no roles, when isOwnerInCompany is called, then returns false")
    void givenMemberWithNoRoles_WhenIsOwnerInCompanyCalled_ThenReturnsFalse() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");

        // Act
        boolean result = member.isOwnerInCompany("company-1");

        // Assert
        assertFalse(result);
    }

    // =========================================================================
    // 6. isManagerInCompany — using mocked CompanyRoleAssignment
    // =========================================================================

    @Test
    @DisplayName("Given a Member with a MANAGER mocked role, when isManagerInCompany is called, then returns true")
    void givenMemberWithManagerMockedRole_WhenIsManagerInCompanyCalled_ThenReturnsTrue() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        when(mockRole.isManager()).thenReturn(true);
        member.addCompanyRole(mockRole);

        // Act
        boolean result = member.isManagerInCompany("company-1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given a Member with a non-MANAGER mocked role, when isManagerInCompany is called, then returns false")
    void givenMemberWithNonManagerMockedRole_WhenIsManagerInCompanyCalled_ThenReturnsFalse() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");
        when(mockRole.getCompanyId()).thenReturn("company-1");
        when(mockRole.isManager()).thenReturn(false);
        member.addCompanyRole(mockRole);

        // Act
        boolean result = member.isManagerInCompany("company-1");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given a Member with no roles, when isManagerInCompany is called, then returns false")
    void givenMemberWithNoRoles_WhenIsManagerInCompanyCalled_ThenReturnsFalse() {
        // Arrange
        Member member = new Member("m-001", "alice", "hash");

        // Act
        boolean result = member.isManagerInCompany("company-1");

        // Assert
        assertFalse(result);
    }
}