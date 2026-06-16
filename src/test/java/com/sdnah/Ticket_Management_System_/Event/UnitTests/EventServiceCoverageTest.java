package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

/**
 * Coverage tests for EventService's search methods — none are exercised by the
 * existing Acceptance / Integration suites. They are pure filter pipelines
 * over the repository result, so mock-based unit tests are appropriate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService — Search Coverage Tests")
class EventServiceCoverageTest {

    @Mock private IEventRepository eventRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private NotificationService notificationService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private TicketRepository ticketRepository;
    @Mock private com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository companyRepository;

    private EventService service;

    private Event eventA; // "Rock Night" at "MSG", description "loud and proud",
                          // CONCERT, starts Day 1
    private Event eventB; // "Jazz Evening" at "Blue Note", description "cool",
                          // CONCERT, starts Day 5
    private Event eventC; // "Comedy Hour" at "MSG", description "stand-up",
                          // STAND_UP, starts Day 10

    private static final Date DAY_1 = new Date(0L);
    private static final Date DAY_5 = new Date(TimeUnit.DAYS.toMillis(5));
    private static final Date DAY_10 = new Date(TimeUnit.DAYS.toMillis(10));

    @BeforeEach
    void setUp() {
        service = new EventService(
                eventRepository, purchaseRepository, notificationService,
                new KeyedLock(), transactionTemplate, ticketRepository,
                companyRepository);

        eventA = mockEvent("Rock Night", "Loud and proud", "MSG",
                show_type.PERFORMANCE, DAY_1, DAY_5, "alice", 5);
        eventB = mockEvent("Jazz Evening", "Cool grooves", "Blue Note",
                show_type.PERFORMANCE, DAY_5, DAY_10, "alice", 3);
        eventC = mockEvent("Comedy Hour", "Stand-up special", "MSG",
                show_type.FESTIVAL, DAY_10, null, "carol", 4);
    }

    private Event mockEvent(String name, String description, String venue,
                            show_type type, Date start, Date end,
                            String reviewerHandle, int rating) {
        Event e = mock(Event.class);
        lenient().when(e.getEventId()).thenReturn(UUID.randomUUID());
        lenient().when(e.getName()).thenReturn(name);
        lenient().when(e.getDescription()).thenReturn(description);
        lenient().when(e.getVenue()).thenReturn(venue);
        lenient().when(e.getEventType()).thenReturn(type);
        lenient().when(e.getStartDate()).thenReturn(start);
        lenient().when(e.getEndDate()).thenReturn(end);
        lenient().when(e.getReviews()).thenReturn(Map.of(UUID.randomUUID(), rating));
        return e;
    }

    // ── searchEventByName ───────────────────────────────────────────────────

    @Test
    @DisplayName("searchEventByName matches case-insensitively on the name field")
    void searchEventByName_matchesSubstring() {
        when(eventRepository.searchEventsByName("rock")).thenReturn(List.of(eventA));

        List<EventDto> hits = service.searchEventByName("rock");

        assertEquals(1, hits.size());
        assertEquals("Rock Night", hits.get(0).name);
    }

    @Test
    @DisplayName("searchEventByName returns empty for null/blank input")
    void searchEventByName_blank() {
        assertTrue(service.searchEventByName(null).isEmpty());
        assertTrue(service.searchEventByName("  ").isEmpty());
    }

    // ── searchEventsByDescription ───────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByDescription matches substring case-insensitively")
    void searchEventsByDescription_match() {
        when(eventRepository.searchEventsByDescription("stand")).thenReturn(List.of(eventC));

        List<EventDto> hits = service.searchEventsByDescription("stand");

        assertEquals(1, hits.size());
        assertEquals("Comedy Hour", hits.get(0).name);
    }

    @Test
    @DisplayName("searchEventsByDescription returns empty for blank input")
    void searchEventsByDescription_blank() {
        assertTrue(service.searchEventsByDescription("").isEmpty());
    }

    // ── searchEventsByCategory ──────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByCategory keeps only matching event type")
    void searchEventsByCategory_match() {
        when(eventRepository.searchEventsByType(show_type.PERFORMANCE)).thenReturn(List.of(eventA, eventC));

        List<EventDto> hits = service.searchEventsByCategory(show_type.PERFORMANCE);

        assertEquals(2, hits.size());
    }

    @Test
    @DisplayName("searchEventsByCategory returns empty for null category")
    void searchEventsByCategory_null() {
        assertTrue(service.searchEventsByCategory(null).isEmpty());
    }

    // ── searchEventsByStartDate ─────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByStartDate keeps events starting on/after the given date")
    void searchEventsByStartDate_filters() {
        when(eventRepository.getEventsByFilter(null, null, DAY_5, null)).thenReturn(List.of(eventB, eventC));

        List<EventDto> hits = service.searchEventsByStartDate(DAY_5);

        assertEquals(2, hits.size()); // eventB (starts DAY_5), eventC (DAY_10)
    }

    @Test
    @DisplayName("searchEventsByStartDate returns empty for null input")
    void searchEventsByStartDate_null() {
        assertTrue(service.searchEventsByStartDate(null).isEmpty());
    }

    // ── searchEventsByEndDate ───────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByEndDate keeps events ending on/before the given date and excludes events without endDate")
    void searchEventsByEndDate_filters() {
        when(eventRepository.getEventsByFilter(null, null, null, DAY_5)).thenReturn(List.of(eventA));

        List<EventDto> hits = service.searchEventsByEndDate(DAY_5);

        // eventA ends DAY_5 → in; eventB ends DAY_10 → out; eventC has no endDate → out
        assertEquals(1, hits.size());
        assertEquals("Rock Night", hits.get(0).name);
    }

    @Test
    @DisplayName("searchEventsByEndDate returns empty for null input")
    void searchEventsByEndDate_null() {
        assertTrue(service.searchEventsByEndDate(null).isEmpty());
    }

    // ── searchEventsByDateRange ─────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByDateRange filters by start in [from, to]")
    void searchEventsByDateRange_range() {
        when(eventRepository.getEventsByFilter(null, null, DAY_1, DAY_5)).thenReturn(List.of(eventA, eventB));

        List<EventDto> hits = service.searchEventsByDateRange(DAY_1, DAY_5);

        // eventA starts DAY_1 → in; eventB starts DAY_5 → in; eventC starts DAY_10 → out
        assertEquals(2, hits.size());
    }

    @Test
    @DisplayName("searchEventsByDateRange handles open-ended ranges (null fromDate/toDate)")
    void searchEventsByDateRange_openEnded() {
        when(eventRepository.getEventsByFilter(null, null, null, null)).thenReturn(List.of(eventA, eventB, eventC));
        when(eventRepository.getEventsByFilter(null, null, DAY_5, null)).thenReturn(List.of(eventB, eventC));
        when(eventRepository.getEventsByFilter(null, null, null, DAY_5)).thenReturn(List.of(eventA, eventB));

        // All events with a start date qualify when both bounds are null.
        assertEquals(3, service.searchEventsByDateRange(null, null).size());

        // Only events starting on/after DAY_5 (eventB, eventC).
        assertEquals(2, service.searchEventsByDateRange(DAY_5, null).size());

        // Only events starting on/before DAY_5 (eventA, eventB).
        assertEquals(2, service.searchEventsByDateRange(null, DAY_5).size());
    }

    // ── searchEventsByVenue ─────────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByVenue matches substring case-insensitively")
    void searchEventsByVenue_match() {
        when(eventRepository.searchEventsByVenue("msg")).thenReturn(List.of(eventA, eventC));

        List<EventDto> hits = service.searchEventsByVenue("msg");

        assertEquals(2, hits.size()); // eventA + eventC are at MSG
    }

    @Test
    @DisplayName("searchEventsByVenue returns empty for blank input")
    void searchEventsByVenue_blank() {
        assertTrue(service.searchEventsByVenue(null).isEmpty());
        assertTrue(service.searchEventsByVenue("").isEmpty());
    }

    // ── searchEventsByMinRating ─────────────────────────────────────────────

    @Test
    @DisplayName("searchEventsByMinRating uses average review rating")
    void searchEventsByMinRating_filters() {
        when(eventRepository.findAllEvents()).thenReturn(List.of(eventA, eventB, eventC));

        List<EventDto> hits = service.searchEventsByMinRating(4.0);

        // eventA rating=5 in, eventC rating=4 in, eventB rating=3 out.
        assertEquals(2, hits.size());
    }

    @Test
    @DisplayName("searchEventsByMinRating treats events with no reviews as rating 0")
    void searchEventsByMinRating_noReviews() {
        Event noReviews = mock(Event.class);
        when(noReviews.getReviews()).thenReturn(Map.of());
        when(eventRepository.findAllEvents()).thenReturn(List.of(noReviews));

        // A min rating of 0 keeps it; any positive cutoff excludes it.
        assertEquals(1, service.searchEventsByMinRating(0.0).size());
        assertEquals(0, service.searchEventsByMinRating(0.1).size());
    }

    // ── searchEventsByKeyword (uses repository projection method) ───────────

    @Test
    @DisplayName("searchEventsByKeyword delegates to repository's case-insensitive name lookup")
    void searchEventsByKeyword_delegates() {
        when(eventRepository.findByNameContainingIgnoreCase("rock"))
                .thenReturn(List.of(eventA));

        List<EventDto> hits = service.searchEventsByKeyword("rock");

        assertEquals(1, hits.size());
        verify(eventRepository).findByNameContainingIgnoreCase("rock");
    }

    @Test
    @DisplayName("searchEventsByKeyword returns empty for blank input without hitting the repository")
    void searchEventsByKeyword_blank() {
        assertTrue(service.searchEventsByKeyword(null).isEmpty());
        assertTrue(service.searchEventsByKeyword("   ").isEmpty());

        verify(eventRepository, never()).findByNameContainingIgnoreCase(anyString());
    }
}
