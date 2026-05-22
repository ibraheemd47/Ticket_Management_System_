package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.TicketGenerator;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;

/**
 * Unit tests covering the seating tree (StandingArea / SeatedArea / Block /
 * Row / Seat), the show value object, and the TicketGenerator that walks the
 * tree to materialize ticket instances.
 */
class EventDomainTest {

    // ── StandingArea ────────────────────────────────────────────────────────

    @Test
    @DisplayName("StandingArea: add tickets up to maxCapacity, then refuse")
    void standingArea_addsUntilFull_thenRejects() {
        StandingArea area = new StandingArea("GA", 2);
        ticket t1 = mockTicket(area);
        ticket t2 = mockTicket(area);
        ticket t3 = mockTicket(area);

        assertThat(area.isFull()).isFalse();
        assertThat(area.getCurrentCapacity()).isZero();

        assertThat(area.addTicket(t1)).isTrue();
        assertThat(area.addTicket(t2)).isTrue();
        assertThat(area.isFull()).isTrue();
        assertThat(area.getCurrentCapacity()).isEqualTo(2);
        assertThat(area.addTicket(t3)).isFalse();        // refused — full
        assertThat(area.getMaxCapacity()).isEqualTo(2);
    }

    @Test
    @DisplayName("StandingArea: removeTicket returns true on hit, false on miss")
    void standingArea_removeTicket() {
        StandingArea area = new StandingArea("GA", 5);
        ticket t1 = mockTicket(area);
        ticket t2 = mockTicket(area);
        area.addTicket(t1);

        assertThat(area.removeTicket(t1)).isTrue();
        assertThat(area.removeTicket(t2)).isFalse();
        assertThat(area.getCurrentCapacity()).isZero();
    }

    @Test
    @DisplayName("StandingArea: setMaxCapacity replaces capacity, areaMap exposes underlying list")
    void standingArea_settersAndGetters() {
        StandingArea area = new StandingArea("GA", 3);
        area.setMaxCapacity(10);
        assertThat(area.getMaxCapacity()).isEqualTo(10);
        assertThat(area.getAreaMap()).isEmpty();
        assertThat(area.getId()).isNotNull();
    }

    // ── SeatedArea + Block + Row + Seat ─────────────────────────────────────

    @Test
    @DisplayName("SeatedArea: stores blocks and exposes them as an array")
    void seatedArea_storesBlocks() {
        SeatedArea seated = new SeatedArea("Hall A", 2);
        Block b1 = new Block(1L, "Left",  1, seated);
        Block b2 = new Block(2L, "Right", 1, seated);
        seated.setBlocks(List.of(b1, b2));

        assertThat(seated.getNumberofBlocks()).isEqualTo(2);
        assertThat(seated.getBlocks()).containsExactly(b1, b2);

        seated.setBlocks(null);                  // null-safe path
        assertThat(seated.getBlocks()).isEmpty();
    }

    @Test
    @DisplayName("Block: roundtrip getters/setters")
    void block_settersGetters() {
        SeatedArea seated = new SeatedArea("Hall", 1);
        Block b = new Block(7L, "VIP", 3, seated);

        assertThat(b.getId()).isEqualTo(7L);
        assertThat(b.getBlockIdentifier()).isEqualTo("VIP");
        assertThat(b.getSeatedArea()).isSameAs(seated);

        b.setId(99L);
        b.setBlockIdentifier("Renamed");
        assertThat(b.getId()).isEqualTo(99L);
        assertThat(b.getBlockIdentifier()).isEqualTo("Renamed");

        Row r = new Row(1L, "A", 0, b);
        b.setRows(List.of(r));
        assertThat(b.getRows()).containsExactly(r);
    }

    @Test
    @DisplayName("Row: ctor + setters")
    void row_settersGetters() {
        SeatedArea seated = new SeatedArea("Hall", 1);
        Block b = new Block(1L, "A", 1, seated);
        Row r = new Row(5L, "Row-5", 10, b);

        assertThat(r.getId()).isEqualTo(5L);
        assertThat(r.getRowNumber()).isEqualTo("Row-5");
        assertThat(r.getBlock()).isSameAs(b);
        assertThat(r.getSeats()).isEmpty();

        r.setId(50L);
        r.setRowNumber("Row-50");
        Seat s = new Seat(1L, "A1", r);
        r.setSeats(List.of(s));
        r.setBlock(b);

        assertThat(r.getId()).isEqualTo(50L);
        assertThat(r.getRowNumber()).isEqualTo("Row-50");
        assertThat(r.getSeats()).containsExactly(s);
    }

