package com.sdnah.Ticket_Management_System_.Event.AcceptanceTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Event Module — Acceptance Tests")
class EventAcceptanceTest {

    @Autowired
    private EventService eventService;

    private static final Long OWNER_ID = 1L;
    private static final Long MANAGER_ID = 2L;
    private static final Long COMPANY_ID = 10L;
    private static final Long UNAUTHORIZED = 99L;

    // -------------------------------------------------------------------------
    // UC II.4.1 — Manage Events and Ticket Inventory
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Manage Events and Ticket Inventory")
    class ManageEventsAndInventory {

        @Test
        @DisplayName("Owner creates an event — persisted and retrievable")
        void createEventPersisted() {
            EventDto dto = new EventDto(null, "Jazz Night", null, show_type.PERFORMANCE, "Tel Aviv");

            Event created = eventService.createEvent(dto, COMPANY_ID, OWNER_ID);

            assertThat(created.getEventId()).isNotNull();
            Event fetched = eventService.getEventDetails(created.getEventId());
            assertThat(fetched.getName()).isEqualTo("Jazz Night");
            assertThat(fetched.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(fetched.getOwnerId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("Manager edits event name — change persisted")
        void editEventNamePersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Original Name", null, show_type.FESTIVAL, "Haifa"), COMPANY_ID, OWNER_ID);
            eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

            eventService.editEventName(event.getEventId(), "Updated Name", MANAGER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Manager edits event type — change persisted")
        void editEventTypePersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Tech Conf", null, show_type.CONFERENCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);

            eventService.editEventType(event.getEventId(), show_type.FESTIVAL, OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getEventType())
                    .isEqualTo(show_type.FESTIVAL);
        }

        @Test
        @DisplayName("Manager edits event dates — change persisted")
        void editEventDatesPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Summer Fest", null, show_type.FESTIVAL, "Eilat"), COMPANY_ID, OWNER_ID);
            Date start = new Date();
            Date end = new Date(start.getTime() + 86_400_000L);

            eventService.editEventDates(event.getEventId(), start, end, OWNER_ID);

            Event fetched = eventService.getEventDetails(event.getEventId());
            assertThat(fetched.getStartDate()).isEqualTo(start);
            assertThat(fetched.getEndDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("Manager edits event venue — change persisted")
        void editEventVenuePersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Rock Night", null, show_type.PERFORMANCE, "TBD"), COMPANY_ID, OWNER_ID);

            eventService.editEventVenue(event.getEventId(), "Yarkon Park", OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getVenue()).isEqualTo("Yarkon Park");
        }

        @Test
        @DisplayName("Manager edits event description — change persisted")
        void editEventDescriptionPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Art Show", null, show_type.PERFORMANCE, "Jerusalem"), COMPANY_ID, OWNER_ID);

            eventService.editEventDescription(event.getEventId(), "Annual art exhibition", OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getDescription())
                    .isEqualTo("Annual art exhibition");
        }

        @Test
        @DisplayName("Owner deletes event — no longer retrievable")
        void deleteEventRemoved() {
            Event event = eventService.createEvent(
                    new EventDto(null, "To Delete", null, show_type.CONFERENCE, "TLV"), COMPANY_ID, OWNER_ID);
            UUID id = event.getEventId();

            eventService.deleteEvent(id, OWNER_ID);

            assertThatThrownBy(() -> eventService.getEventDetails(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }

        @Test
        @DisplayName("Non-owner cannot delete event — permission denied")
        void deleteEventUnauthorized() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Protected", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

            assertThatThrownBy(() -> eventService.deleteEvent(event.getEventId(), UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only the owner can delete the event");
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event name — permission denied")
        void editEventNameUnauthorized() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Safe Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

            assertThatThrownBy(() -> eventService.editEventName(event.getEventId(), "Hacked", UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event name");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.4.1 — Manager Assignment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Manager Assignment")
    class ManagerAssignment {

        @Test
        @DisplayName("Owner assigns a manager — persisted in manager list")
        void assignManagerPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Managed Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

            eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getManagerIds()).contains(MANAGER_ID);
        }

        @Test
        @DisplayName("Owner removes a manager — removed from manager list")
        void removeManagerPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Managed Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

            eventService.removeManager(event.getEventId(), MANAGER_ID, OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getManagerIds())
                    .doesNotContain(MANAGER_ID);
        }

        @Test
        @DisplayName("Owner transfers ownership — new owner persisted")
        void transferOwnershipPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Transfer Event", null, show_type.CONFERENCE, "TLV"), COMPANY_ID, OWNER_ID);
            Long newOwner = 50L;

            eventService.transferOwnership(event.getEventId(), newOwner, OWNER_ID);

            assertThat(eventService.getEventDetails(event.getEventId()).getOwnerId()).isEqualTo(newOwner);
        }

        @Test
        @DisplayName("Non-owner cannot transfer ownership — permission denied")
        void transferOwnershipUnauthorized() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Protected Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

            assertThatThrownBy(() -> eventService.transferOwnership(event.getEventId(), 50L, UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only the current owner can transfer ownership");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.4.1 — Show Management
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Show Management")
    class ShowManagement {

        @Test
        @DisplayName("Manager adds a show — retrievable from event")
        void addShowPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Multi-Show Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            show s = new show(event.getEventId(), "Night 1", "Opening night", "Artist A", new Date());

            eventService.addShowToEvent(event.getEventId(), s, OWNER_ID);

            List<show> shows = eventService.getShowsForEvent(event.getEventId());
            assertThat(shows).hasSize(1);
            assertThat(shows.get(0).getName()).isEqualTo("Night 1");
        }

        @Test
        @DisplayName("Manager removes a show — no longer retrievable")
        void removeShowPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Multi-Show Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            show s = new show(event.getEventId(), "Night 1", "Opening night", "Artist A", new Date());
            eventService.addShowToEvent(event.getEventId(), s, OWNER_ID);

            eventService.removeShowFromEvent(event.getEventId(), s, OWNER_ID);

            assertThat(eventService.getShowsForEvent(event.getEventId())).isEmpty();
        }

        @Test
        @DisplayName("Unauthorized user cannot add a show — permission denied")
        void addShowUnauthorized() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Protected Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            show s = new show(event.getEventId(), "Night 1", "Hack", "Artist X", new Date());

            assertThatThrownBy(() -> eventService.addShowToEvent(event.getEventId(), s, UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can add shows");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.1 — View Events of a Production Company
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.1 — View Events of a Production Company")
    class ViewCompanyEvents {

        @Test
        @DisplayName("Returns all events for a company")
        void viewEventsOfCompany() {
            eventService.createEvent(new EventDto(null, "Event A", null, show_type.FESTIVAL, "TLV"), COMPANY_ID,
                    OWNER_ID);
            eventService.createEvent(new EventDto(null, "Event B", null, show_type.CONFERENCE, "TLV"), COMPANY_ID,
                    OWNER_ID);

            List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(e -> e.getCompanyId().equals(COMPANY_ID));
        }

        @Test
        @DisplayName("Returns empty list when company has no events")
        void noEventsForCompany() {
            List<Event> result = eventService.getEventsByCompany(999L);
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.3 — Search Events
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.3 — Search Events")
    class SearchEvents {

        @Test
        @DisplayName("Search by name returns matching events")
        void searchByName() {
            eventService.createEvent(new EventDto(null, "Jazz Night", null, show_type.PERFORMANCE, "TLV"), COMPANY_ID,
                    OWNER_ID);
            eventService.createEvent(new EventDto(null, "Rock Festival", null, show_type.FESTIVAL, "TLV"), COMPANY_ID,
                    OWNER_ID);

            List<Event> result = eventService.searchEventsByName("Jazz");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Jazz Night");
        }

        @Test
        @DisplayName("Search by name is case-insensitive")
        void searchByNameCaseInsensitive() {
            eventService.createEvent(new EventDto(null, "Jazz Night", null, show_type.PERFORMANCE, "TLV"), COMPANY_ID,
                    OWNER_ID);

            assertThat(eventService.searchEventsByName("jazz")).hasSize(1);
            assertThat(eventService.searchEventsByName("JAZZ")).hasSize(1);
        }

        @Test
        @DisplayName("Search by type returns matching events")
        void searchByType() {
            eventService.createEvent(new EventDto(null, "Big Festival", null, show_type.FESTIVAL, "TLV"), COMPANY_ID,
                    OWNER_ID);
            eventService.createEvent(new EventDto(null, "Tech Conf", null, show_type.CONFERENCE, "TLV"), COMPANY_ID,
                    OWNER_ID);

            List<Event> result = eventService.searchEventsByType(show_type.FESTIVAL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).isEqualTo(show_type.FESTIVAL);
        }

        @Test
        @DisplayName("Search returns empty list when no matches found")
        void noSearchResults() {
            eventService.createEvent(new EventDto(null, "Jazz Night", null, show_type.PERFORMANCE, "TLV"), COMPANY_ID,
                    OWNER_ID);

            List<Event> result = eventService.searchEventsByName("ClassicOrchestra");

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.1 — Reviews
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.1 — Event Reviews")
    class EventReviews {

        @Test
        @DisplayName("User adds a review — persisted and retrievable")
        void addReviewPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Reviewed Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            UUID userId = UUID.randomUUID();

            eventService.addReviewToEvent(event.getEventId(), userId, 5);

            Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());
            assertThat(reviews).containsEntry(userId, 5);
        }

        @Test
        @DisplayName("Multiple users can review the same event")
        void multipleReviewsPersisted() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Popular Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            eventService.addReviewToEvent(event.getEventId(), user1, 4);
            eventService.addReviewToEvent(event.getEventId(), user2, 2);

            Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());
            assertThat(reviews).hasSize(2);
            assertThat(reviews.get(user1)).isEqualTo(4);
            assertThat(reviews.get(user2)).isEqualTo(2);
        }

        @Test
        @DisplayName("Review with invalid rating is rejected")
        void invalidRatingRejected() {
            Event event = eventService.createEvent(
                    new EventDto(null, "Rated Event", null, show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

            assertThatThrownBy(() -> eventService.addReviewToEvent(event.getEventId(), UUID.randomUUID(), 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.4 / II.2.8 — Reserve & Checkout (owned by Booking service)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.4 / II.2.8 — Reserve and Checkout")
    class ReserveAndCheckout {

        @Test
        @Disabled("Requires active_order_service + Booking_service integration")
        @DisplayName("Reserve tickets — status becomes LOCKED_IN_CART")
        void reserveTickets() {
        }

        @Test
        @Disabled("Requires Booking_service + IPaymentGateway + ITicketSupplierGateway")
        @DisplayName("Checkout — tickets become PURCHASED (all-or-nothing)")
        void checkout() {
        }
    }
}
