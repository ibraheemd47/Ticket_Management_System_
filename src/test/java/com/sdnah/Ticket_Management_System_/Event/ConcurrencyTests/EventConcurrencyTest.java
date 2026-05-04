package com.sdnah.Ticket_Management_System_.Event.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

@SpringBootTest
@ActiveProfiles("test")
class EventConcurrencyTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private IEventRepository eventRepository;

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
    }

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
    }

    private Outcome runConcurrently(int threads, Runnable action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Outcome outcome = new Outcome();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    action.run();
                    outcome.successes.incrementAndGet();
                } catch (Throwable t) {
                    outcome.failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();

        boolean finished = done.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "concurrent workload did not finish within timeout");
        return outcome;
    }

    private Event createBasicEvent(String name, Long companyId, Long ownerId) {
        EventDto dto = new EventDto();
        dto.name = name;
        dto.eventType = show_type.CONFERENCE; // change this if your enum has another value

        return eventService.createEvent(dto, companyId, ownerId);
    }

    @Test
    @DisplayName("Concurrent assignManager on same manager: only one succeeds")
    void concurrentAssignManager_SameManager_OnlyOneSucceeds() throws Exception {
        // Arrange
        Long companyId = 1L;
        Long ownerId = 10L;
        Long managerId = 20L;

        Event event = createBasicEvent("Manager Race Event", companyId, ownerId);
        UUID eventId = event.getEventId();

        // Act
        Outcome outcome = runConcurrently(10,
                () -> eventService.assignManager(eventId, managerId, ownerId));

        // Assert
        List<UUID> managerIds = eventService.getEventsByManager(eventId);

        assertEquals(1, outcome.successes.get(), "exactly one manager assignment should succeed");
        assertEquals(9, outcome.failures.get(), "the rest should fail because manager already exists");

        assertTrue(managerIds.contains(managerId));

        long managerOccurrences = managerIds.stream()
                .filter(id -> id.equals(managerId))
                .count();

        assertEquals(1, managerOccurrences, "manager should appear exactly once");
    }

    @Test
    @DisplayName("Concurrent transferOwnership to same new owner: final owner is correct")
    void concurrentTransferOwnership_SameNewOwner_FinalOwnerIsCorrect() throws Exception {
        // Arrange
        Long companyId = 1L;
        Long currentOwnerId = 10L;
        Long newOwnerId = 20L;

        Event event = createBasicEvent("Ownership Race Event", companyId, currentOwnerId);
        UUID eventId = event.getEventId();

        // new owner must be manager or transferOwnership will add him if missing
        // according to your Event.transferOwnership implementation

        // Act
        Outcome outcome = runConcurrently(10,
                () -> eventService.transferOwnership(eventId, newOwnerId, currentOwnerId));

        // Assert
        Event reloaded = eventService.getEventDetails(eventId);

        assertEquals(newOwnerId, reloaded.getOwnerId());
        assertTrue(outcome.successes.get() >= 1, "at least one transfer should succeed");
        assertTrue(reloaded.getManagerIds().contains(newOwnerId));
    }

    @Test
    @DisplayName("Concurrent addReview by same user: only one review entry remains")
    void concurrentAddReview_SameUser_OnlyOneReviewEntryRemains() throws Exception {
        // Arrange
        Long companyId = 1L;
        Long ownerId = 10L;
        UUID userId = UUID.randomUUID();

        Event event = createBasicEvent("Review Race Event", companyId, ownerId);
        UUID eventId = event.getEventId();

        // Act
        Outcome outcome = runConcurrently(10,
                () -> eventService.addReviewToEvent(eventId, userId, 5));

        // Assert
        Map<UUID, Integer> reviews = eventService.getEventReviews(eventId);

        assertTrue(outcome.successes.get() >= 1);
        assertEquals(1, reviews.size(), "same user should have only one review entry");
        assertEquals(5, reviews.get(userId));
    }

    @Test
    @DisplayName("Concurrent createEvent with different names: all succeed")
    void concurrentCreateEvent_DifferentEvents_AllSucceed() throws Exception {
        // Arrange
        int threads = 20;
        Long companyId = 1L;
        Long ownerId = 10L;

        // Act
        Outcome outcome = runConcurrently(threads, () -> {
            EventDto dto = new EventDto();
            dto.name = "Event-" + UUID.randomUUID();
            dto.eventType = show_type.CONFERENCE; // change if needed
            eventService.createEvent(dto, companyId, ownerId);
        });

        // Assert
        assertEquals(threads, outcome.successes.get(), "all independent event creations should succeed");
        assertEquals(0, outcome.failures.get(), "independent event creation should not fail");
        assertEquals(threads, eventService.getAllEvents().size());
    }
}