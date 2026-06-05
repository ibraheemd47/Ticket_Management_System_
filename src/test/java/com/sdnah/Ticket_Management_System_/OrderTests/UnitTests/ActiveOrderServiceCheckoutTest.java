package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

class ActiveOrderServiceCheckoutTest {

    @Test
    void checkout_SavesPurchasePaymentOrder_AndTicketIsNoLongerAvailable() {
        ActiveOrderRepository orderRepo = mock(ActiveOrderRepository.class);
        PurchaseRepository purchaseRepo = mock(PurchaseRepository.class);
        PaymentTransactionRepository transactionRepo = mock(PaymentTransactionRepository.class);
        PaymentService paymentService = mock(PaymentService.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        ITicketSupplierGateway ticketGateway = mock(ITicketSupplierGateway.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        PolicyRepository policyRepo = mock(PolicyRepository.class);
        IrepresnteUserService userService = mock(IrepresnteUserService.class);
        OrderActionLogRepository actionLogRepo = mock(OrderActionLogRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        LotteryRepository lotteryRepo = mock(LotteryRepository.class);
        IEventRepository eventRepo = mock(IEventRepository.class);

        ActiveOrderService service = new ActiveOrderService(
                orderRepo,
                notificationService,
                purchaseRepo,
                transactionRepo,
                paymentService,
                paymentGateway,
                ticketGateway,
                ticketRepo,
                policyRepo,
                userService,
                actionLogRepo,lotteryRepo,eventRepo
        );

        String userToken = "token-1";
        String buyerId = UUID.randomUUID().toString();
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 30);

        Area area = new Area("GA");
        Lock lock = new Lock(ticketId.toString(), buyerId, order.getExpiresAt());

        order.addTicket(
                ticketId.toString(),
                1L,
                area.getId(),
                BigDecimal.TEN,
                lock
        );

        ticket realTicket = new ticket(
                ticketId,
                eventId,
                area,
                new Date(),
                BigDecimal.TEN
        );

        realTicket.lockInCart(UUID.fromString(buyerId));

        PaymentTransaction tx = new PaymentTransaction(
                "tx-1",
                order.getId(),
                BigDecimal.TEN,
                PaymentTransaction.Status.SUCCESS
        );

        when(userService.requireMemberId(userToken)).thenReturn(buyerId);
        //NEW by yara:
        Member mockBuyer = mock(Member.class);
        when(mockBuyer.getAge()).thenReturn(25);
        when(userService.requireMember(userToken)).thenReturn(mockBuyer);

        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(realTicket));

        when(paymentGateway.charge(eq(order.getId()), any(BigDecimal.class), any()))
                .thenReturn(tx);

        when(ticketGateway.issueTickets(eq(order.getId()), anyList()))
                .thenReturn(List.of(new Ticketcode("CODE-1", "QR-1")));

        PaymentDetailsDTO paymentDTO = mock(PaymentDetailsDTO.class);
        when(paymentDTO.getCardToken()).thenReturn("card-token");
        when(paymentDTO.getBillingName()).thenReturn("Buyer Name");
        when(paymentDTO.getPaymentMethod()).thenReturn("CREDIT_CARD");

        PurchaseDTO result = service.checkout(order.getId(), userToken, paymentDTO);

        assertThat(result).isNotNull();

        assertThat(order.getStatus())
                .isEqualTo(ActiveOrder.Status.COMPLETED);

        assertThat(realTicket.getStatus())
                .isEqualTo(ticket.TicketStatus.PURCHASED);

        assertThat(realTicket.getStatus())
                .isNotEqualTo(ticket.TicketStatus.AVAILABLE);

        verify(purchaseRepo, times(1)).save(any());
        verify(paymentService, times(1)).saveTransaction(tx);
        verify(orderRepo, atLeastOnce()).save(order);
        verify(ticketRepo, times(1)).save(realTicket);
        verify(paymentGateway, never()).refund(any());
    }
}