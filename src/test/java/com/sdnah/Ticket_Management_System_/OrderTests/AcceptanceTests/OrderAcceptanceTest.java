package com.sdnah.Ticket_Management_System_.OrderTests.AcceptanceTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderRepositoryImpl;

class OrderAcceptanceTest {

    private OrderRepositoryImpl orderRepository;
    private IPaymentGateway paymentGateway;
    private ITicketSupplierGateway ticketGateway;
    private PolicyService policyService;
    private PaymentService paymentService;
    private ActiveOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepositoryImpl();

        paymentGateway = mock(IPaymentGateway.class);
        ticketGateway = mock(ITicketSupplierGateway.class);

        policyService = new PolicyService() {
            @Override
            public double applyGeneralDiscounts(UUID eventId, double total, int itemCount) {
                return total;
            }

            @Override
            public double calculateCouponDiscount(UUID eventId, double currentPrice, int itemCount, String couponCode) {
                return currentPrice;
            }
        };

        paymentService = new PaymentService(paymentGateway, orderRepository);
        orderService = new ActiveOrderService(orderRepository, paymentService, ticketGateway, policyService);
    }

    @Test//this test makes sure that the active order is executed in the right way 
    void reserveTickets_shouldCreateActiveOrderAndReserveTickets() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderDTO result = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        assertNotNull(result);
        assertEquals("buyer1", result.getBuyerId());
        assertEquals(eventId, result.getEventId());
        assertEquals(1, result.getItems().size());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test//checks the option for trying to reserve a ticket already reserved,and not get it through
    void reserveTickets_shouldFail_whenTicketAlreadyReserved() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        orderService.reserveTickets("buyer1", eventId, List.of(seat));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.reserveTickets("buyer2", eventId, List.of(seat))
        );

        assertEquals("Ticket already reserved: ticket1", exception.getMessage());
    }

    @Test//verifies the successful checkout flow confirms that a purchase is created, linked to the original order, contains the issued ticket code, and has the correct final price.
    void checkout_shouldCompletePurchase_whenPaymentAndTicketIssuanceSucceed() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction Transaction = new PaymentTransaction(
                "tx-success",
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), any(PaymentDetails.class)))
                .thenReturn(Transaction);

        when(ticketGateway.issueTickets(any(UUID.class), anyList()))
                .thenReturn(List.of(new Ticketcode("code1", "qr1")));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "token123",
                "buyer1",
                "VISA"
        );

        PurchaseDTO purchase = orderService.checkout(order.getOrderId(), "buyer1", paymentDTO);

        assertNotNull(purchase);
        assertEquals(order.getOrderId(), purchase.getOrderId());
        assertEquals(List.of("code1"), purchase.getTicketCodes());
        assertEquals(new BigDecimal("50"), purchase.getFinalPrice());
    }

    @Test//the payment didnt work so the checkout dosent happen
    void checkout_shouldFail_whenPaymentRejected() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction failedTransaction = new PaymentTransaction(
                "tx-failed",
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.FAILED
        );

        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), any(PaymentDetails.class)))
                .thenReturn(failedTransaction);

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "token123",
                "buyer1",
                "VISA"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.checkout(order.getOrderId(), "buyer1", paymentDTO)
        );

        assertEquals("Payment failed", ex.getMessage());
    }

    @Test//the system does refund if the transaction worked but doing the ticket failed
    void checkout_shouldRefund_whenTicketIssuanceFails() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction successTx = new PaymentTransaction(
                "tx-success",
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        PaymentTransaction refundTx = new PaymentTransaction(
                "tx-success",
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.REFUNDED
        );

        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), any(PaymentDetails.class)))
                .thenReturn(successTx);

        when(ticketGateway.issueTickets(any(UUID.class), anyList()))
                .thenThrow(new RuntimeException("issuance failed"));

        when(paymentGateway.refund("tx-success"))
                .thenReturn(refundTx);

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "token123",
                "buyer1",
                "VISA"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.checkout(order.getOrderId(), "buyer1", paymentDTO)
        );

        assertEquals("Ticket issuance failed, payment refunded", ex.getMessage());
        verify(paymentGateway).refund("tx-success");
    }
    @Test//prevents the user to make 2 active orders for the same event
    void reserveTickets_shouldFail_whenActiveOrderAlreadyExists() {
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

        orderService.reserveTickets("buyer1", eventId, List.of(seat1));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.reserveTickets("buyer1", eventId, List.of(seat2))
        );

        assertEquals("Active order already exists", ex.getMessage());
    }
   @Test//the idea from this that after the user booked a ticket he can go back and see his active order
    void getActiveOrder_shouldReturnExistingActiveOrder() {
    UUID eventId = UUID.randomUUID();

    SeatRequest seat = new SeatRequest(
            "ticket1",
            1L,
            UUID.randomUUID(),
            new BigDecimal("50")
    );

    orderService.reserveTickets("buyer1", eventId, List.of(seat));

    OrderDTO activeOrder = orderService.getActiveOrder("buyer1", eventId);

    assertNotNull(activeOrder);
    assertEquals("buyer1", activeOrder.getBuyerId());
    assertEquals(eventId, activeOrder.getEventId());
    assertEquals(1, activeOrder.getItems().size());
    }
    @Test//cancels the order and checks that the system cant get active order after deleting it 
    void cancelOrder_shouldRemoveOrder() {
    UUID eventId = UUID.randomUUID();

    SeatRequest seat = new SeatRequest(
            "ticket1",
            1L,
            UUID.randomUUID(),
            new BigDecimal("50")
    );

    OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

    orderService.cancelOrder(order.getOrderId(), "buyer1");

    IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orderService.getActiveOrder("buyer1", eventId)
    );
    assertEquals("No active order", exception.getMessage());
   }
    

    @Test//after the user does a checkot it will be added to purchase history 
    void getPurchaseHistory_shouldReturnCompletedPurchasesForBuyer() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction tx = new PaymentTransaction(
                "tx-success",
                order.getOrderId(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), any(PaymentDetails.class)))
                .thenReturn(tx);

        when(ticketGateway.issueTickets(any(UUID.class), anyList()))
                .thenReturn(List.of(new Ticketcode("code1", "qr1")));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "token123",
                "buyer1",
                "VISA"
        );

        orderService.checkout(order.getOrderId(), "buyer1", paymentDTO);

        List<PurchaseDTO> history = orderService.getPurchaseHistory("buyer1");

        assertEquals(1, history.size());
    }
    @Test//if the oder expires the user cant checkout
    void checkout_shouldFail_whenActiveOrderExpired() {
    UUID eventId = UUID.randomUUID();

    SeatRequest seat = new SeatRequest(
            "ticket1",
            1L,
            UUID.randomUUID(),
            new BigDecimal("50")
    );

    OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

    ActiveOrder domainOrder = orderRepository.findById(order.getOrderId()).get();
    domainOrder.markExpired();

    PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
            "token123",
            "buyer1",
            "VISA"
    );

    IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orderService.checkout(order.getOrderId(), "buyer1", paymentDTO)
    );

    assertEquals("Order expired", ex.getMessage());
    }
}