    @Test
    @DisplayName("Seat: Fillseat flips state once; second call throws")
    void seat_fillSeat() {
        SeatedArea seated = new SeatedArea("Hall", 1);
        Block b = new Block(1L, "A", 1, seated);
        Row r = new Row(1L, "Row-1", 5, b);
        Seat s = new Seat(1L, "A1", r);

        s.setSeatNumber("A1-renamed");
        assertThat(s.getSeatNumber()).isEqualTo("A1-renamed");
        assertThat(s.getRow()).isSameAs(r);

        assertThat(s.Fillseat()).isTrue();
        assertThatThrownBy(s::Fillseat).isInstanceOf(IllegalStateException.class);

        s.setId(99L);
        assertThat(s.getId()).isEqualTo(99L);
        Row r2 = new Row(2L, "Row-2", 5, b);
        s.setRow(r2);
        assertThat(s.getRow()).isSameAs(r2);
    }

    // ── show ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("show: ctor populates all fields and equality follows showid")
    void show_ctorAndEquality() {
        UUID eventId = UUID.randomUUID();
        Date when = new Date();
        show s = new show(eventId, "Coldplay", "Tour 26", "Coldplay", when);

        assertThat(s.getShowid()).isNotNull();
        assertThat(s.getEventId()).isEqualTo(eventId);
        assertThat(s.getShowDate()).isEqualTo(when);
        assertThat(s.getName()).isEqualTo("Coldplay");
        assertThat(s.getDescription()).isEqualTo("Tour 26");
        assertThat(s.getSinger()).isEqualTo("Coldplay");

        // setters
        s.setName("Renamed");
        s.setDescription("Updated");
        s.setSinger("New singer");
        Date newDate = new Date(when.getTime() + 86_400_000L);
        s.setShowDate(newDate);

        UUID newEvent = UUID.randomUUID();
        s.setEventId(newEvent);

        assertThat(s.getName()).isEqualTo("Renamed");
        assertThat(s.getDescription()).isEqualTo("Updated");
        assertThat(s.getSinger()).isEqualTo("New singer");
        assertThat(s.getShowDate()).isEqualTo(newDate);
        assertThat(s.getEventId()).isEqualTo(newEvent);

        // equality is based on showid
        assertThat(s).isEqualTo(s).hasSameHashCodeAs(s);
        assertThat(s).isNotEqualTo("not a show");
        assertThat(s).isNotEqualTo(new show(eventId, "x", "x", "x", when));
    }

    @Test
    @DisplayName("show: areas list is mutable via setter")
    void show_areasSetter() {
        show s = new show(UUID.randomUUID(), "x", "x", "x", new Date());
        StandingArea ga = new StandingArea("GA", 50);
        s.setAreas(List.of(ga));
        assertThat(s.getAreas()).containsExactly(ga);
    }

    // ── TicketGenerator ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TicketGenerator: standing area produces one ticket per maxCapacity")
    void ticketGenerator_standingArea() {
        StandingArea ga = new StandingArea("GA", 3);

        show s = new show(UUID.randomUUID(), "x", "x", "x", new Date());
        s.setAreas(List.of(ga));

        List<ticket> generated =
                new TicketGenerator().generateAllTicketsForShow(s, new BigDecimal("25.00"));

        assertThat(generated).hasSize(3);
        assertThat(generated).allMatch(t -> t.getShowId().equals(s.getShowid()));
        assertThat(generated).allMatch(t -> t.getPrice().compareTo(new BigDecimal("25.00")) == 0);
    }

    @Test
    @DisplayName("TicketGenerator: seated area produces one ticket per seat across blocks/rows")
    void ticketGenerator_seatedArea() {
        SeatedArea seated = new SeatedArea("Hall", 1);
        Block b = new Block(1L, "VIP", 2, seated);
        Row r1 = new Row(1L, "Row-1", 2, b);
        Row r2 = new Row(2L, "Row-2", 1, b);
        r1.setSeats(List.of(
                new Seat(1L, "A1", r1),
                new Seat(2L, "A2", r1)));
        r2.setSeats(List.of(new Seat(3L, "B1", r2)));
        b.setRows(List.of(r1, r2));
        seated.setBlocks(List.of(b));

        show s = new show(UUID.randomUUID(), "x", "x", "x", new Date());
        s.setAreas(List.of(seated));

        List<ticket> out =
                new TicketGenerator().generateAllTicketsForShow(s, new BigDecimal("80"));

        assertThat(out).hasSize(3); // 2 + 1
        assertThat(out).allMatch(t -> t.getShowId().equals(s.getShowid()));
        assertThat(out).allMatch(t -> t.getSeat() != null);
        assertThat(out).allMatch(t -> t.getArea() != null);
    }

    @Test
    @DisplayName("TicketGenerator: empty show yields an empty list")
    void ticketGenerator_emptyShow() {
        show s = new show(UUID.randomUUID(), "x", "x", "x", new Date());
        s.setAreas(List.of());
        assertThat(new TicketGenerator().generateAllTicketsForShow(s, BigDecimal.TEN)).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static ticket mockTicket(Area area) {
        return new ticket(UUID.randomUUID(), UUID.randomUUID(), area, new Date(), BigDecimal.ONE);
    }
}
