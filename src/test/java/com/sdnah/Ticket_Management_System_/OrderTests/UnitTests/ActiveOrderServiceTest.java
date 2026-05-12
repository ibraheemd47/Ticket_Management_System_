package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import com.sdnah.Ticket_Management_System_.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

class ActiveOrderServiceTest {

    @Mock
    private ActiveOrderRepository orderRepo;

    @Mock
    private PurchaseRepository purchaseRepo;

    @Mock
    private PaymentTransactionRepository txRepo;

    @Mock
    private PaymentService paymentService;

    @Mock
    private IPaymentGateway paymentGateway;

    @Mock
    private ITicketSupplierGateway ticketGateway;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private IrepresnteUserService represnteUserService;

    @Mock
    private OrderActionLogRepository actionLogRepo;

    private ActiveOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        service = new ActiveOrderService(
                orderRepo,
                purchaseRepo,
                txRepo,
                paymentService,
                paymentGateway,
                ticketGateway,
                ticketRepository,
                policyRepository,
                represnteUserService,
                actionLogRepo
        );
    }

    @Test
    @DisplayName("Given available tickets, when reserving tickets, then active order is created")
    void reserveTickets_shouldCreateOrder_whenTicketsAvailable() {

        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        when(policyRepository.findPurchasePolicyByEventId(eventId)).thenReturn(null);
        when(policyRepository.findDiscountPolicyByEventId(eventId)).thenReturn(null);

        OrderDTO result = service.reserveTickets(userToken, eventId, List.of(seat));

        assertEquals(buyerId, result.getbuyerId());
        assertEquals(eventId, result.getEventId());

        verify(represnteUserService).requireMemberId(userToken);
        verify(orderRepo).findActiveOrder(buyerId, eventId);
        verify(orderRepo).isTicketLocked(ticketId.toString());
        verify(orderRepo, times(2)).save(any(ActiveOrder.class));
    }

    @Test
    @DisplayName("Given active order already exists, when reserving tickets, then exception is thrown")
    void reserveTickets_shouldThrow_whenActiveOrderExists() {

        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);

        when(orderRepo.findActiveOrder(buyerId, eventId))
                .thenReturn(Optional.of(new ActiveOrder(buyerId, eventId, 10)));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.reserveTickets(userToken, eventId, List.of())
        );

        assertEquals("Active order already exists", ex.getMessage());

        verify(represnteUserService).requireMemberId(userToken);
        verify(orderRepo).findActiveOrder(buyerId, eventId);
    }

    @Test
    @DisplayName("Given ticket already locked, when reserving tickets, then exception is thrown")
    void reserveTickets_shouldThrow_whenTicketAlreadyLocked() {

        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.reserveTickets(userToken, eventId, List.of(seat))
        );

        assertEquals("Ticket already reserved: " + ticketId, ex.getMessage());

        verify(represnteUserService).requireMemberId(userToken);
        verify(orderRepo).isTicketLocked(ticketId.toString());
    }

    @Test
    @DisplayName("Given repository save fails, when reserving tickets, then exception is propagated")
    void reserveTickets_shouldPropagateException_whenRepositorySaveFails() {

        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(orderRepo).save(any(ActiveOrder.class));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.reserveTickets(userToken, eventId, List.of(seat))
        );
    }

    @Test
    @DisplayName("Given valid active order and successful payment, when checkout, then purchase is completed")
    void checkout_shouldCompletePurchase_whenPaymentAndTicketIssuanceSucceed() {

        String userToken = "token-123";
        String buyerId = "11111111-1111-1111-1111-111111111111";
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);

        when(ticketRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(mock(ticket.class)));

        when(policyRepository.findPurchasePolicyByEventId(eventId)).thenReturn(null);
        when(policyRepository.findDiscountPolicyByEventId(eventId)).thenReturn(null);

        OrderDTO orderDTO = service.reserveTickets(userToken, eventId, List.of(seat));

        ActiveOrder savedOrder = new ActiveOrder(buyerId, eventId, 10);
        savedOrder.reserveTickets(List.of(seat), buyerId, List.of(false));

        when(orderRepo.findById(orderDTO.getOrderId()))
                .thenReturn(Optional.of(savedOrder));

        when(paymentGateway.charge(any(), any(), any()))
                .thenReturn(new PaymentTransaction(
                        "tx-" + UUID.randomUUID(),
                        orderDTO.getOrderId(),
                        new BigDecimal("50"),
                        PaymentTransaction.Status.SUCCESS
                ));

        when(ticketGateway.issueTickets(any(), anyList()))
                .thenReturn(List.of(new Ticketcode("code1", "qr1")));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "card-token",
                buyerId,
                "VISA"
        );

        PurchaseDTO purchase =
                service.checkout(orderDTO.getOrderId(), userToken, paymentDTO);

        assertNotNull(purchase);

        verify(paymentGateway).charge(any(), any(), any());
        verify(ticketGateway).issueTickets(any(), anyList());
        verify(purchaseRepo).save(any());
        verify(paymentService).saveTransaction(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Given payment rejected, when checkout, then exception is thrown")
    void checkout_shouldThrow_whenPaymentRejected() {

        String userToken = "token-123";
        String buyerId = "11111111-1111-1111-1111-111111111111";

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);
        order.reserveTickets(List.of(seat), buyerId, List.of(false));

        when(represnteUserService.requireMemberId(userToken))
                .thenReturn(buyerId);

        when(orderRepo.findById(orderId))
                .thenReturn(Optional.of(order));

        when(paymentGateway.charge(any(), any(), any()))
                .thenReturn(new PaymentTransaction(
                        "tx-" + UUID.randomUUID(),
                        orderId,
                        new BigDecimal("50"),
                        PaymentTransaction.Status.FAILED
                ));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "card-token",
                buyerId,
                "VISA"
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.checkout(orderId, userToken, paymentDTO)
        );
    }
}