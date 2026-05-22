package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.SuspensionDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

/**
 * Acceptance tests for the system-admin suspension flow (issue #94 /
 * use cases II.6.7 / II.6.8 / II.6.9).
 *
 *   - suspendUser(token, username, hours) — temporary suspension
 *   - suspendPermanently(token, username) — permanent suspension
 *   - unsuspendUser(token, username)      — lift any suspension
 *   - getSuspensions(token)               — list currently suspended members
 *
 * Each test pins one rule: happy path, permission check, edge case.
 */
@ExtendWith(MockitoExtension.class)
class SystemAdminSuspensionAcceptanceTest {

    private static final String ADMIN_TOKEN  = "admin-token";
    private static final String TARGET_USER  = "alice";
    private static final String TARGET_ID    = "member-1";

    @Mock private UserRepository userRepository;
    @Mock private SystemAdminRepository systemAdminRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private Waiting_QueueRepository waitingQueueRepository;
    @Mock private UserService userService;
    @Mock private ComplaintRepository complaintRepository;
    @Mock private ComplaintService complaintService;

    @Spy  private KeyedLock keyedLock = new KeyedLock();

    private SystemAdminService service;
    private Member admin;
    private Member target;

    @BeforeEach
    void setUp() {
        service = new SystemAdminService(
                userRepository, systemAdminRepository, purchaseRepository,
                waitingQueueRepository, userService, complaintRepository,
                complaintService, keyedLock);

        admin  = new System_admin(new Member("admin-1", "admin", "hash"), "System");
        target = new Member(TARGET_ID, TARGET_USER, "hash");
    }

    // ── suspendUser (temporary) ────────────────────────────────────────────

    @Test
    @DisplayName("Admin suspends a user for 2h → target.isSuspended()=true and suspendedUntil is ~now+2h")
    void suspendUser_HappyPath_MarksMemberSuspendedForDuration() {
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername(TARGET_USER)).thenReturn(Optional.of(target));
        when(userRepository.findByMemberId(TARGET_ID)).thenReturn(target);

        LocalDateTime before = LocalDateTime.now();
        service.suspendUser(ADMIN_TOKEN, TARGET_USER, 2);

        assertTrue(target.isSuspended(), "member should be suspended");
        assertFalse(target.isSuspendedPermanently(), "temp suspension must not flip the permanent flag");
        assertTrue(target.getSuspendedUntil().isAfter(before.plusHours(1)),
                "suspendedUntil must be ~2h ahead");
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Non-admin token → suspendUser is denied; member untouched, repo not called")
    void suspendUser_NonAdmin_Denied() {
        Member regular = new Member("u-1", "regular", "hash");
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(regular);

        assertThrows(IllegalArgumentException.class,
                () -> service.suspendUser(ADMIN_TOKEN, TARGET_USER, 1));
        assertFalse(target.isSuspended());
        verify(userRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Unknown user → IllegalArgumentException")
    void suspendUser_UnknownUser_Throws() {
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByMemberId("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.suspendUser(ADMIN_TOKEN, "ghost", 1));
    }

    @Test
    @DisplayName("Cannot suspend another system admin")
    void suspendUser_SuspendingAdmin_Denied() {
        Member otherAdmin = new System_admin(new Member("admin-2", "admin2", "hash"), "System");
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername("admin2")).thenReturn(Optional.of(otherAdmin));
        when(userRepository.findByMemberId("admin-2")).thenReturn(otherAdmin);

        assertThrows(IllegalArgumentException.class,
                () -> service.suspendUser(ADMIN_TOKEN, "admin2", 1));
        assertFalse(otherAdmin.isSuspended());
    }

    // ── suspendPermanently ─────────────────────────────────────────────────

    @Test
    @DisplayName("Admin suspends permanently → flag set, suspendedUntil stays null, isSuspended()=true")
    void suspendPermanently_HappyPath() {
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername(TARGET_USER)).thenReturn(Optional.of(target));
        when(userRepository.findByMemberId(TARGET_ID)).thenReturn(target);

        service.suspendPermanently(ADMIN_TOKEN, TARGET_USER);

        assertTrue(target.isSuspendedPermanently());
        assertTrue(target.isSuspended());
        verify(userRepository).save(target);
    }

    // ── unsuspendUser ──────────────────────────────────────────────────────

    @Test
    @DisplayName("unsuspend a suspended member → flags cleared, isSuspended()=false")
    void unsuspendUser_HappyPath() {
        target.suspend(LocalDateTime.now().plusHours(5));
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername(TARGET_USER)).thenReturn(Optional.of(target));
        when(userRepository.findByMemberId(TARGET_ID)).thenReturn(target);

        service.unsuspendUser(ADMIN_TOKEN, TARGET_USER);

        assertFalse(target.isSuspended());
        assertFalse(target.isSuspendedPermanently());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("unsuspending a non-suspended member is rejected")
    void unsuspendUser_NotSuspended_Throws() {
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findByUsername(TARGET_USER)).thenReturn(Optional.of(target));
        when(userRepository.findByMemberId(TARGET_ID)).thenReturn(target);

        assertThrows(IllegalArgumentException.class,
                () -> service.unsuspendUser(ADMIN_TOKEN, TARGET_USER));
        verify(userRepository, never()).save(any(Member.class));
    }

    // ── getSuspensions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getSuspensions returns only the currently-suspended members")
    void getSuspensions_OnlySuspendedReturned() {
        Member temp = new Member("m-1", "temp", "h");
        temp.suspend(LocalDateTime.now().plusHours(1));

        Member perma = new Member("m-2", "perma", "h");
        perma.suspendPermanently();

        Member clean = new Member("m-3", "clean", "h");

        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(admin);
        when(userRepository.findAll()).thenReturn(List.of(temp, perma, clean));

        List<SuspensionDTO> out = service.getSuspensions(ADMIN_TOKEN);

        assertEquals(2, out.size(), "only temp + permanent should appear");
        assertTrue(out.stream().anyMatch(s -> "temp".equals(s.getUsername()) && !s.isSuspendedPermanently()));
        assertTrue(out.stream().anyMatch(s -> "perma".equals(s.getUsername()) && s.isSuspendedPermanently()));
    }

    @Test
    @DisplayName("non-admin token cannot read the suspensions list")
    void getSuspensions_NonAdmin_Denied() {
        Member regular = new Member("u-1", "regular", "hash");
        when(userService.getMemberByToken(ADMIN_TOKEN)).thenReturn(regular);

        assertThrows(IllegalArgumentException.class,
                () -> service.getSuspensions(ADMIN_TOKEN));
    }
}
