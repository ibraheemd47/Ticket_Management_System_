package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

/**
 * Coverage-focused unit tests for TicketService. The existing integration test
 * exercises the happy path through real JPA; this file fills in the gaps:
 * seat availability map, area / standing usage counts, and the "ticket not
 * found" short-circuit branches.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService — Coverage Tests")
class TicketServiceCoverageTest {

    @Mock private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    // ── not-found short-circuit branches ─────────────────────────────────────

    @Test
    @DisplayName("lockTicketForUser returns false when ticket id is unknown")
    void lockTicketForUser_unknownTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertFalse(ticketService.lockTicketForUser(ticketId, userId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPurchase returns false when ticket id is unknown")
    void confirmPurchase_unknownTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertFalse(ticketService.confirmPurchase(ticketId, userId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("scanTicketAtDoor returns false when ticket id is unknown")
    void scanTicketAtDoor_unknownTicket() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertFalse(ticketService.scanTicketAtDoor(ticketId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("releaseTicket on a missing id is a no-op (no exception, no save)")
    void releaseTicket_unknownTicket() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ticketService.releaseTicket(ticketId));
        verify(ticketRepository, never()).save(any());
    }

    // ── getSeatAvailability ─────────────────────────────────────────────────

    @Test
    @DisplayName("getSeatAvailability builds a seat->isAvailable map, skipping standing tickets")
    void getSeatAvailability_buildsMap() {
        UUID showId = UUID.randomUUID();

        Seat s1 = mock(Seat.class);
        when(s1.getId()).thenReturn(1L);
        Seat s2 = mock(Seat.class);
        when(s2.getId()).thenReturn(2L);

        ticket available = mock(ticket.class);
        when(available.getSeat()).thenReturn(s1);
        when(available.getStatus()).thenReturn(ticket.TicketStatus.AVAILABLE);

        ticket locked = mock(ticket.class);
        when(locked.getSeat()).thenReturn(s2);
        when(locked.getStatus()).thenReturn(ticket.TicketStatus.LOCKED_IN_CART);

        ticket standing = mock(ticket.class);
        when(standing.getSeat()).thenReturn(null); // standing ticket filtered out

        when(ticketRepository.findByShowId(showId))
                .thenReturn(List.of(available, locked, standing));

        Map<Long, Boolean> result = ticketService.getSeatAvailability(showId);

        assertEquals(2, result.size());
        assertTrue(result.get(1L));   // seat 1 has an AVAILABLE ticket
        assertFalse(result.get(2L));  // seat 2's ticket is locked
    }

    @Test
    @DisplayName("getSeatAvailability merges duplicate seat ids with OR semantics")
    void getSeatAvailability_mergesDuplicates() {
        UUID showId = UUID.randomUUID();

        Seat s = mock(Seat.class);
        when(s.getId()).thenReturn(5L);

        ticket sold = mock(ticket.class);
        when(sold.getSeat()).thenReturn(s);
        when(sold.getStatus()).thenReturn(ticket.TicketStatus.PURCHASED);

        ticket reAvailable = mock(ticket.class);
        when(reAvailable.getSeat()).thenReturn(s);
        when(reAvailable.getStatus()).thenReturn(ticket.TicketStatus.AVAILABLE);

        when(ticketRepository.findByShowId(showId))
                .thenReturn(List.of(sold, reAvailable));

        Map<Long, Boolean> result = ticketService.getSeatAvailability(showId);

        // Merge function is `a || b` -> any AVAILABLE wins.
        assertEquals(1, result.size());
        assertTrue(result.get(5L));
    }

    // ── getStandingUsedCount / getAreaUsedCount / getStandingAvailableCount ──

    @Test
    @DisplayName("getStandingUsedCount delegates to repository with seat-is-null filter")
    void getStandingUsedCount_delegates() {
        UUID showId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(ticketRepository.countByShowIdAndArea_IdAndSeatIsNullAndStatusIn(
                eq(showId), eq(areaId), any())).thenReturn(7L);

        assertEquals(7, ticketService.getStandingUsedCount(showId, areaId));

        verify(ticketRepository).countByShowIdAndArea_IdAndSeatIsNullAndStatusIn(
                eq(showId), eq(areaId), eq(Set.of(
                        ticket.TicketStatus.LOCKED_IN_CART,
                        ticket.TicketStatus.PURCHASED,
                        ticket.TicketStatus.SCANNED)));
    }

    @Test
    @DisplayName("getAreaUsedCount delegates to repository without the null-seat filter")
    void getAreaUsedCount_delegates() {
        UUID showId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(ticketRepository.countByShowIdAndArea_IdAndStatusIn(
                eq(showId), eq(areaId), any())).thenReturn(12L);

        assertEquals(12, ticketService.getAreaUsedCount(showId, areaId));
    }

    @Test
    @DisplayName("getStandingAvailableCount = max(0, capacity - used)")
    void getStandingAvailableCount_doesArithmetic() {
        UUID showId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(ticketRepository.countByShowIdAndArea_IdAndSeatIsNullAndStatusIn(
                eq(showId), eq(areaId), any())).thenReturn(3L);

        assertEquals(7, ticketService.getStandingAvailableCount(showId, areaId, 10));
    }

    @Test
    @DisplayName("getStandingAvailableCount clamps to 0 when oversold")
    void getStandingAvailableCount_clampsAtZero() {
        UUID showId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(ticketRepository.countByShowIdAndArea_IdAndSeatIsNullAndStatusIn(
                eq(showId), eq(areaId), any())).thenReturn(15L);

        assertEquals(0, ticketService.getStandingAvailableCount(showId, areaId, 10));
    }

    // ── getTicketsByOwner ───────────────────────────────────────────────────

    @Test
    @DisplayName("getTicketsByOwner just forwards to the repository")
    void getTicketsByOwner_forwards() {
        UUID ownerId = UUID.randomUUID();
        ticket t = mock(ticket.class);
        when(ticketRepository.findByOwnerId(ownerId)).thenReturn(List.of(t));

        assertEquals(1, ticketService.getTicketsByOwner(ownerId).size());
    }
}
