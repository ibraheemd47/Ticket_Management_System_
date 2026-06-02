package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.UserDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

/**
 * Coverage-focused unit tests for SystemAdminService — fills gaps in
 * close_company / open_company, queue management, removeMember, purchase
 * lookups and complaint admin handlers that the existing Acceptance test
 * suite doesn't reach.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemAdminService — Coverage Tests")
class SystemAdminServiceCoverageTest {

    @Mock private UserRepository userRepository;
    @Mock private SystemAdminRepository systemAdminRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private Waiting_QueueRepository waitingQueueRepository;
    @Mock private UserService userService;
    @Mock private ComplaintRepository complaintRepository;
    @Mock private ComplaintService complaintService;
    @Mock private NotificationService notificationService;

    private SystemAdminService systemAdminService;

    private Member adminMember;
    private Member regularMember;

    private static final String TOKEN = "admin-token";
    private static final String ADMIN_ID = "admin-1";
    private static final String MEMBER_ID = "member-1";

    @BeforeEach
    void setUp() {
        systemAdminService = new SystemAdminService(
                userRepository, systemAdminRepository, purchaseRepository,
                waitingQueueRepository, userService, complaintRepository,
                complaintService, notificationService, new KeyedLock());

        adminMember = new System_admin(new Member(ADMIN_ID, "admin", "hash"), "System");
        regularMember = new Member(MEMBER_ID, "alice", "hash");

        // Default: token resolves to the admin user. Individual tests override.
        lenient().when(userService.getMemberByToken(TOKEN)).thenReturn(adminMember);
    }

    // ── close_company / open_company ─────────────────────────────────────────

    @Test
    @DisplayName("close_company throws UnsupportedOperationException once domain wiring exists")
    void close_company_isStubbedOut() {
        assertThrows(UnsupportedOperationException.class,
                () -> systemAdminService.close_company(TOKEN, "company-1"));
    }

    @Test
    @DisplayName("close_company rejects blank company id with friendly error")
    void close_company_rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.close_company(TOKEN, "  "));
    }

    @Test
    @DisplayName("open_company throws UnsupportedOperationException once domain wiring exists")
    void open_company_isStubbedOut() {
        assertThrows(UnsupportedOperationException.class,
                () -> systemAdminService.open_company(TOKEN, "company-1"));
    }

    @Test
    @DisplayName("open_company rejects null company id with friendly error")
    void open_company_rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.open_company(TOKEN, null));
    }

    // ── getAllUsers / getLoggedInUsersCount ──────────────────────────────────

    @Test
    @DisplayName("getAllUsers returns one DTO per repository member")
    void getAllUsers_mapsEveryMember() {
        Member m2 = new Member("m-2", "bob", "hash2");
        when(userRepository.findAll()).thenReturn(List.of(regularMember, m2));

        List<UserDTO> result = systemAdminService.getAllUsers(TOKEN);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getLoggedInUsersCount counts only logged-in members")
    void getLoggedInUsersCount_counts() {
        Member loggedIn = new Member("m-2", "bob", "hash2");
        loggedIn.login();
        // regularMember stays logged out
        when(userRepository.findAll()).thenReturn(List.of(regularMember, loggedIn));

        assertEquals(1, systemAdminService.getLoggedInUsersCount(TOKEN));
    }

    // ── removeMember ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeMember deletes the target when found and not an admin")
    void removeMember_success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(regularMember));
        when(userRepository.findByMemberId(MEMBER_ID)).thenReturn(regularMember);

        systemAdminService.removeMember(TOKEN, "alice");

        verify(userRepository).delete(regularMember);
    }

    @Test
    @DisplayName("removeMember throws when member is missing")
    void removeMember_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByMemberId("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.removeMember(TOKEN, "ghost"));
        verify(userRepository, never()).delete(any(Member.class));
    }

    @Test
    @DisplayName("removeMember refuses to delete another system admin")
    void removeMember_refusesAdminTarget() {
        Member otherAdmin = new System_admin(new Member("a-2", "other-admin", "h"), "System");
        when(userRepository.findByUsername("other-admin")).thenReturn(Optional.of(otherAdmin));

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.removeMember(TOKEN, "other-admin"));
        verify(userRepository, never()).delete(any(Member.class));
    }

    @Test
    @DisplayName("removeMember rejects blank identifier")
    void removeMember_rejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.removeMember(TOKEN, "   "));
    }

    // ── queue management ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllQueues delegates to repository when admin")
    void getAllQueues_delegates() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(waitingQueueRepository.findAll()).thenReturn(List.of(q));

        List<WaitingQueue> all = systemAdminService.getAllQueues(TOKEN);

        assertEquals(1, all.size());
        assertSame(q, all.get(0));
    }

    @Test
    @DisplayName("increaseQueueFlow looks up, mutates, saves")
    void increaseQueueFlow_success() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(waitingQueueRepository.findById(7L)).thenReturn(Optional.of(q));
        when(waitingQueueRepository.save(q)).thenReturn(q);

        WaitingQueue out = systemAdminService.increaseQueueFlow(TOKEN, 7L, 3);

        verify(q).increaseFlow(3);
        verify(waitingQueueRepository).save(q);
        assertSame(q, out);
    }

    @Test
    @DisplayName("increaseQueueFlow rejects zero or negative amounts")
    void increaseQueueFlow_rejectsBadAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.increaseQueueFlow(TOKEN, 1L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.increaseQueueFlow(TOKEN, 1L, -5));
    }

    @Test
    @DisplayName("increaseQueueFlow throws when queue is missing")
    void increaseQueueFlow_queueNotFound() {
        when(waitingQueueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.increaseQueueFlow(TOKEN, 99L, 5));
    }

    @Test
    @DisplayName("decreaseQueueFlow looks up, mutates, saves")
    void decreaseQueueFlow_success() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(waitingQueueRepository.findById(7L)).thenReturn(Optional.of(q));
        when(waitingQueueRepository.save(q)).thenReturn(q);

        systemAdminService.decreaseQueueFlow(TOKEN, 7L, 2);

        verify(q).decreaseFlow(2);
        verify(waitingQueueRepository).save(q);
    }

    @Test
    @DisplayName("decreaseQueueFlow rejects zero amount")
    void decreaseQueueFlow_rejectsBadAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.decreaseQueueFlow(TOKEN, 1L, 0));
    }

    @Test
    @DisplayName("decreaseQueueFlow throws when queue is missing")
    void decreaseQueueFlow_queueNotFound() {
        when(waitingQueueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.decreaseQueueFlow(TOKEN, 99L, 5));
    }

    @Test
    @DisplayName("clearQueue empties the queue and saves it")
    void clearQueue_success() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(waitingQueueRepository.findById(7L)).thenReturn(Optional.of(q));

        systemAdminService.clearQueue(TOKEN, 7L);

        verify(q).clearQueue();
        verify(waitingQueueRepository).save(q);
    }

    @Test
    @DisplayName("clearQueue throws when queue is missing")
    void clearQueue_queueNotFound() {
        when(waitingQueueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.clearQueue(TOKEN, 99L));
    }

    // ── purchase lookups ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPurchasesByEvent rejects null event id")
    void getPurchasesByEvent_rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.getPurchasesByEvent(TOKEN, null));
        verify(purchaseRepository, never()).findByEventId(any());
    }

    @Test
    @DisplayName("getPurchasesByEvent calls repository with given id")
    void getPurchasesByEvent_callsRepo() {
        UUID eventId = UUID.randomUUID();
        when(purchaseRepository.findByEventId(eventId)).thenReturn(List.of());

        var result = systemAdminService.getPurchasesByEvent(TOKEN, eventId);

        assertNotNull(result);
        verify(purchaseRepository).findByEventId(eventId);
    }

    @Test
    @DisplayName("getPurchasesByBuyer rejects blank identifier")
    void getPurchasesByBuyer_rejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.getPurchasesByBuyer(TOKEN, ""));
    }

    @Test
    @DisplayName("getPurchasesByBuyer rejects unknown buyer")
    void getPurchasesByBuyer_unknownBuyer() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByMemberId("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.getPurchasesByBuyer(TOKEN, "ghost"));
    }

    @Test
    @DisplayName("getPurchasesByBuyer resolves member then queries repository")
    void getPurchasesByBuyer_success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(regularMember));
        when(purchaseRepository.findByBuyerId(MEMBER_ID)).thenReturn(List.of());

        var result = systemAdminService.getPurchasesByBuyer(TOKEN, "alice");

        assertNotNull(result);
        verify(purchaseRepository).findByBuyerId(MEMBER_ID);
    }

    // ── complaint admin handlers ────────────────────────────────────────────

    @Test
    @DisplayName("getAllComplaints delegates to ComplaintService")
    void getAllComplaints_delegates() {
        when(complaintService.getUserComplaints(TOKEN)).thenReturn(List.of());

        systemAdminService.getAllComplaints(TOKEN);

        verify(complaintService).getUserComplaints(TOKEN);
    }

    @Test
    @DisplayName("getAllSystemComplaintst returns all complaints as DTOs")
    void getAllSystemComplaintst_mapsAll() {
        Complaint c = new Complaint(MEMBER_ID, "subj", "desc", "ORDER", "o-1");
        when(complaintRepository.findAll()).thenReturn(List.of(c));

        List<ComplaintDTO> dtos = systemAdminService.getAllSystemComplaintst(TOKEN);

        assertEquals(1, dtos.size());
        assertEquals("subj", dtos.get(0).getSubject());
    }

    @Test
    @DisplayName("resolveComplaint notifies the reporter and delegates persistence")
    void resolveComplaint_success() {
        // Reporter member id must be a valid UUID for the production code path
        // (it calls UUID.fromString on the reporter id before notifying).
        String reporterUuid = UUID.randomUUID().toString();
        Complaint c = new Complaint(reporterUuid, "subj", "desc", "ORDER", "o-1");
        Member reporter = new Member(reporterUuid, "reporter", "hash");

        when(complaintRepository.findById(c.getComplaintId())).thenReturn(Optional.of(c));
        when(userRepository.findByMemberId(reporterUuid)).thenReturn(reporter);

        systemAdminService.resolveComplaint(TOKEN, c.getComplaintId(), "Refunded");

        verify(notificationService).notifyComplaintResolved(reporterUuid, c.getComplaintId().toString());
        verify(complaintService).resolveComplaint(TOKEN, c.getComplaintId(), "Refunded");
    }

    @Test
    @DisplayName("resolveComplaint throws when complaint id is unknown")
    void resolveComplaint_notFound() {
        UUID missing = UUID.randomUUID();
        when(complaintRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.resolveComplaint(TOKEN, missing, "any"));
        verifyNoInteractions(notificationService);
    }

    // ── getMemberIdByUsername ───────────────────────────────────────────────

    @Test
    @DisplayName("getMemberIdByUsername returns the persisted member id")
    void getMemberIdByUsername_success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(regularMember));

        assertEquals(MEMBER_ID, systemAdminService.getMemberIdByUsername(TOKEN, "alice"));
    }

    @Test
    @DisplayName("getMemberIdByUsername rejects blank input")
    void getMemberIdByUsername_blank() {
        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.getMemberIdByUsername(TOKEN, "  "));
    }

    @Test
    @DisplayName("getMemberIdByUsername rejects unknown user")
    void getMemberIdByUsername_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.getMemberIdByUsername(TOKEN, "ghost"));
    }
}
