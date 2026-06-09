package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CreateComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ComplaintStatus;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;

/**
 * Unit tests for ComplaintService. Mocks the repository + UserService; uses a
 * real {@link KeyedLock} (cheap, no I/O) so the lock-based critical sections
 * are exercised end-to-end without bringing up Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComplaintService — Unit Tests")
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserService userService;

    private ComplaintService complaintService;

    private static final String TOKEN = "valid-token";
    private static final String MEMBER_ID = "member-42";

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(complaintRepository, userService, new KeyedLock());
    }

    // ── createComplaint ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createComplaint saves and returns the new complaint id")
    void createComplaint_persistsAndReturnsId() {
        when(userService.requireMemberId(TOKEN)).thenReturn(MEMBER_ID);
        CreateComplaintDTO request = new CreateComplaintDTO(
                "Wrong seat", "I got row B not row A", "ORDER", "order-1");

        UUID id = complaintService.createComplaint(TOKEN, request);

        assertNotNull(id);
        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository).save(captor.capture());
        Complaint saved = captor.getValue();
        assertEquals(MEMBER_ID, saved.getReporterMemberId());
        assertEquals("Wrong seat", saved.getSubject());
        assertEquals("I got row B not row A", saved.getDescription());
        assertEquals("ORDER", saved.getTargetType());
        assertEquals("order-1", saved.getTargetId());
        assertEquals(ComplaintStatus.OPEN, saved.getStatus());
        assertEquals(id, saved.getComplaintId());
    }

    @Test
    @DisplayName("createComplaint throws if user is not authenticated")
    void createComplaint_rejectsAnonymousCaller() {
        when(userService.requireMemberId(TOKEN))
                .thenThrow(new RuntimeException("Invalid token"));
        CreateComplaintDTO request = new CreateComplaintDTO("s", "d", "t", "1");

        assertThrows(RuntimeException.class,
                () -> complaintService.createComplaint(TOKEN, request));
        verify(complaintRepository, never()).save(any());
    }

    // ── getUserComplaints ────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserComplaints maps repo result through ComplaintDTO")
    void getUserComplaints_returnsMappedDtos() {
        when(userService.requireMemberId(TOKEN)).thenReturn(MEMBER_ID);
        Complaint c1 = new Complaint(MEMBER_ID, "Subject 1", "Desc 1", "ORDER", "o1");
        Complaint c2 = new Complaint(MEMBER_ID, "Subject 2", "Desc 2", "EVENT", "e1");
        when(complaintRepository.findByReporterMemberId(MEMBER_ID))
                .thenReturn(List.of(c1, c2));

        List<ComplaintDTO> result = complaintService.getUserComplaints(TOKEN);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> "Subject 1".equals(d.getSubject())));
        assertTrue(result.stream().anyMatch(d -> "Subject 2".equals(d.getSubject())));
    }

    @Test
    @DisplayName("getUserComplaints returns empty list when user has no complaints")
    void getUserComplaints_emptyWhenNone() {
        when(userService.requireMemberId(TOKEN)).thenReturn(MEMBER_ID);
        when(complaintRepository.findByReporterMemberId(MEMBER_ID)).thenReturn(List.of());

        assertTrue(complaintService.getUserComplaints(TOKEN).isEmpty());
    }

    // ── getComplaintsByStatus ────────────────────────────────────────────────

    @Test
    @DisplayName("getComplaintsByStatus checks admin and returns matching complaints")
    void getComplaintsByStatus_returnsMatching() {
        Complaint c = new Complaint("other-user", "subj", "desc", "ORDER", "o1");
        when(complaintRepository.findByStatus(ComplaintStatus.OPEN))
                .thenReturn(List.of(c));

        List<ComplaintDTO> result =
                complaintService.getComplaintsByStatus(TOKEN, ComplaintStatus.OPEN);

        verify(userService).requireAdmin(TOKEN);
        assertEquals(1, result.size());
        assertEquals("subj", result.get(0).getSubject());
    }

    @Test
    @DisplayName("getComplaintsByStatus rejects non-admin caller")
    void getComplaintsByStatus_rejectsNonAdmin() {
        doThrow(new RuntimeException("Admin permission required"))
                .when(userService).requireAdmin(TOKEN);

        assertThrows(RuntimeException.class,
                () -> complaintService.getComplaintsByStatus(TOKEN, ComplaintStatus.OPEN));
        verify(complaintRepository, never()).findByStatus(any());
    }

    // ── markInProgress ───────────────────────────────────────────────────────

    @Test
    @DisplayName("markInProgress flips status and persists")
    void markInProgress_success() {
        Complaint c = new Complaint(MEMBER_ID, "s", "d", "ORDER", "o1");
        when(complaintRepository.findById(c.getComplaintId())).thenReturn(Optional.of(c));

        complaintService.markInProgress(TOKEN, c.getComplaintId());

        verify(userService).requireAdmin(TOKEN);
        assertEquals(ComplaintStatus.IN_PROGRESS, c.getStatus());
        verify(complaintRepository).save(c);
    }

    @Test
    @DisplayName("markInProgress throws when complaint is missing")
    void markInProgress_notFound() {
        UUID missing = UUID.randomUUID();
        when(complaintRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> complaintService.markInProgress(TOKEN, missing));
        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("markInProgress rejects non-admin caller")
    void markInProgress_rejectsNonAdmin() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Admin permission required"))
                .when(userService).requireAdmin(TOKEN);

        assertThrows(RuntimeException.class,
                () -> complaintService.markInProgress(TOKEN, id));
        verify(complaintRepository, never()).findById(any());
        verify(complaintRepository, never()).save(any());
    }

    // ── resolveComplaint ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveComplaint records admin response and updates status")
    void resolveComplaint_success() {
        Complaint c = new Complaint(MEMBER_ID, "s", "d", "ORDER", "o1");
        when(complaintRepository.findById(c.getComplaintId())).thenReturn(Optional.of(c));

        complaintService.resolveComplaint(TOKEN, c.getComplaintId(), "Refund processed");

        verify(userService).requireAdmin(TOKEN);
        assertEquals(ComplaintStatus.RESOLVED, c.getStatus());
        assertEquals("Refund processed", c.getAdminResponse());
        assertNotNull(c.getResolvedAt());
        verify(complaintRepository).save(c);
    }

    @Test
    @DisplayName("resolveComplaint throws when complaint is missing")
    void resolveComplaint_notFound() {
        UUID missing = UUID.randomUUID();
        when(complaintRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> complaintService.resolveComplaint(TOKEN, missing, "any"));
    }

    @Test
    @DisplayName("resolveComplaint rejects non-admin caller")
    void resolveComplaint_rejectsNonAdmin() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Admin permission required"))
                .when(userService).requireAdmin(TOKEN);

        assertThrows(RuntimeException.class,
                () -> complaintService.resolveComplaint(TOKEN, id, "x"));
        verify(complaintRepository, never()).findById(any());
    }

    // ── rejectComplaint ──────────────────────────────────────────────────────

    @Test
    @DisplayName("rejectComplaint records admin response and updates status")
    void rejectComplaint_success() {
        Complaint c = new Complaint(MEMBER_ID, "s", "d", "ORDER", "o1");
        when(complaintRepository.findById(c.getComplaintId())).thenReturn(Optional.of(c));

        complaintService.rejectComplaint(TOKEN, c.getComplaintId(), "Out of scope");

        verify(userService).requireAdmin(TOKEN);
        assertEquals(ComplaintStatus.REJECTED, c.getStatus());
        assertEquals("Out of scope", c.getAdminResponse());
        assertNotNull(c.getResolvedAt());
        verify(complaintRepository).save(c);
    }

    @Test
    @DisplayName("rejectComplaint throws when complaint is missing")
    void rejectComplaint_notFound() {
        UUID missing = UUID.randomUUID();
        when(complaintRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> complaintService.rejectComplaint(TOKEN, missing, "any"));
    }

    @Test
    @DisplayName("rejectComplaint rejects non-admin caller")
    void rejectComplaint_rejectsNonAdmin() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Admin permission required"))
                .when(userService).requireAdmin(TOKEN);

        assertThrows(RuntimeException.class,
                () -> complaintService.rejectComplaint(TOKEN, id, "x"));
        verify(complaintRepository, never()).findById(any());
    }
}
