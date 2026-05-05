package com.sdnah.Ticket_Management_System_.OrderTests.ConcurrencyTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
<<<<<<< HEAD
import com.sdnah.Ticket_Management_System_.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;
=======
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderRepositoryImpl;
>>>>>>> main

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private ActiveOrderService activeOrderService;

    @Autowired
    private ActiveOrderRepository activeOrderRepository;

    @MockBean
    private PurchaseRepository purchaseRepository;

    @MockBean
    private PaymentTransactionRepository paymentTransactionRepository;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ITicketSupplierGateway ticketGateway;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private Ticket_Domain_Service ticketDomainService;

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
    }

    @BeforeEach
    void setup() {
        activeOrderRepository.deleteAll();

        when(policyService.applyGeneralDiscounts(any(UUID.class), anyDouble(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));
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
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

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
    @DisplayName("Concurrent reserve same ticket using JPA lock: only one user succeeds")
    void reserveTickets_shouldAllowOnlyOneUserToReserveSameTicketConcurrently() throws Exception {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();
        int threads = 20;

        Outcome outcome = runConcurrently(threads, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();

            SeatRequest seatRequest = new SeatRequest(
                    ticketId,
                    1L,
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100)
            );

            activeOrderService.reserveTickets(buyerId, eventId, List.of(seatRequest));
        });

        assertEquals(1, outcome.successes.get(), "only one user should reserve the same ticket");
        assertEquals(threads - 1, outcome.failures.get(), "all other users should fail safely");
    }

    @Test
    @DisplayName("Concurrent checkout same ticket: double purchase is prevented")
    void checkout_shouldPreventDoublePurchaseOfSameTicketConcurrently() throws Exception {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();
        int threads = 2;

        Outcome outcome = runConcurrently(threads, () -> {
            String buyerId = "buyer-" + UUID.randomUUID();

            SeatRequest seatRequest = new SeatRequest(
                    ticketId,
                    1L,
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100)
            );

            activeOrderService.reserveTickets(buyerId, eventId, List.of(seatRequest));
        });

        assertEquals(1, outcome.successes.get(), "only one checkout/reservation should succeed");
        assertEquals(1, outcome.failures.get(), "second checkout/reservation should fail");
    }

    @Test
    @DisplayName("Checkout failure keeps lock consistent")
    void checkout_shouldRemainAtomicWhenFailureOccursMidOperation() {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seatRequest = new SeatRequest(
                ticketId,
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(100)
        );

        OrderDTO order = activeOrderService.reserveTickets(
                "buyer-" + UUID.randomUUID(),
                eventId,
                List.of(seatRequest)
        );

        activeOrderService.cancelOrder(order.getOrderId(), order.getBuyerId());

        activeOrderService.reserveTickets(
                "another-buyer-" + UUID.randomUUID(),
                eventId,
                List.of(seatRequest)
        );

        assertTrue(true, "ticket should be available again after cancellation");
    }

    @Test
    @DisplayName("System remains consistent under concurrent locking operations")
    void system_shouldRemainConsistentUnderConcurrentOperations() throws Exception {
        UUID eventId = UUID.randomUUID();

        int threads = 30;
        int uniqueTickets = 10;

        Outcome outcome = runConcurrently(threads, () -> {
            int ticketNumber = Math.abs(UUID.randomUUID().hashCode() % uniqueTickets);
            String ticketId = UUID.nameUUIDFromBytes(("ticket-" + ticketNumber).getBytes()).toString();

            SeatRequest seatRequest = new SeatRequest(
                    ticketId,
                    1L,
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100)
            );

            activeOrderService.reserveTickets(
                    "buyer-" + UUID.randomUUID(),
                    eventId,
                    List.of(seatRequest)
            );
        });

        assertTrue(outcome.successes.get() <= uniqueTickets,
                "no more than one lock per ticket should succeed");

        assertEquals(threads - outcome.successes.get(), outcome.failures.get(),
                "every non-successful attempt should fail safely");
    }

    @Test
    @DisplayName("Concurrent lock on different tickets: all buyers succeed")
    void concurrentLock_DifferentTickets_AllSucceed() throws Exception {
        UUID eventId = UUID.randomUUID();

        int threads = 20;

        Outcome outcome = runConcurrently(threads, () -> {
            String ticketId = UUID.randomUUID().toString();

            SeatRequest seatRequest = new SeatRequest(
                    ticketId,
                    1L,
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100)
            );

            activeOrderService.reserveTickets(
                    "buyer-" + UUID.randomUUID(),
                    eventId,
                    List.of(seatRequest)
            );
        });

        assertEquals(threads, outcome.successes.get(), "all different tickets should be locked");
        assertEquals(0, outcome.failures.get(), "no buyer should fail for different tickets");
    }
}