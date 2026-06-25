package com.sdnah.Ticket_Management_System_.Event.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

/**
 * Integration tests for {@link EventService#updateShowSeating} against a real
 * Spring context and the in-memory H2 database. Verifies the force-resize
 * behaviour end-to-end: growing preserves all bookings, shrinking keeps the
 * bookings whose seats still fit and cancels the ones whose seats are dropped
 * (without violating the {@code seat_id} foreign key).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Event Module — Seating Resize Integration Tests")
class EventSeatingResizeIntegrationTest {

    private static final String OWNER_ID = "owner-seat-1";
    private static final String MANAGER_ID = OWNER_ID;
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private EventService eventService;
    @Autowired private IEventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;

    private UUID eventId;
    private UUID showId;

    @BeforeEach
    void setUp() {
        cleanDb();

        Event event = eventService.createEvent(
                new EventDto(null, "Resize Event", null, show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID, OWNER_ID);
        eventId = event.getEventId();

        show s = new show(eventId, "Resize Show", "desc", "singer", new Date());
        s.setSeatedPrice(new BigDecimal("50.00"));
        s.setAreas(List.of(buildSeated(2, 2, 3))); // 2 blocks × 2 rows × 3 seats = 12 seats
        eventService.addShowToEvent(eventId, s, MANAGER_ID);
        showId = s.getShowid();
    }

    // This is a committing (non-transactional) @SpringBootTest sharing the
    // JVM-wide in-memory H2 DB. Tear down afterwards too, otherwise leftover
    // tickets keep referencing seats and the next class's eventRepository
    // .deleteAll() cascade trips the seat_id foreign key.
    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        ticketRepository.deleteAll();       // tickets reference seats/areas — delete first
        eventRepository.deleteAll();        // cascades to shows → areas → seats
        notificationRepository.deleteAll();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Growing the layout keeps every booking and adds the new seats")
    void growingKeepsBookingsAndAddsSeats() {
        bookSeatAt(0, 0, 0);
        assertEquals(1, lockedTicketCount());

        eventService.updateShowSeating(eventId, showId, 0, 3, 3, 4, MANAGER_ID); // 36 seats

        assertArrayEquals(new int[] { 0, 3, 3, 4 }, eventService.getShowSeatingDims(eventId, showId));
        assertEquals(36, totalSeats());
        assertEquals(1, lockedTicketCount(), "booking must survive a grow");
    }

    @Test
    @DisplayName("Shrinking keeps a booking whose seat still fits the new layout")
    void shrinkingKeepsInRangeBooking() {
        bookSeatAt(0, 0, 0); // top-left seat survives a 1×1×1 grid

        eventService.updateShowSeating(eventId, showId, 0, 1, 1, 1, MANAGER_ID);

        assertArrayEquals(new int[] { 0, 1, 1, 1 }, eventService.getShowSeatingDims(eventId, showId));
        assertEquals(1, totalSeats());
        assertEquals(1, lockedTicketCount(), "in-range booking must survive a shrink");
        assertEquals(0, notificationCount(), "no notification when the seat is kept");
    }

    @Test
    @DisplayName("Shrinking cancels a booking whose seat is dropped and notifies the holder")
    void shrinkingCancelsOutOfRangeBooking() {
        bookSeatAt(1, 1, 2); // last seat of last row of last block — outside a 1×1×1 grid

        eventService.updateShowSeating(eventId, showId, 0, 1, 1, 1, MANAGER_ID);

        assertArrayEquals(new int[] { 0, 1, 1, 1 }, eventService.getShowSeatingDims(eventId, showId));
        assertEquals(1, totalSeats());
        assertEquals(0, lockedTicketCount(), "dropped seat's booking must be cancelled");
        assertEquals(1, notificationCount(), "holder must be notified their seat was removed");
    }

    private long notificationCount() {
        return notificationService.getNotificationsForUser(USER_ID.toString()).size();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Books (locks in cart) the seat at the given positional indices. */
    private void bookSeatAt(int blockIdx, int rowIdx, int seatIdx) {
        show full = eventService.loadShowFully(eventId, showId);
        SeatedArea seated = (SeatedArea) full.getAreas().stream()
                .filter(a -> a instanceof SeatedArea).findFirst().orElseThrow();
        Block block = seated.getBlocks()[blockIdx];
        Row row = block.getRows().get(rowIdx);
        Seat seat = row.getSeats().get(seatIdx);
        eventService.reserveSeat(eventId, showId, seated.getId(), seat.getId(), USER_ID);
    }

    private long lockedTicketCount() {
        return ticketRepository.findByShowId(showId).stream()
                .filter(t -> t.getStatus() == ticket.TicketStatus.LOCKED_IN_CART)
                .count();
    }

    private int totalSeats() {
        show full = eventService.loadShowFully(eventId, showId);
        int total = 0;
        for (Area a : full.getAreas()) {
            if (a instanceof SeatedArea seated) {
                for (Block b : seated.getBlocks())
                    for (Row r : b.getRows())
                        total += r.getSeats().size();
            }
        }
        return total;
    }

    private static SeatedArea buildSeated(int numBlocks, int rowsPerBlock, int seatsPerRow) {
        SeatedArea seated = new SeatedArea("Seated Area", numBlocks);
        List<Block> blocks = new ArrayList<>();
        for (int b = 0; b < numBlocks; b++) {
            Block block = new Block(0, String.valueOf((char) ('A' + b)), rowsPerBlock, seated);
            block.setId(null);
            List<Row> rows = new ArrayList<>();
            for (int r = 0; r < rowsPerBlock; r++) {
                Row row = new Row(0, String.valueOf(r + 1), seatsPerRow, block);
                row.setId(null);
                List<Seat> seats = new ArrayList<>();
                for (int s = 1; s <= seatsPerRow; s++) {
                    Seat seat = new Seat(0, String.valueOf(s), row);
                    seat.setId(null);
                    seats.add(seat);
                }
                row.setSeats(seats);
                rows.add(row);
            }
            block.setRows(rows);
            blocks.add(block);
        }
        seated.setBlocks(blocks);
        return seated;
    }
}
