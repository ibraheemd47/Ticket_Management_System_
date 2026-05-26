package com.sdnah.Ticket_Management_System_.Event.AcceptanceTests;

import static org.assertj.core.api.Assertions.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Event Module — Acceptance Tests")
class EventAcceptanceTest {

        @Autowired
        private EventService eventService;

        @Autowired
        private IEventRepository eventRepository;

        private static final String OWNER_ID = "owner-1";
        private static final String MANAGER_ID = "manager-2";
        private static final UUID COMPANY_ID = UUID.randomUUID();
        private static final String UNAUTHORIZED = "user-99";

        @BeforeEach
        void cleanDb() {
                eventRepository.deleteAll();
        }

        private EventDto dto(String name, show_type type, String venue) {
                return new EventDto(null, name, null, type, venue);
        }

        // -------------------------------------------------------------------------
        // UC II.4.1 — Manage Events and Ticket Inventory
        // -------------------------------------------------------------------------
        @Nested
        @DisplayName("UC II.4.1 — Manage Events and Ticket Inventory")
        class ManageEventsAndInventory {

                @Test
                @DisplayName("Given owner creates event, when event is fetched, then event is persisted")
                void givenOwnerCreatesEvent_WhenEventFetched_ThenEventIsPersisted() {
                        // Act
                        Event created = eventService.createEvent(
                                        dto("Jazz Night", show_type.PERFORMANCE, "Tel Aviv"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Assert
                        assertThat(created.getEventId()).isNotNull();

                        Event fetched = eventService.getEventDetails(created.getEventId());

                        assertThat(fetched.getName()).isEqualTo("Jazz Night");
                        assertThat(fetched.getCompanyId()).isEqualTo(COMPANY_ID);
                        assertThat(fetched.getOwnerId()).isEqualTo(OWNER_ID);
                }

                @Test
                @DisplayName("Given manager edits event name, when event is fetched, then change is persisted")
                void givenManagerEditsEventName_WhenEventFetched_ThenChangeIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Original Name", show_type.FESTIVAL, "Haifa"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

                        // Act
                        eventService.editEventName(event.getEventId(), "Updated Name", MANAGER_ID);

                        // Assert
                        assertThat(eventService.getEventDetails(event.getEventId()).getName())
                                        .isEqualTo("Updated Name");
                }

                @Test
                @DisplayName("Given owner edits event type, when event is fetched, then change is persisted")
                void givenOwnerEditsEventType_WhenEventFetched_ThenChangeIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Tech Conf", show_type.CONFERENCE, "Tel Aviv"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act
                        eventService.editEventType(event.getEventId(), show_type.FESTIVAL, OWNER_ID);

                        // Assert
                        assertThat(eventService.getEventDetails(event.getEventId()).getEventType())
                                        .isEqualTo(show_type.FESTIVAL);
                }

                @Test
                @DisplayName("Given owner edits event dates, when event is fetched, then dates are persisted")
                void givenOwnerEditsEventDates_WhenEventFetched_ThenDatesArePersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Summer Fest", show_type.FESTIVAL, "Eilat"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        Date start = new Date();
                        Date end = new Date(start.getTime() + 86_400_000L);

                        // Act
                        eventService.editEventDates(event.getEventId(), start, end, OWNER_ID);

                        // Assert
                        Event fetched = eventService.getEventDetails(event.getEventId());

                        assertThat(fetched.getStartDate().getTime()).isEqualTo(start.getTime());
                        assertThat(fetched.getEndDate().getTime()).isEqualTo(end.getTime());
                }

                @Test
                @DisplayName("Given owner edits event venue, when event is fetched, then venue is persisted")
                void givenOwnerEditsEventVenue_WhenEventFetched_ThenVenueIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Rock Night", show_type.PERFORMANCE, "TBD"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act
                        eventService.editEventVenue(event.getEventId(), "Yarkon Park", OWNER_ID);

