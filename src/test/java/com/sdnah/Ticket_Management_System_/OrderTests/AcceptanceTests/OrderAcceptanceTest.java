package com.sdnah.Ticket_Management_System_.OrderTests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@ActiveProfiles("test")
@Import(ActiveOrderService.class)
class OrderAcceptanceTest {

    @Autowired
    private ActiveOrderService orderService;

    @Autowired
    private ActiveOrderRepository orderRepository;

    @MockBean
    private IrepresnteUserService represnteUserService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ITicketSupplierGateway ticketGateway;

    @MockBean
    private PurchaseRepository purchaseRepository;

    @MockBean
    private PaymentTransactionRepository paymentTransactionRepository;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private PolicyRepository policyRepository;

    @MockBean
    private OrderPolicyDomainService orderPolicyDomainService;

    private static final String TOKEN_BUYER_1 = "token-buyer-1";
    private static final String TOKEN_BUYER_2 = "token-buyer-2";
    private static final String BUYER_1 = "buyer1";
    private static final String BUYER_2 = "buyer2";

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();

        when(represnteUserService.requireMemberId(TOKEN_BUYER_1)).thenReturn(BUYER_1);
        when(represnteUserService.requireMemberId(TOKEN_BUYER_2)).thenReturn(BUYER_2);

        when(policyRepository.findPurchasePolicyByEventId(any())).thenReturn(null);
        when(policyRepository.findDiscountPolicyByEventId(any())).thenReturn(null);

        when(ticketRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Given available tickets, when reserving tickets, then active order is created")
    void reserveTickets_shouldCreateActiveOrderAndReserveTickets() {
        // Arrange
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        // Act
        OrderDTO result = orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        // Assert
        assertNotNull(result);
        assertEquals(BUYER_1, result.getbuyerId());
        assertEquals(eventId, result.getEventId());

        verify(represnteUserService).requireMemberId(TOKEN_BUYER_1);
        verify(orderPolicyDomainService).validatePurchasePolicy(any(), isNull());
        verify(orderPolicyDomainService).applyDiscountPolicy(any(), isNull(), isNull());
    }

    @Test
    @DisplayName("Given same ticket already reserved, when another buyer reserves it, then exception is thrown")
    void reserveTickets_shouldFail_whenTicketAlreadyReserved() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(
                ticketId,
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> orderService.reserveTickets(TOKEN_BUYER_2, eventId, List.of(seat)));
    }

    @Test
    @DisplayName("Given payment and ticket issuance succeed, when checking out, then purchase is completed")
    void checkout_shouldCompletePurchase_whenPaymentAndTicketIssuanceSucceed() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(
                ticketId,
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        PaymentTransaction tx = new PaymentTransaction(
                "tx-" + UUID.randomUUID(),
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS);

        when(paymentService.charge(any(), any(), any())).thenReturn(tx);

        when(ticketGateway.issueTickets(any(), anyList()))
                .thenReturn(List.of(new Ticketcode("code1", "qr1")));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO("card-token", "buyer1", "VISA");

        // Act
        PurchaseDTO purchase = orderService.checkout(order.getOrderId(), TOKEN_BUYER_1, paymentDTO);

        // Assert
        assertNotNull(purchase);
        verify(paymentService).charge(any(), any(), any());
        verify(ticketGateway).issueTickets(any(), anyList());
        verify(purchaseRepository).save(any());
        verify(paymentTransactionRepository).save(tx);
    }

    @Test
    @DisplayName("Given payment rejected, when checking out, then checkout fails")
    void checkout_shouldFail_whenPaymentRejected() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(
                ticketId,
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        PaymentTransaction tx = new PaymentTransaction(
                "tx-" + UUID.randomUUID(),
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.FAILED);

        when(paymentService.charge(any(), any(), any())).thenReturn(tx);

        PaymentDetailsDTO dto = new PaymentDetailsDTO("card-token", "buyer1", "VISA");

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> orderService.checkout(order.getOrderId(), TOKEN_BUYER_1, dto));
    }

    @Test
    @DisplayName("Given active order exists, when fetching active order, then it is returned")
    void getActiveOrder_shouldReturnExistingActiveOrder() {
        // Arrange
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        // Act
        OrderDTO activeOrder = orderService.getActiveOrder(TOKEN_BUYER_1, eventId);

        // Assert
        assertNotNull(activeOrder);
        assertEquals(BUYER_1, activeOrder.getbuyerId());
        assertEquals(eventId, activeOrder.getEventId());
    }

    @Test
    @DisplayName("Given active order exists, when cancelling order, then order is no longer active")
    void cancelOrder_shouldRemoveOrder() {
        // Arrange
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets(TOKEN_BUYER_1, eventId, List.of(seat));

        // Act
        orderService.cancelOrder(order.getOrderId(), TOKEN_BUYER_1);

        // Assert
        assertThrows(IllegalStateException.class,
                () -> orderService.getActiveOrder(TOKEN_BUYER_1, eventId));
    }
}