package com.sdnah.Ticket_Management_System_.Event.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sdnah.Ticket_Management_System_.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket.TicketStatus;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Concurrency tests for TicketService. The hot race is two users locking the
 * same seat at the same time: exactly one cart should win, the rest must see
 * the ticket as already taken. We also verify that distinct tickets don't
 * block each other, and that purchase / scan transitions are one-shot.
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketConcurrencyTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void cleanDb() {
        ticketRepository.deleteAll();
    }

    @AfterEach
    void afterCleanDb() {
        ticketRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
    }

    /**
     * Runs {@code action} from {@code threads} workers that all start at the
     * same instant. Counts a "success" when the supplier returns true and a
     * "failure" otherwise (including thrown exceptions).
     */
    private Outcome runConcurrently(int threads, BooleanSupplier action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Outcome outcome = new Outcome();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (action.getAsBoolean()) {
                        outcome.successes.incrementAndGet();
                    } else {
                        outcome.failures.incrementAndGet();
                    }
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

    /**
     * Persist an Area + ticket inside a fresh transaction and return the ticket id.
     * Using TransactionTemplate so workers later see committed rows.
     */
    private UUID createAvailableTicket() {
        return txTemplate.execute(status -> {
            Area area = entityManager.merge(new Area("General Admission"));
            entityManager.flush();
            ticket t = new ticket(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    area,
                    new Date(),
                    BigDecimal.TEN);
            ticketRepository.save(t);
            return t.getTicketId();
        });
    }

    private UUID createPurchasedTicket(String ownerId) {
        return txTemplate.execute(status -> {
            Area area = entityManager.merge(new Area("General Admission"));
            entityManager.flush();
            ticket t = new ticket(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    area,
                    new Date(),
                    BigDecimal.TEN);
            t.lockInCart(ownerId);
            t.purchase(ownerId);
            ticketRepository.save(t);
            return t.getTicketId();
        });
    }

    // -------------------------------------------------------------------------
    // The seat race: N users try to lock the same ticket
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent lock on same ticket: exactly one user wins, ticket has one owner")
    void concurrentLock_SameTicket_ExactlyOneSucceeds() throws Exception {
        UUID ticketId = createAvailableTicket();

        int threads = 20;
        Outcome outcome = runConcurrently(threads,
                () -> ticketService.lockTicketForUser(ticketId, "user-" + Thread.currentThread().getId()));

        assertEquals(1, outcome.successes.get(), "exactly one lock should succeed");
        assertEquals(threads - 1, outcome.failures.get(), "all other locks should fail");

        ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.LOCKED_IN_CART, reloaded.getStatus());
        assertNotNull(reloaded.getOwnerId(), "winning ticket must have an owner");
    }

    // -------------------------------------------------------------------------
    // Different tickets in parallel: no false sharing
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent lock on different tickets: all succeed")
    void concurrentLock_DifferentTickets_AllSucceed() throws Exception {
        int n = 10;
        UUID[] ids = new UUID[n];
        for (int i = 0; i < n; i++) {
            ids[i] = createAvailableTicket();
        }

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < n; i++) {
            final UUID id = ids[i];
            final String userId = "user-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (ticketService.lockTicketForUser(id, userId)) {
                        successes.incrementAndGet();
                    }
                } catch (Throwable ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(n, successes.get(), "distinct tickets should not block each other");
        for (UUID id : ids) {
            assertEquals(TicketStatus.LOCKED_IN_CART,
                    ticketRepository.findById(id).orElseThrow().getStatus());
        }
    }

    // -------------------------------------------------------------------------
    // Purchase race: ticket is already locked, only the locker can confirm
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent confirmPurchase on locked ticket: only the locker succeeds")
    void concurrentConfirmPurchase_OnlyLockerSucceeds() throws Exception {
        String locker = "locker-" + UUID.randomUUID();
        UUID ticketId = createAvailableTicket();
        assertTrue(ticketService.lockTicketForUser(ticketId, locker));

        int threads = 10;
        Outcome outcome = runConcurrently(threads, () -> {
            // Half the threads use the legitimate locker id, half use a stranger.
            // Even with the legit id, only one purchase should ever flip the row.
            String who = Math.random() < 0.5 ? locker : "intruder-" + Thread.currentThread().getId();
            return ticketService.confirmPurchase(ticketId, who);
        });

        ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, reloaded.getStatus(),
                "ticket must end up PURCHASED");
        assertTrue(outcome.successes.get() >= 1, "at least one purchase must succeed");
        // Only one transition AVAILABLE/LOCKED -> PURCHASED is possible per row;
        // any subsequent confirmPurchase sees status != LOCKED_IN_CART and returns
        // false.
        assertEquals(1, outcome.successes.get(), "only one confirmPurchase should win");
    }

    // -------------------------------------------------------------------------
    // Scan race: a purchased ticket must scan exactly once
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent scanTicketAtDoor on purchased ticket: scans exactly once")
    void concurrentScan_PurchasedTicket_ScansExactlyOnce() throws Exception {
        String owner = "owner-" + UUID.randomUUID();
        UUID ticketId = createPurchasedTicket(owner);

        int threads = 15;
        Outcome outcome = runConcurrently(threads,
                () -> ticketService.scanTicketAtDoor(ticketId));

        ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.SCANNED, reloaded.getStatus());
        assertEquals(1, outcome.successes.get(),
                "a purchased ticket must scan exactly once across all attempts");
    }

    // -------------------------------------------------------------------------
    // Lock vs release: if a release races with a lock attempt, end state is
    // consistent
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent lock and release on same ticket: row never ends up corrupted")
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NEVER)
    void concurrentLockAndRelease_RowStaysConsistent() throws Exception {
        String locker = "locker-" + UUID.randomUUID();
        UUID ticketId = createAvailableTicket();
        assertTrue(ticketService.lockTicketForUser(ticketId, locker));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        pool.submit(() -> {
            try {
                start.await();
                ticketService.releaseTicket(ticketId);
            } catch (Throwable ignored) {
            } finally {
                done.countDown();
            }
        });
        pool.submit(() -> {
            try {
                start.await();
                ticketService.lockTicketForUser(ticketId, "second-" + UUID.randomUUID());
            } catch (Throwable ignored) {
            } finally {
                done.countDown();
            }
        });

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        TicketStatus status = reloaded.getStatus();
        // Whichever ordering wins, the ticket must be in a valid terminal state for
        // this scenario: AVAILABLE (release won last) or LOCKED_IN_CART (lock won
        // last).
        assertTrue(
                status == TicketStatus.AVAILABLE || status == TicketStatus.LOCKED_IN_CART,
                "unexpected status after race: " + status);
        if (status == TicketStatus.AVAILABLE) {
            assertEquals(null, reloaded.getOwnerId(),
                    "available ticket must not have an owner");
        } else {
            assertNotNull(reloaded.getOwnerId(),
                    "locked ticket must have an owner");
        }
    }
}
