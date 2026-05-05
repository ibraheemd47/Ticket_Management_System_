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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PolicyService;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

@SpringBootTest
class OrderAcceptanceTest {

    @Autowired
    private ActiveOrderService orderService;

    @Autowired
    private ActiveOrderRepository orderRepository;

    @MockBean
    private IPaymentGateway paymentGateway;

    @MockBean
    private ITicketSupplierGateway ticketGateway;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PurchaseRepository purchaseRepository;

    @MockBean
    private PaymentTransactionRepository paymentTransactionRepository;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private Ticket_Domain_Service ticketDomainService;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();

        when(policyService.applyGeneralDiscounts(any(), anyDouble(), anyInt()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void reserveTickets_shouldCreateActiveOrderAndReserveTickets() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(UUID.randomUUID().toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        OrderDTO result = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        assertNotNull(result);
        assertEquals("buyer1", result.getBuyerId());
    }

    @Test
    void reserveTickets_shouldFail_whenTicketAlreadyReserved() {
        UUID eventId = UUID.randomUUID();

        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(ticketId, 1L, UUID.randomUUID(), new BigDecimal("50"));

        orderService.reserveTickets("buyer1", eventId, List.of(seat));

        assertThrows(IllegalStateException.class,
                () -> orderService.reserveTickets("buyer2", eventId, List.of(seat)));
    }

    @Test
    void checkout_shouldCompletePurchase_whenPaymentAndTicketIssuanceSucceed() {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(ticketId, 1L, UUID.randomUUID(), new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction tx = new PaymentTransaction(
                "tx", order.getOrderId(), new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS);

        // ✅ FIXED HERE
        when(paymentService.charge(any(), any(), any())).thenReturn(tx);

        when(ticketGateway.issueTickets(any(), anyList()))
                .thenReturn(List.of(new Ticketcode("code1", "qr1")));

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO("token", "buyer1", "VISA");

        PurchaseDTO purchase = orderService.checkout(order.getOrderId(), "buyer1", paymentDTO);

        assertNotNull(purchase);
    }

    @Test
    void checkout_shouldFail_whenPaymentRejected() {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        SeatRequest seat = new SeatRequest(ticketId, 1L, UUID.randomUUID(), new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        PaymentTransaction tx = new PaymentTransaction(
                "tx", order.getOrderId(), new BigDecimal("50"),
                PaymentTransaction.Status.FAILED);

        // ✅ FIXED HERE
        when(paymentService.charge(any(), any(), any())).thenReturn(tx);

        PaymentDetailsDTO dto = new PaymentDetailsDTO("token", "buyer1", "VISA");

        assertThrows(IllegalStateException.class,
                () -> orderService.checkout(order.getOrderId(), "buyer1", dto));
    }

    @Test
    void getActiveOrder_shouldReturnExistingActiveOrder() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(UUID.randomUUID().toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        orderService.reserveTickets("buyer1", eventId, List.of(seat));

        OrderDTO activeOrder = orderService.getActiveOrder("buyer1", eventId);

        assertNotNull(activeOrder);
    }

    @Test
    void cancelOrder_shouldRemoveOrder() {
        UUID eventId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(UUID.randomUUID().toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        OrderDTO order = orderService.reserveTickets("buyer1", eventId, List.of(seat));

        orderService.cancelOrder(order.getOrderId(), "buyer1");

        assertThrows(IllegalStateException.class,
                () -> orderService.getActiveOrder("buyer1", eventId));
    }
}