package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;

class ActiveOrderServiceTest {

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ITicketSupplierGateway ticketGateway;

    @Mock
    private PolicyService policyService;

    private ActiveOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ActiveOrderService(
                orderRepository,
                paymentService,
                ticketGateway,
                policyService
        );
    }

    @Test
    void reserveTickets_shouldCreateOrder_whenTicketsAvailable() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(orderRepository.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.empty());

        when(orderRepository.acquireLock(any(Lock.class)))
                .thenReturn(true);

        when(policyService.applyGeneralDiscounts(eq(eventId), anyDouble(), anyInt()))
                .thenReturn(50.0);

        OrderDTO result = service.reserveTickets(buyerId, eventId, List.of(seat));

        assertEquals(buyerId, result.getBuyerId());
        assertEquals(eventId, result.getEventId());

        verify(orderRepository).findActiveOrder(buyerId, eventId);
        verify(orderRepository).acquireLock(any(Lock.class));
        verify(orderRepository).save(any());
    }

    @Test
    void reserveTickets_shouldThrow_whenActiveOrderAlreadyExists() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        when(orderRepository.findActiveOrder(eq(buyerId), eq(eventId)))
                .thenReturn(Optional.of(
                        new com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder(
                                buyerId,
                                eventId,
                                10
                        )
                ));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.reserveTickets(buyerId, eventId, List.of())
        );

        assertEquals("Active order already exists", ex.getMessage());
    }

    @Test
    void reserveTickets_shouldRollbackLocks_whenSecondTicketFails() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        SeatRequest seat1 = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        SeatRequest seat2 = new SeatRequest(
                "ticket2",
                2L,
                UUID.randomUUID(),
                new BigDecimal("30")
        );

        when(orderRepository.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.empty());

        when(orderRepository.acquireLock(any(Lock.class)))
                .thenReturn(true)
                .thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.reserveTickets(buyerId, eventId, List.of(seat1, seat2))
        );

        assertEquals("Ticket already reserved: ticket2", ex.getMessage());

        verify(orderRepository).releaseLock("ticket1");
    }

    @Test
    void constructor_shouldThrow_whenRepositoryIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActiveOrderService(null, paymentService, ticketGateway, policyService)
        );
    }

    @Test
    void constructor_shouldThrow_whenPaymentServiceIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActiveOrderService(orderRepository, null, ticketGateway, policyService)
        );
    }

    @Test
    void constructor_shouldThrow_whenTicketGatewayIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActiveOrderService(orderRepository, paymentService, null, policyService)
        );
    }

    @Test
    void constructor_shouldThrow_whenPolicyServiceIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActiveOrderService(orderRepository, paymentService, ticketGateway, null)
        );
    }
}