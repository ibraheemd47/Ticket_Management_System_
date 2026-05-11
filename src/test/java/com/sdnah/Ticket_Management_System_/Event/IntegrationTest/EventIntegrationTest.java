package com.sdnah.Ticket_Management_System_.Event.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * Integration tests for {@link EventService} against a real Spring context and
 * the in-memory H2 database. Exercises the full mutate-then-reload cycle so
 * the JPA mappings, transaction boundaries, and {@code KeyedLock} integration
 * are all covered end-to-end.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Event Module — Integration Tests")
class EventIntegrationTest {

    private static final Long OWNER_ID = 100L;
    private static final Long OTHER_OWNER_ID = 101L;
    private static final Long MANAGER_ID = 200L;
    private static final Long SECOND_MANAGER_ID = 201L;
    private static final Long COMPANY_ID = 500L;
    private static final Long OTHER_COMPANY_ID = 501L;

    @Autowired
    private EventService eventService;

    @Autowired
    private IEventRepository eventRepository;

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EventDto dto(String name, show_type type, String venue) {
        return new EventDto(null, name, null, type, venue);
    }

    private Date addDays(Date base, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    // -------------------------------------------------------------------------
    // Creation & deletion
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given an event is created, when fetched by id, then it is persisted with owner as initial manager")
    void givenEventIsCreated_WhenFetchedById_ThenItIsPersistedWithOwnerAsManager() {
        Event created = eventService.createEvent(
                dto("Jazz Night", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        Event reloaded = eventService.getEventDetails(created.getEventId());

        assertNotNull(reloaded.getEventId());
        assertEquals("Jazz Night", reloaded.getName());
        assertEquals(COMPANY_ID, reloaded.getCompanyId());
        assertEquals(OWNER_ID, reloaded.getOwnerId());
        assertTrue(eventService.getEventManagerIds(created.getEventId()).contains(OWNER_ID),
                "Owner should be added as the initial manager");
    }

    @Test
    @DisplayName("Given an event, when deleted by owner, then it is removed from the repository")
    void givenEvent_WhenDeletedByOwner_ThenItIsRemoved() {
        Event event = eventService.createEvent(
                dto("Pop Concert", show_type.PERFORMANCE, "Haifa"),
                COMPANY_ID,
                OWNER_ID);

        eventService.deleteEvent(event.getEventId(), OWNER_ID);

        assertFalse(eventRepository.findById(event.getEventId()).isPresent());
    }

    @Test
    @DisplayName("Given an event, when a non-owner tries to delete it, then an exception is thrown and the event remains")
    void givenEvent_WhenNonOwnerDeletes_ThenExceptionAndEventRemains() {
        Event event = eventService.createEvent(
                dto("Indie Night", show_type.PERFORMANCE, "Eilat"),
                COMPANY_ID,
                OWNER_ID);

        assertThrows(RuntimeException.class,
                () -> eventService.deleteEvent(event.getEventId(), OTHER_OWNER_ID));

        assertTrue(eventRepository.findById(event.getEventId()).isPresent());
    }

    // -------------------------------------------------------------------------
    // Manager management
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given an event, when a manager is assigned, then the manager id list is persisted")
    void givenEvent_WhenManagerAssigned_ThenManagerIdListIsPersisted() {
        Event event = eventService.createEvent(
                dto("Tech Conf", show_type.CONFERENCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

        List<Long> managers = eventService.getEventManagerIds(event.getEventId());
        assertTrue(managers.contains(OWNER_ID));
        assertTrue(managers.contains(MANAGER_ID));
    }

    @Test
    @DisplayName("Given an event with two managers, when one is removed, then only the other remains")
    void givenEventWithTwoManagers_WhenOneIsRemoved_ThenOnlyOtherRemains() {
        Event event = eventService.createEvent(
                dto("Film Fest", show_type.FESTIVAL, "Jerusalem"),
                COMPANY_ID,
                OWNER_ID);

        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);
        eventService.assignManager(event.getEventId(), SECOND_MANAGER_ID, OWNER_ID);

        eventService.removeManager(event.getEventId(), MANAGER_ID, OWNER_ID);

        List<Long> managers = eventService.getEventManagerIds(event.getEventId());
        assertFalse(managers.contains(MANAGER_ID));
        assertTrue(managers.contains(SECOND_MANAGER_ID));
        assertTrue(managers.contains(OWNER_ID));
    }

    @Test
    @DisplayName("Given an event, when ownership is transferred, then new owner is set and old owner is removed from managers")
    void givenEvent_WhenOwnershipTransferred_ThenNewOwnerIsSet() {
        Event event = eventService.createEvent(
                dto("Comedy Night", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        eventService.transferOwnership(event.getEventId(), OTHER_OWNER_ID, OWNER_ID);

        Event reloaded = eventService.getEventDetails(event.getEventId());
        assertEquals(OTHER_OWNER_ID, reloaded.getOwnerId());

        List<Long> managers = eventService.getEventManagerIds(event.getEventId());
        assertTrue(managers.contains(OTHER_OWNER_ID));
        assertFalse(managers.contains(OWNER_ID),
                "Previous owner should be removed from managers after transfer");
    }

    // -------------------------------------------------------------------------
    // Event field edits
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given an event, when name is edited, then the new name is persisted")
    void givenEvent_WhenNameEdited_ThenNewNamePersisted() {
        Event event = eventService.createEvent(
                dto("Original Name", show_type.FESTIVAL, "Haifa"),
                COMPANY_ID,
                OWNER_ID);

        eventService.editEventName(event.getEventId(), "Updated Name", OWNER_ID);

        assertEquals("Updated Name",
                eventService.getEventDetails(event.getEventId()).getName());
    }

    @Test
    @DisplayName("Given an event, when type is edited, then the new type is persisted")
    void givenEvent_WhenTypeEdited_ThenNewTypePersisted() {
        Event event = eventService.createEvent(
                dto("Will Change Type", show_type.CONFERENCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        eventService.editEventType(event.getEventId(), show_type.FESTIVAL, OWNER_ID);

        assertEquals(show_type.FESTIVAL,
                eventService.getEventDetails(event.getEventId()).getEventType());
    }

    @Test
    @DisplayName("Given an event, when dates are edited, then both start and end dates are persisted")
    void givenEvent_WhenDatesEdited_ThenDatesPersisted() {
        Event event = eventService.createEvent(
                dto("Summer Fest", show_type.FESTIVAL, "Eilat"),
                COMPANY_ID,
                OWNER_ID);

        Date now = new Date();
        Date start = addDays(now, 30);
        Date end = addDays(now, 32);

        eventService.editEventDates(event.getEventId(), start, end, OWNER_ID);

        Event reloaded = eventService.getEventDetails(event.getEventId());
        assertEquals(start.getTime(), reloaded.getStartDate().getTime());
        assertEquals(end.getTime(), reloaded.getEndDate().getTime());
    }

    @Test
    @DisplayName("Given an event, when description is edited, then the new description is persisted")
    void givenEvent_WhenDescriptionEdited_ThenNewDescriptionPersisted() {
        Event event = eventService.createEvent(
                dto("Anything", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        eventService.editEventDescription(event.getEventId(), "An evening of stories", OWNER_ID);

        assertEquals("An evening of stories",
                eventService.getEventDetails(event.getEventId()).getDescription());
    }

    @Test
    @DisplayName("Given an event, when venue is edited, then the new venue is persisted")
    void givenEvent_WhenVenueEdited_ThenNewVenuePersisted() {
        Event event = eventService.createEvent(
                dto("Anything", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        eventService.editEventVenue(event.getEventId(), "Heichal HaTarbut", OWNER_ID);

        assertEquals("Heichal HaTarbut",
                eventService.getEventDetails(event.getEventId()).getVenue());
    }

    @Test
    @DisplayName("Given an event, when a non-manager tries to edit it, then an exception is thrown")
    void givenEvent_WhenNonManagerEdits_ThenExceptionThrown() {
        Event event = eventService.createEvent(
                dto("Locked Down", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        assertThrows(RuntimeException.class,
                () -> eventService.editEventName(event.getEventId(), "Hacked", OTHER_OWNER_ID));
    }

    // -------------------------------------------------------------------------
    // Reviews
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given an event, when reviews are added by multiple users, then all are persisted")
    void givenEvent_WhenMultipleReviewsAdded_ThenAllPersisted() {
        Event event = eventService.createEvent(
                dto("Reviewable", show_type.FESTIVAL, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        eventService.addReviewToEvent(event.getEventId(), userA, 5);
        eventService.addReviewToEvent(event.getEventId(), userB, 3);

        Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());
        assertEquals(2, reviews.size());
        assertEquals(5, reviews.get(userA));
        assertEquals(3, reviews.get(userB));
    }

    @Test
    @DisplayName("Given the same user reviews twice, when the latest is added, then only the latest rating is kept")
    void givenSameUserReviewsTwice_WhenLatestAdded_ThenOnlyLatestKept() {
        Event event = eventService.createEvent(
                dto("Re-Reviewed", show_type.FESTIVAL, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        UUID userId = UUID.randomUUID();

        eventService.addReviewToEvent(event.getEventId(), userId, 2);
        eventService.addReviewToEvent(event.getEventId(), userId, 5);

        Map<UUID, Integer> reviews = eventService.getEventReviews(event.getEventId());
        assertEquals(1, reviews.size());
        assertEquals(5, reviews.get(userId));
    }

    @Test
    @DisplayName("Given an event, when an out-of-range rating is submitted, then an exception is thrown")
    void givenEvent_WhenOutOfRangeRating_ThenExceptionThrown() {
        Event event = eventService.createEvent(
                dto("Strict Reviews", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        UUID userId = UUID.randomUUID();

        assertThrows(RuntimeException.class,
                () -> eventService.addReviewToEvent(event.getEventId(), userId, 6));
    }

    // -------------------------------------------------------------------------
    // Queries / search
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given multiple events from two companies, when fetched by company, then only that company's events are returned")
    void givenMultipleEvents_WhenFetchedByCompany_ThenOnlyMatchingReturned() {
        eventService.createEvent(dto("A1", show_type.PERFORMANCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("A2", show_type.FESTIVAL, "Haifa"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("B1", show_type.CONFERENCE, "Tel Aviv"), OTHER_COMPANY_ID, OTHER_OWNER_ID);

        List<Event> companyAEvents = eventService.getEventsByCompany(COMPANY_ID);
        List<Event> companyBEvents = eventService.getEventsByCompany(OTHER_COMPANY_ID);

        assertEquals(2, companyAEvents.size());
        assertEquals(1, companyBEvents.size());
        assertTrue(companyAEvents.stream().allMatch(e -> COMPANY_ID.equals(e.getCompanyId())));
    }

    @Test
    @DisplayName("Given multiple events, when fetched by owner, then only events owned by that owner are returned")
    void givenMultipleEvents_WhenFetchedByOwner_ThenOnlyOwnerMatchesReturned() {
        eventService.createEvent(dto("Mine 1", show_type.PERFORMANCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Mine 2", show_type.FESTIVAL, "Haifa"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Theirs", show_type.CONFERENCE, "Eilat"), COMPANY_ID, OTHER_OWNER_ID);

        List<Event> mine = eventService.getEventsByOwner(OWNER_ID);

        assertEquals(2, mine.size());
        assertTrue(mine.stream().allMatch(e -> OWNER_ID.equals(e.getOwnerId())));
    }

    @Test
    @DisplayName("Given an event with assigned manager, when fetched by manager, then it is found")
    void givenEventWithManager_WhenFetchedByManager_ThenFound() {
        Event event = eventService.createEvent(
                dto("Managed", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);
        eventService.assignManager(event.getEventId(), MANAGER_ID, OWNER_ID);

        List<Event> managed = eventService.getEventsByManager(MANAGER_ID);

        assertEquals(1, managed.size());
        assertEquals(event.getEventId(), managed.get(0).getEventId());
    }

    @Test
    @DisplayName("Given events with various names, when searched by partial name, then matching events are returned")
    void givenEvents_WhenSearchedByPartialName_ThenMatchingReturned() {
        eventService.createEvent(dto("Jazz Night", show_type.PERFORMANCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Jazz Festival", show_type.FESTIVAL, "Haifa"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Tech Conf", show_type.CONFERENCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);

        List<Event> result = eventService.searchEventsByName("jazz");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getName().toLowerCase().contains("jazz")));
    }

    @Test
    @DisplayName("Given events of different types, when searched by type, then only that type is returned")
    void givenEventsOfDifferentTypes_WhenSearchedByType_ThenOnlyThatTypeReturned() {
        eventService.createEvent(dto("Festival 1", show_type.FESTIVAL, "Tel Aviv"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Festival 2", show_type.FESTIVAL, "Haifa"), COMPANY_ID, OWNER_ID);
        eventService.createEvent(dto("Perf 1", show_type.PERFORMANCE, "Eilat"), COMPANY_ID, OWNER_ID);

        List<Event> result = eventService.searchEventsByType(show_type.FESTIVAL);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getEventType() == show_type.FESTIVAL));
    }

    @Test
    @DisplayName("Given events with date ranges, when filtered by name and date window, then only matching events are returned")
    void givenEventsWithDates_WhenFilteredByNameAndDateWindow_ThenOnlyMatchingReturned() {
        Event a = eventService.createEvent(dto("Inside Window", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID, OWNER_ID);
        Event b = eventService.createEvent(dto("Outside Window", show_type.PERFORMANCE, "Tel Aviv"),
                COMPANY_ID, OWNER_ID);

        Date now = new Date();
        eventService.editEventDates(a.getEventId(), addDays(now, 5), addDays(now, 6), OWNER_ID);
        eventService.editEventDates(b.getEventId(), addDays(now, 60), addDays(now, 61), OWNER_ID);

        List<Event> result = eventService.getEventsByFilter(
                "window",
                show_type.PERFORMANCE,
                addDays(now, 1),
                addDays(now, 30));

        assertEquals(1, result.size());
        assertEquals(a.getEventId(), result.get(0).getEventId());
    }

    @Test
    @DisplayName("Given no events match filter, when filtered, then an empty list is returned")
    void givenNoMatch_WhenFiltered_ThenEmptyListReturned() {
        eventService.createEvent(dto("Solo", show_type.PERFORMANCE, "Tel Aviv"), COMPANY_ID, OWNER_ID);

        List<Event> result = eventService.searchEventsByName("nothing-matches-this");

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Shows (read paths)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Given an event with no shows, when shows are fetched, then an empty list is returned")
    void givenEventWithoutShows_WhenShowsFetched_ThenEmptyListReturned() {
        Event event = eventService.createEvent(
                dto("Empty Lineup", show_type.FESTIVAL, "Tel Aviv"),
                COMPANY_ID,
                OWNER_ID);

        List<show> shows = eventService.getShowsForEvent(event.getEventId());

        assertNotNull(shows);
        assertTrue(shows.isEmpty());
    }

    @Test
    @DisplayName("Given a missing event id, when details are fetched, then an exception is thrown")
    void givenMissingEventId_WhenDetailsFetched_ThenExceptionThrown() {
        assertThrows(RuntimeException.class,
                () -> eventService.getEventDetails(UUID.randomUUID()));
    }
}