                        // Assert
                        assertThat(eventService.getEventDetails(event.getEventId()).getVenue())
                                        .isEqualTo("Yarkon Park");
                }

                @Test
                @DisplayName("Given owner edits event description, when event is fetched, then description is persisted")
                void givenOwnerEditsEventDescription_WhenEventFetched_ThenDescriptionIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Art Show", show_type.PERFORMANCE, "Jerusalem"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act
                        eventService.editEventDescription(event.getEventId(), "Annual art exhibition", OWNER_ID);

                        // Assert
                        assertThat(eventService.getEventDetails(event.getEventId()).getDescription())
                                        .isEqualTo("Annual art exhibition");
                }

                @Test
                @DisplayName("Given owner deletes event, when fetching event, then event is not found")
                void givenOwnerDeletesEvent_WhenFetchingEvent_ThenEventIsNotFound() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("To Delete", show_type.CONFERENCE, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        UUID id = event.getEventId();

                        // Act
                        eventService.deleteEvent(id, OWNER_ID);

                        // Assert
                        assertThatThrownBy(() -> eventService.getEventDetails(id))
                                        .isInstanceOf(RuntimeException.class)
                                        .hasMessageContaining("Event not found");
                }

                @Test
                @DisplayName("Given non-owner deletes event, when delete is requested, then permission denied")
                void givenNonOwnerDeletesEvent_WhenDeleteRequested_ThenPermissionDenied() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Protected", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act + Assert
                        assertThatThrownBy(() -> eventService.deleteEvent(event.getEventId(), UNAUTHORIZED))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Only the owner can delete the event");
                }

                @Test
                @DisplayName("Given unauthorized user edits event name, when edit is requested, then permission denied")
                void givenUnauthorizedUserEditsEventName_WhenEditRequested_ThenPermissionDenied() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Safe Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act + Assert
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
                @DisplayName("Given owner assigns manager, when manager list is fetched, then manager is persisted")
                void givenOwnerAssignsManager_WhenManagerListFetched_ThenManagerIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Managed Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act
                        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

                        // Assert
                        List<String> managerIds = eventService.getEventManagerIds(event.getEventId());
                        assertThat(managerIds).contains(MANAGER_ID);
                }

                @Test
                @DisplayName("Given owner removes manager, when manager list is fetched, then manager is removed")
                void givenOwnerRemovesManager_WhenManagerListFetched_ThenManagerIsRemoved() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Managed Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

                        // Act
                        eventService.removeManager(event.getEventId(), MANAGER_ID, OWNER_ID);

                        // Assert
                        List<String> managerIds = eventService.getEventManagerIds(event.getEventId());
                        assertThat(managerIds).doesNotContain(MANAGER_ID);
                }

                @Test
                @DisplayName("Given owner transfers ownership, when event is fetched, then new owner is persisted")
                void givenOwnerTransfersOwnership_WhenEventFetched_ThenNewOwnerIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Transfer Event", show_type.CONFERENCE, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        String newOwner = "owner-50";

                        // Act
                        eventService.transferOwnership(event.getEventId(), newOwner, OWNER_ID);

                        // Assert
                        Event fetched = eventService.getEventDetails(event.getEventId());

                        assertThat(fetched.getOwnerId()).isEqualTo(newOwner);
                        assertThat(eventService.getEventManagerIds(event.getEventId())).contains(newOwner);
                }

                @Test
                @DisplayName("Given non-owner transfers ownership, when transfer requested, then permission denied")
                void givenNonOwnerTransfersOwnership_WhenTransferRequested_ThenPermissionDenied() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Protected Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act + Assert
                        assertThatThrownBy(() -> eventService.transferOwnership(event.getEventId(), "owner-50", UNAUTHORIZED))
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
                @DisplayName("Given manager adds show, when shows are fetched, then show is returned")
                void givenManagerAddsShow_WhenShowsFetched_ThenShowIsReturned() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Multi-Show Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        show s = new show(event.getEventId(), "Night 1", "Opening night", "Artist A", new Date());

                        // Act
                        eventService.addShowToEvent(event.getEventId(), s, OWNER_ID);

                        // Assert
                        List<show> shows = eventService.getShowsForEvent(event.getEventId());

                        assertThat(shows).hasSize(1);
                        assertThat(shows.get(0).getName()).isEqualTo("Night 1");
                }

                @Test
                @DisplayName("Given manager removes show, when shows are fetched, then list is empty")
                void givenManagerRemovesShow_WhenShowsFetched_ThenListIsEmpty() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Multi-Show Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        show s = new show(event.getEventId(), "Night 1", "Opening night", "Artist A", new Date());

                        eventService.addShowToEvent(event.getEventId(), s, OWNER_ID);

                        // Act
                        eventService.removeShowFromEvent(event.getEventId(), s, OWNER_ID);

                        // Assert
                        assertThat(eventService.getShowsForEvent(event.getEventId())).isEmpty();
                }

                @Test
                @DisplayName("Given unauthorized user adds show, when add is requested, then permission denied")
                void givenUnauthorizedUserAddsShow_WhenAddRequested_ThenPermissionDenied() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Protected Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        show s = new show(event.getEventId(), "Night 1", "Hack", "Artist X", new Date());

                        // Act + Assert
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
                @DisplayName("Given company has events, when fetching company events, then all company events are returned")
                void givenCompanyHasEvents_WhenFetchingCompanyEvents_ThenAllCompanyEventsReturned() {
                        // Arrange
                        eventService.createEvent(dto("Event A", show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
                        eventService.createEvent(dto("Event B", show_type.CONFERENCE, "TLV"), COMPANY_ID, OWNER_ID);

                        // Act
                        List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

                        // Assert
                        assertThat(result).hasSize(2);
                        assertThat(result).allMatch(e -> e.getCompanyId().equals(COMPANY_ID));
                }

                @Test
                @DisplayName("Given company has no events, when fetching company events, then empty list is returned")
                void givenCompanyHasNoEvents_WhenFetchingCompanyEvents_ThenEmptyListReturned() {
                        // Act
                        List<Event> result = eventService.getEventsByCompany(COMPANY_ID);

                        // Assert
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
                @DisplayName("Given event exists, when searching by name, then matching event is returned")
                void givenEventExists_WhenSearchingByName_ThenMatchingEventReturned() {
                        // Arrange
                        eventService.createEvent(dto("Jazz Night", show_type.PERFORMANCE, "TLV"), COMPANY_ID, OWNER_ID);
                        eventService.createEvent(dto("Rock Festival", show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);

                        // Act
                        List<Event> result = eventService.searchEventsByName("Jazz");

                        // Assert
                        assertThat(result).hasSize(1);
                        assertThat(result.get(0).getName()).isEqualTo("Jazz Night");
                }

                @Test
                @DisplayName("Given event exists, when searching by name with different case, then matching event is returned")
                void givenEventExists_WhenSearchingByNameDifferentCase_ThenMatchingEventReturned() {
                        // Arrange
                        eventService.createEvent(dto("Jazz Night", show_type.PERFORMANCE, "TLV"), COMPANY_ID, OWNER_ID);

                        // Assert
                        assertThat(eventService.searchEventsByName("jazz")).hasSize(1);
                        assertThat(eventService.searchEventsByName("JAZZ")).hasSize(1);
                }

                @Test
                @DisplayName("Given events exist, when searching by type, then matching type is returned")
                void givenEventsExist_WhenSearchingByType_ThenMatchingTypeReturned() {
                        // Arrange
                        eventService.createEvent(dto("Big Festival", show_type.FESTIVAL, "TLV"), COMPANY_ID, OWNER_ID);
                        eventService.createEvent(dto("Tech Conf", show_type.CONFERENCE, "TLV"), COMPANY_ID, OWNER_ID);

                        // Act
                        List<Event> result = eventService.searchEventsByType(show_type.FESTIVAL);

                        // Assert
                        assertThat(result).hasSize(1);
                        assertThat(result.get(0).getEventType()).isEqualTo(show_type.FESTIVAL);
                }

                @Test
                @DisplayName("Given no matching event, when searching by name, then empty list is returned")
                void givenNoMatchingEvent_WhenSearchingByName_ThenEmptyListReturned() {
                        // Arrange
                        eventService.createEvent(dto("Jazz Night", show_type.PERFORMANCE, "TLV"), COMPANY_ID, OWNER_ID);

                        // Act
                        List<Event> result = eventService.searchEventsByName("ClassicOrchestra");

                        // Assert
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
                @DisplayName("Given user adds review, when reviews are fetched, then review is persisted")
                void givenUserAddsReview_WhenReviewsFetched_ThenReviewIsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Reviewed Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        UUID userId = UUID.randomUUID();

                        // Act
                        eventService.addReviewToEvent(event.getEventId(), userId, 5);

                        // Assert
                        Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());
                        assertThat(reviews).containsEntry(userId, 5);
                }

                @Test
                @DisplayName("Given multiple users review same event, when reviews are fetched, then all reviews are persisted")
                void givenMultipleUsersReviewSameEvent_WhenReviewsFetched_ThenAllReviewsPersisted() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Popular Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        UUID user1 = UUID.randomUUID();
                        UUID user2 = UUID.randomUUID();

                        // Act
                        eventService.addReviewToEvent(event.getEventId(), user1, 4);
                        eventService.addReviewToEvent(event.getEventId(), user2, 2);

                        // Assert
                        Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());

                        assertThat(reviews).hasSize(2);
                        assertThat(reviews.get(user1)).isEqualTo(4);
                        assertThat(reviews.get(user2)).isEqualTo(2);
                }

                @Test
                @DisplayName("Given invalid review rating, when adding review, then rejected")
                void givenInvalidReviewRating_WhenAddingReview_ThenRejected() {
                        // Arrange
                        Event event = eventService.createEvent(
                                        dto("Rated Event", show_type.FESTIVAL, "TLV"),
                                        COMPANY_ID,
                                        OWNER_ID);

                        // Act + Assert
                        assertThatThrownBy(
                                        () -> eventService.addReviewToEvent(event.getEventId(), UUID.randomUUID(), 6))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Rating must be between 1 and 5");
                }
        }

}