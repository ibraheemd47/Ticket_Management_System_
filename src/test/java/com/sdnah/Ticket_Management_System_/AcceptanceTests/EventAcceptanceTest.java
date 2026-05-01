package com.sdnah.Ticket_Management_System_.AcceptanceTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Module — Acceptance Tests")
class EventAcceptanceTest {

    @Mock
    private IEventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private static final Long OWNER_ID       = 1L;
    private static final Long MANAGER_ID     = 2L;
    private static final Long COMPANY_ID     = 10L;
    private static final Long UNAUTHORIZED   = 99L;

    private Event existingEvent;
    private UUID  eventId;

    @BeforeEach
    void setUp() {
        existingEvent = new Event("Rock Festival", show_type.FESTIVAL, COMPANY_ID, OWNER_ID);
        eventId = existingEvent.getEventId();
    }

    // -------------------------------------------------------------------------
    // UC II.4.1 — Manage Events and Ticket Inventory
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Manage Events and Ticket Inventory")
    class ManageEventsAndInventory {

        @Test
        @DisplayName("Owner creates an event successfully")
        void addEventSuccessfully() {
            EventDto dto = new EventDto(null, "Jazz Night", null, show_type.PERFORMANCE, "Tel Aviv");
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event created = eventService.createEvent(dto, COMPANY_ID, OWNER_ID);

            assertThat(created.getName()).isEqualTo("Jazz Night");
            assertThat(created.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(created.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(created.getManagerIds()).contains(OWNER_ID);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Manager edits event name successfully")
        void editEventNameSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.editEventName(eventId, "Updated Name", MANAGER_ID);

            assertThat(existingEvent.getName()).isEqualTo("Updated Name");
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Manager edits event type successfully")
        void editEventTypeSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.editEventType(eventId, show_type.CONFERENCE, MANAGER_ID);

            assertThat(existingEvent.getEventType()).isEqualTo(show_type.CONFERENCE);
        }

        @Test
        @DisplayName("Manager edits event dates successfully")
        void editEventDatesSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Date start = new Date();
            Date end   = new Date(start.getTime() + 86_400_000L);
            eventService.editEventDates(eventId, start, end, MANAGER_ID);

            assertThat(existingEvent.getStartDate()).isEqualTo(start);
            assertThat(existingEvent.getEndDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("Manager edits event venue successfully")
        void editEventVenueSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.editEventVenue(eventId, "Yarkon Park", MANAGER_ID);

            assertThat(existingEvent.getVenue()).isEqualTo("Yarkon Park");
        }

        @Test
        @DisplayName("Manager edits event description successfully")
        void editEventDescriptionSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.editEventDescription(eventId, "An amazing outdoor concert", MANAGER_ID);

            assertThat(existingEvent.getDescription()).isEqualTo("An amazing outdoor concert");
        }

        @Test
        @DisplayName("Owner removes event successfully")
        void removeEventSuccessfully() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            eventService.deleteEvent(eventId, OWNER_ID);

            verify(eventRepository).delete(existingEvent);
        }

