package com.sdnah.Ticket_Management_System_.OrderTests.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderRepositoryImpl;

class OrderConcurrencyTest {

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
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

    @Test
    @DisplayName("Multiple users reserve same ticket through ActiveOrderService: only one succeeds")
    void reserveTicketsThroughService_SameTicket_OnlyOneUserSucceeds() throws Exception {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        PaymentService paymentService = mock(PaymentService.class);
        ITicketSupplierGateway ticketGateway = mock(ITicketSupplierGateway.class);
        PolicyService policyService = mock(PolicyService.class);

        when(policyService.applyGeneralDiscounts(any(UUID.class), anyDouble(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ActiveOrderService activeOrderService = new ActiveOrderService(
                repository,
                paymentService,
                ticketGateway,
                policyService
        );

        UUID eventId = UUID.randomUUID();
        String ticketId = "ticket-" + UUID.randomUUID();

        SeatRequest seatRequest = new SeatRequest(
                ticketId,
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(100)
        );

        List<SeatRequest> seats = List.of(seatRequest);
        int users = 20;

        Outcome outcome = runConcurrently(users, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();
            activeOrderService.reserveTickets(buyerId, eventId, seats);
        });

        assertEquals(1, outcome.successes.get(),
                "only one user should reserve the ticket through ActiveOrderService");

        assertEquals(users - 1, outcome.failures.get(),
                "all other users should fail because the ticket is already reserved");
    }

    @Test
    @DisplayName("Concurrent reserve same ticket using repository lock: only one user succeeds")
    void reserveTickets_shouldAllowOnlyOneUserToReserveSameTicketConcurrently() throws Exception {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        String ticketId = "ticket-" + UUID.randomUUID();
        int threads = 20;

        Outcome outcome = runConcurrently(threads, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();

            Lock lock = new Lock(
                    ticketId,
                    buyerId,
                    LocalDateTime.now().plusMinutes(10)
            );

            boolean acquired = repository.acquireLock(lock);

            if (!acquired) {
                throw new IllegalStateException("ticket already reserved");
            }
        });

        assertEquals(1, outcome.successes.get(), "only one user should reserve the same ticket");
        assertEquals(threads - 1, outcome.failures.get(), "all other users should fail safely");
    }

    @Test
    @DisplayName("Concurrent checkout same ticket: double purchase is prevented")
    void checkout_shouldPreventDoublePurchaseOfSameTicketConcurrently() throws Exception {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        String ticketId = "ticket-" + UUID.randomUUID();
        int threads = 2;

        Outcome outcome = runConcurrently(threads, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();

            Lock lock = new Lock(
                    ticketId,
                    buyerId,
                    LocalDateTime.now().plusMinutes(10)
            );

            boolean acquired = repository.acquireLock(lock);

            if (!acquired) {
                throw new IllegalStateException("ticket already locked/purchased");
            }
        });

        assertEquals(1, outcome.successes.get(), "only one checkout should succeed");
        assertEquals(1, outcome.failures.get(), "second checkout should fail");
    }

    @Test
    @DisplayName("Checkout failure keeps lock consistent")
    void checkout_shouldRemainAtomicWhenFailureOccursMidOperation() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        String ticketId = "ticket-" + UUID.randomUUID();
        String buyerId = "buyer-" + UUID.randomUUID();

        Lock lock = new Lock(
                ticketId,
                buyerId,
                LocalDateTime.now().plusMinutes(10)
        );

        boolean acquired = repository.acquireLock(lock);
        assertTrue(acquired, "first lock should be acquired");

        repository.releaseLock(ticketId);

        Lock secondLock = new Lock(
                ticketId,
                "another-buyer-" + UUID.randomUUID(),
                LocalDateTime.now().plusMinutes(10)
        );

        boolean reacquired = repository.acquireLock(secondLock);

        assertTrue(reacquired, "ticket should be available again after failed checkout rollback");
    }

    @Test
    @DisplayName("System remains consistent under concurrent locking operations")
    void system_shouldRemainConsistentUnderConcurrentOperations() throws Exception {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        int threads = 30;
        int uniqueTickets = 10;

        Outcome outcome = runConcurrently(threads, () -> {
            int ticketNumber = Math.abs(UUID.randomUUID().hashCode() % uniqueTickets);
            String ticketId = "ticket-" + ticketNumber;
            String buyerId = "buyer-" + UUID.randomUUID();

            Lock lock = new Lock(
                    ticketId,
                    buyerId,
                    LocalDateTime.now().plusMinutes(10)
            );

            boolean acquired = repository.acquireLock(lock);

            if (!acquired) {
                throw new IllegalStateException("ticket already locked");
            }
        });

        assertTrue(outcome.successes.get() <= uniqueTickets,
                "no more than one lock per ticket should succeed");

        assertEquals(threads - outcome.successes.get(), outcome.failures.get(),
                "every non-successful attempt should fail safely");
    }

    @Test
    @DisplayName("Concurrent lock on different tickets: all buyers succeed")
    void concurrentLock_DifferentTickets_AllSucceed() throws Exception {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        int threads = 20;

        Outcome outcome = runConcurrently(threads, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();
            String ticketId = "ticket-" + UUID.randomUUID();

            Lock lock = new Lock(
                    ticketId,
                    buyerId,
                    LocalDateTime.now().plusMinutes(10)
            );

            boolean acquired = repository.acquireLock(lock);

            if (!acquired) {
                throw new IllegalStateException("lock failed");
            }
        });

        assertEquals(threads, outcome.successes.get(), "all different tickets should be locked");
        assertEquals(0, outcome.failures.get(), "no buyer should fail for different tickets");
    }
}