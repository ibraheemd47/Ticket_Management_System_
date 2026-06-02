package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

/**
 * Coverage tests for UserService — covers role / admin / token helper methods
 * and validateCompanyRoleRequest that the main UserServiceAcceptanceTest
 * doesn't reach.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Coverage Tests")
class UserServiceCoverageTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private AuthTokenService authTokenService;
    @Mock private VerificationEmail verificationService;

    private UserService userService;

    private static final String MEMBER_ID = "m-001";
    private static final String USERNAME = "alice";
    private static final String TOKEN = "jwt-token";
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordHasher, authTokenService,
                verificationService, new KeyedLock());
    }

    private Member member() {
        return new Member(MEMBER_ID, USERNAME, "hashed");
    }

    private void stubTokenResolvesTo(Member m) {
        when(authTokenService.validateToken(TOKEN)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(m));
    }

    // ── getMemberById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getMemberById returns whatever the repository returns")
    void getMemberById_delegates() {
        Member m = member();
        when(userRepository.findByMemberId(MEMBER_ID)).thenReturn(m);

        assertSame(m, userService.getMemberById(MEMBER_ID));
    }

    // ── validateCompanyRoleRequest ──────────────────────────────────────────

    @Test
    @DisplayName("validateCompanyRoleRequest rejects null company id")
    void validateCompanyRoleRequest_nullCompanyId() {
        // The real constructor refuses to build an assignment with a null
        // company id, so mock the assignment to simulate stale persisted data.
        CompanyRoleAssignment a = mock(CompanyRoleAssignment.class);
        when(a.getCompanyId()).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> userService.validateCompanyRoleRequest(a));
    }

    @Test
    @DisplayName("validateCompanyRoleRequest rejects null role type")
    void validateCompanyRoleRequest_nullRoleType() {
        CompanyRoleAssignment a = mock(CompanyRoleAssignment.class);
        when(a.getCompanyId()).thenReturn(COMPANY_ID);
        when(a.getRoleType()).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> userService.validateCompanyRoleRequest(a));
    }

    @Test
    @DisplayName("validateCompanyRoleRequest rejects OWNER assignment without appointedBy")
    void validateCompanyRoleRequest_ownerWithoutAppointedBy() {
        CompanyRoleAssignment a = new CompanyRoleAssignment(
                COMPANY_ID, null, CompanyRoleType.OWNER, Set.of());

        assertThrows(RuntimeException.class,
                () -> userService.validateCompanyRoleRequest(a));
    }

    @Test
    @DisplayName("validateCompanyRoleRequest rejects MANAGER with blank appointedBy")
    void validateCompanyRoleRequest_managerWithBlankAppointedBy() {
        CompanyRoleAssignment a = new CompanyRoleAssignment(
                COMPANY_ID, "  ", CompanyRoleType.MANAGER, Set.of());

        assertThrows(RuntimeException.class,
                () -> userService.validateCompanyRoleRequest(a));
    }

    @Test
    @DisplayName("validateCompanyRoleRequest accepts a well-formed OWNER assignment")
    void validateCompanyRoleRequest_validOwner() {
        CompanyRoleAssignment a = new CompanyRoleAssignment(
                COMPANY_ID, "appointed-by", CompanyRoleType.OWNER, Set.of());

        assertDoesNotThrow(() -> userService.validateCompanyRoleRequest(a));
    }

    // ── getMemberByUsername ─────────────────────────────────────────────────

    @Test
    @DisplayName("getMemberByUsername returns persisted member")
    void getMemberByUsername_success() {
        Member m = member();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(m));

        assertSame(m, userService.getMemberByUsername(USERNAME));
    }

    @Test
    @DisplayName("getMemberByUsername throws when missing")
    void getMemberByUsername_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getMemberByUsername("ghost"));
    }

    // ── requireOwner / requireManager / requireAdmin ────────────────────────

    @Test
    @DisplayName("requireOwner throws when member is not an owner")
    void requireOwner_notAnOwner() {
        Member m = member();
        // Add MANAGER role for the company, so isOwnerInCompany returns false
        m.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, MEMBER_ID, CompanyRoleType.MANAGER, Set.of()));
        stubTokenResolvesTo(m);

        assertThrows(RuntimeException.class,
                () -> userService.requireOwner(TOKEN, COMPANY_ID));
    }

    @Test
    @DisplayName("requireManager returns the member when they are an owner (owner ⊇ manager)")
    void requireManager_ownerCounts() {
        Member m = member();
        m.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, MEMBER_ID, CompanyRoleType.OWNER, Set.of()));
        stubTokenResolvesTo(m);

        assertSame(m, userService.requireManager(TOKEN, COMPANY_ID));
    }

    @Test
    @DisplayName("requireManager throws when member has no role in the company")
    void requireManager_noRole() {
        stubTokenResolvesTo(member());

        assertThrows(RuntimeException.class,
                () -> userService.requireManager(TOKEN, COMPANY_ID));
    }

    @Test
    @DisplayName("requireAdmin returns the member when they are a system admin")
    void requireAdmin_success() {
        Member admin = new System_admin(member(), "System");
        stubTokenResolvesTo(admin);

        assertSame(admin, userService.requireAdmin(TOKEN));
    }

    @Test
    @DisplayName("requireAdmin throws when member is not an admin")
    void requireAdmin_fail() {
        stubTokenResolvesTo(member());

        assertThrows(RuntimeException.class,
                () -> userService.requireAdmin(TOKEN));
    }

    // ── isSystemAdmin / getMemberIdByToken / requireMember / requireMemberId

    @Test
    @DisplayName("isSystemAdmin returns true for admin token")
    void isSystemAdmin_true() {
        Member admin = new System_admin(member(), "System");
        stubTokenResolvesTo(admin);

        assertTrue(userService.isSystemAdmin(TOKEN));
    }

    @Test
    @DisplayName("isSystemAdmin returns false for regular member token")
    void isSystemAdmin_false() {
        stubTokenResolvesTo(member());

        assertFalse(userService.isSystemAdmin(TOKEN));
    }

    @Test
    @DisplayName("getMemberIdByToken pulls memberId from the token-resolved member")
    void getMemberIdByToken_returnsId() {
        stubTokenResolvesTo(member());

        assertEquals(MEMBER_ID, userService.getMemberIdByToken(TOKEN));
    }

    @Test
    @DisplayName("requireMember and requireMemberId resolve via the token")
    void requireMember_and_requireMemberId() {
        Member m = member();
        stubTokenResolvesTo(m);

        assertSame(m, userService.requireMember(TOKEN));
        assertEquals(MEMBER_ID, userService.requireMemberId(TOKEN));
    }

    // ── validateToken ───────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken passes through to AuthTokenService")
    void validateToken_passesThrough() {
        when(authTokenService.validateToken(TOKEN)).thenReturn(true);
        assertTrue(userService.validateToken(TOKEN));

        when(authTokenService.validateToken("bad")).thenReturn(false);
        assertFalse(userService.validateToken("bad"));
    }
}