        @Test
        @DisplayName("Non-owner cannot delete event — permission denied")
        void deleteEventUnauthorized() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.deleteEvent(eventId, UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only the owner can delete the event");

            verify(eventRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event name — permission denied")
        void editEventNameUnauthorized() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.editEventName(eventId, "Hack", UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event name");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Event not found — throws RuntimeException")
        void eventNotFound() {
            when(eventRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(UUID.randomUUID(), OWNER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.4.1 — Manager Assignment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Manager Assignment")
    class ManagerAssignment {

        @Test
        @DisplayName("Owner assigns a new manager successfully")
        void assignManagerSuccessfully() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            eventService.assignManager(eventId, MANAGER_ID, OWNER_ID);

            assertThat(existingEvent.getManagerIds()).contains(MANAGER_ID);
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Non-owner cannot assign a manager — permission denied")
        void assignManagerUnauthorized() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.assignManager(eventId, MANAGER_ID, UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only the owner can add managers");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Owner removes a manager successfully")
        void removeManagerSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            eventService.removeManager(eventId, MANAGER_ID, OWNER_ID);

            assertThat(existingEvent.getManagerIds()).doesNotContain(MANAGER_ID);
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Cannot remove a manager who is not assigned")
        void removeNonExistentManager() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.removeManager(eventId, MANAGER_ID, OWNER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("This manager is not assigned");
        }
    }

    // -------------------------------------------------------------------------
    // UC II.4.1 — Show Management
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.4.1 — Show Management")
    class ShowManagement {

        @Test
        @DisplayName("Manager adds a show to an event successfully")
        void addShowSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            show newShow = new show(eventId, "Night 1", "Opening show", "Artist A", new Date());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            eventService.addShowToEvent(eventId, newShow, MANAGER_ID);

            assertThat(existingEvent.getShows()).contains(newShow);
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Unauthorized user cannot add a show — permission denied")
        void addShowUnauthorized() {
            show newShow = new show(eventId, "Night 1", "Opening show", "Artist A", new Date());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.addShowToEvent(eventId, newShow, UNAUTHORIZED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can add shows");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Manager removes a show from an event successfully")
        void removeShowSuccessfully() {
            existingEvent.addManager(MANAGER_ID, OWNER_ID);
            show existingShow = new show(eventId, "Night 1", "Opening show", "Artist A", new Date());
            existingEvent.addShow(existingShow, MANAGER_ID);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            eventService.removeShowFromEvent(eventId, existingShow, MANAGER_ID);

            assertThat(existingEvent.getShows()).doesNotContain(existingShow);
            verify(eventRepository).save(existingEvent);
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.1 — View Events of a Production Company
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.1 — View Events of a Production Company")
    class ViewCompanyEvents {

        @Test
        @DisplayName("Returns all events for an active company")
        void viewEventsOfActiveCompany() {
            List<Event> events = List.of(
                    new Event("Event A", show_type.FESTIVAL, COMPANY_ID, OWNER_ID),
                    new Event("Event B", show_type.CONFERENCE, COMPANY_ID, OWNER_ID)
            );
            when(eventRepository.findByCompanyId(COMPANY_ID)).thenReturn(events);

            List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

            assertThat(result).hasSize(2);
            verify(eventRepository).findByCompanyId(COMPANY_ID);
        }

        @Test
        @DisplayName("Returns empty list when company has no events")
        void noEventsForActiveCompany() {
            when(eventRepository.findByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());

            List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

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
        void searchByNameSuccessfully() {
            List<Event> matches = List.of(new Event("Jazz Night", show_type.PERFORMANCE, COMPANY_ID, OWNER_ID));
            when(eventRepository.searchEventsByName("Jazz")).thenReturn(matches);

            List<Event> result = eventService.searchEventsByName("Jazz");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Jazz Night");
        }

        @Test
        @DisplayName("Search by type returns matching events")
        void searchByTypeSuccessfully() {
            List<Event> matches = List.of(new Event("Big Festival", show_type.FESTIVAL, COMPANY_ID, OWNER_ID));
            when(eventRepository.searchEventsByType(show_type.FESTIVAL)).thenReturn(matches);

            List<Event> result = eventService.searchEventsByType(show_type.FESTIVAL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).isEqualTo(show_type.FESTIVAL);
        }

        @Test
        @DisplayName("Search by singer name returns matching events")
        void searchBySingerSuccessfully() {
            List<Event> matches = List.of(new Event("Adele Live", show_type.PERFORMANCE, COMPANY_ID, OWNER_ID));
            when(eventRepository.searchEventsBySingerName("Adele")).thenReturn(matches);

            List<Event> result = eventService.searchEventsBySingerName("Adele");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Search with filters returns matching events")
        void searchWithFilters() {
            Date start = new Date();
            Date end   = new Date(start.getTime() + 7 * 86_400_000L);
            List<Event> matches = List.of(new Event("Conference 2026", show_type.CONFERENCE, COMPANY_ID, OWNER_ID));
            when(eventRepository.getEventsByFilter("Conference", show_type.CONFERENCE, start, end))
                    .thenReturn(matches);

            List<Event> result = eventService.getEventsByFilter("Conference", show_type.CONFERENCE, start, end);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Search returns empty list when no matches found")
        void noSearchResults() {
            when(eventRepository.searchEventsByName("NonExistent")).thenReturn(Collections.emptyList());

            List<Event> result = eventService.searchEventsByName("NonExistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Search within a specific company returns only that company's events")
        void searchWithinCompany() {
            Long otherCompany = 999L;
            List<Event> companyEvents = List.of(
                    new Event("Company Event", show_type.FESTIVAL, COMPANY_ID, OWNER_ID)
            );
            when(eventRepository.findByCompanyId(COMPANY_ID)).thenReturn(companyEvents);

            List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

            assertThat(result).allMatch(e -> e.getCompanyId().equals(COMPANY_ID));
            assertThat(result).noneMatch(e -> e.getCompanyId().equals(otherCompany));
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.1 — Reviews
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.1 — Event Reviews")
    class EventReviews {

        @Test
        @DisplayName("User adds a review successfully")
        void addReviewSuccessfully() {
            existingEvent.addReview(UUID.randomUUID(), 4);

            assertThat(existingEvent.getReviews()).hasSize(1);
            assertThat(existingEvent.getReviews().values()).containsExactly(4);
        }

        @Test
        @DisplayName("Review with rating out of range is rejected")
        void reviewOutOfRange() {
            assertThatThrownBy(() -> existingEvent.addReview(UUID.randomUUID(), 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");

            assertThatThrownBy(() -> existingEvent.addReview(UUID.randomUUID(), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");
        }

        @Test
        @DisplayName("Multiple users can review the same event")
        void multipleReviews() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            existingEvent.addReview(user1, 5);
            existingEvent.addReview(user2, 3);

            assertThat(existingEvent.getReviews()).hasSize(2);
            assertThat(existingEvent.getReviews().get(user1)).isEqualTo(5);
            assertThat(existingEvent.getReviews().get(user2)).isEqualTo(3);
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.4 — Reserve Tickets (ticket locking — Event module concern)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.4 — Reserve (Lock) Tickets")
    class ReserveTickets {

        @Test
        @Disabled("Requires active_order_service integration — bookSeat delegates to repo custom method not yet implemented")
        @DisplayName("Reserve tickets successfully — status becomes LOCKED_IN_CART")
        void reserveTicketsSuccessfully() {
            // When active_order_service is integrated, it will call EventService.bookSeat()
            // which locks the ticket via ticket.lockInCart(userId)
            // Then: ticket status == LOCKED_IN_CART
        }

        @Test
        @Disabled("Requires active_order_service integration")
        @DisplayName("Reservation expires — tickets released back to AVAILABLE")
        void reservationExpires() {
            // expir_order_service will call unlock on expired tickets
        }
    }

    // -------------------------------------------------------------------------
    // UC II.2.8 — Checkout (ticket lifecycle)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("UC II.2.8 — Checkout (Ticket Lifecycle)")
    class CheckoutTickets {

        @Test
        @Disabled("Requires Booking_service + IPaymentGateway + ITicketSupplierGateway")
        @DisplayName("Checkout successfully — tickets become PURCHASED")
        void checkoutSuccessfully() {
            // Booking_service orchestrates: policy check → payment → issuance → purchase()
        }

        @Test
        @Disabled("Requires Booking_service + IPaymentGateway mock")
        @DisplayName("Payment rejected — tickets released back to AVAILABLE")
        void paymentRejected() {
            // IPaymentGateway mock returns failure → tickets unlocked
        }

        @Test
        @Disabled("Requires Booking_service + ITicketSupplierGateway mock + auto-refund")
        @DisplayName("Ticket issuance rejected — auto refund, tickets released")
        void issuanceRejected() {
            // ITicketSupplierGateway mock returns failure → refund triggered → tickets unlocked
        }

        @Test
        @Disabled("Requires Booking_service — all-or-nothing atomicity")
        @DisplayName("All-or-nothing — partial checkout never persists")
        void noPartialPurchase() {
            // If any ticket in order fails issuance, ALL are rolled back to AVAILABLE
        }
    }
}
