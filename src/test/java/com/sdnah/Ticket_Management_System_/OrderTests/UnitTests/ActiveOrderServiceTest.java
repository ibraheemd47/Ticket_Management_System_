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
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
<<<<<<< HEAD
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;
=======
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
>>>>>>> main

class ActiveOrderServiceTest {

    @Mock private ActiveOrderRepository orderRepo;
    @Mock private PurchaseRepository purchaseRepo;              // ✅ NEW
    @Mock private PaymentTransactionRepository txRepo;          // ✅ NEW
    @Mock private PaymentService paymentService;
    @Mock private ITicketSupplierGateway ticketGateway;
    @Mock private PolicyService policyService;
    @Mock private TicketRepository ticketRepository;
    @Mock private Ticket_Domain_Service ticketDomainService;

    private ActiveOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        service = new ActiveOrderService(
                orderRepo,
                purchaseRepo,     // ✅ FIX
                txRepo,           // ✅ FIX
                paymentService,
                ticketGateway,
                policyService,
                ticketRepository,
                ticketDomainService
        );
    }

    @Test
    void reserveTickets_shouldCreateOrder_whenTicketsAvailable() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(orderRepo.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.empty());

        when(policyService.applyGeneralDiscounts(any(), anyDouble(), anyInt()))
                .thenReturn(50.0);

        OrderDTO result = service.reserveTickets(buyerId, eventId, List.of(seat));

        assertEquals(buyerId, result.getBuyerId());
        assertEquals(eventId, result.getEventId());

        verify(orderRepo).saveAndFlush(any());
    }

    @Test
    void reserveTickets_shouldThrow_whenActiveOrderExists() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        when(orderRepo.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.of(new ActiveOrder(buyerId, eventId, 10)));

        assertThrows(IllegalStateException.class,
                () -> service.reserveTickets(buyerId, eventId, List.of()));
    }

    @Test
    void reserveTickets_shouldFail_whenDuplicateTicket() {
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(orderRepo.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.empty());

        when(policyService.applyGeneralDiscounts(any(), anyDouble(), anyInt()))
                .thenReturn(50.0);

        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"))
                .when(orderRepo).saveAndFlush(any());

        assertThrows(IllegalStateException.class,
                () -> service.reserveTickets(buyerId, eventId, List.of(seat)));
    }
}