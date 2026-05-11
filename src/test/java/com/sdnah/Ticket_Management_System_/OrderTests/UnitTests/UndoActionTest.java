package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderActionLog;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

/**
 * End-to-end unit test for the user-driven undo flow on ActiveOrderService:
 * - removeFromOrder writes an OrderActionLog entry
 * - undoLast pops it, re-adds the seat, and deletes the entry
 * - undoLast refuses gracefully when the seat was taken in the meantime
 */
class UndoActionTest {

    private ActiveOrderRepository orderRepo;
    private PurchaseRepository purchaseRepo;
    private PaymentTransactionRepository txRepo;
    private PaymentService paymentService;
    private IPaymentGateway paymentGateway;
    private ITicketSupplierGateway ticketGateway;
    private TicketRepository ticketRepository;
    private PolicyRepository policyRepository;
    private OrderPolicyDomainService policyDomainService;
    private IrepresnteUserService userService;
    private OrderActionLogRepository actionLogRepo;
    private ActiveOrderService service;

    private final String USER_TOKEN = "tok";
    private final String BUYER_ID = "buyer-1";
    private final UUID EVENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderRepo = mock(ActiveOrderRepository.class);
        purchaseRepo = mock(PurchaseRepository.class);
        txRepo = mock(PaymentTransactionRepository.class);
        paymentService = mock(PaymentService.class);
        paymentGateway = mock(IPaymentGateway.class);
        ticketGateway = mock(ITicketSupplierGateway.class);
        ticketRepository = mock(TicketRepository.class);
        policyRepository = mock(PolicyRepository.class);
        policyDomainService = mock(OrderPolicyDomainService.class);
        userService = mock(IrepresnteUserService.class);
        actionLogRepo = mock(OrderActionLogRepository.class);

        service = new ActiveOrderService(
                orderRepo, purchaseRepo, txRepo, paymentService, paymentGateway, ticketGateway,
                ticketRepository, policyRepository, policyDomainService, userService,
                actionLogRepo);

        when(userService.requireMemberId(USER_TOKEN)).thenReturn(BUYER_ID);
    }

    private ActiveOrder orderWithOneItem(String ticketId) {
        ActiveOrder order = new ActiveOrder(BUYER_ID, EVENT_ID, 30);
        Lock lock = new Lock(ticketId, BUYER_ID, java.time.LocalDateTime.now().plusMinutes(10));
        order.addTicket(ticketId, 7L, UUID.randomUUID(), new BigDecimal("50"), lock);
        return order;
    }

    // -------------------------------------------------------------------------
    // removeFromOrder writes an action-log entry that captures the removed item
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given a user removes a ticket, when removeFromOrder runs, then an OrderActionLog is persisted")
    void removeFromOrder_PersistsActionLog() {
        String ticketId = UUID.randomUUID().toString();
        ActiveOrder order = orderWithOneItem(ticketId);
        OrderItem item = order.getItems().get(0);
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(ticketRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        service.removeFromOrder(order.getId(), item.getItemId(), USER_TOKEN);

        ArgumentCaptor<OrderActionLog> captor = ArgumentCaptor.forClass(OrderActionLog.class);
        verify(actionLogRepo, times(1)).save(captor.capture());
        OrderActionLog persisted = captor.getValue();

        assertThat(persisted.getType()).isEqualTo(OrderActionLog.ActionType.REMOVE_TICKET);
        assertThat(persisted.getOrderId()).isEqualTo(order.getId());
        assertThat(persisted.getTicketId()).isEqualTo(ticketId);
        assertThat(persisted.getSeatId()).isEqualTo(7L);
        assertThat(persisted.getPrice()).isEqualByComparingTo("50");
        assertThat(order.getItems()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // undoLast(REMOVE_TICKET) re-adds the seat and deletes the log entry
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given the last action was REMOVE_TICKET, when undoLast runs, then the seat is re-added and the log entry is deleted")
    void undoLast_OnRemoveTicket_ReAddsSeatAndDeletesLog() {
        String ticketId = UUID.randomUUID().toString();
        // Start with the order in its post-remove state (item gone).
        ActiveOrder order = new ActiveOrder(BUYER_ID, EVENT_ID, 30);
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepo.isTicketLocked(ticketId)).thenReturn(false);
        when(ticketRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        OrderItem removed = new OrderItem(ticketId, 7L, UUID.randomUUID(), new BigDecimal("50"));
        OrderActionLog log = OrderActionLog.forRemovedTicket(order.getId(), removed);
        // Give the entity an id so deleteById is callable. (Field is package-private
        // for tests — use reflection since the field is private.)
        setId(log, 99L);

        when(actionLogRepo.findTopByOrderIdOrderByIdDesc(order.getId()))
                .thenReturn(Optional.of(log));

        OrderDTO dto = service.undoLast(order.getId(), USER_TOKEN);

        assertThat(order.getItems()).hasSize(1);
        OrderItem reAdded = order.getItems().get(0);
        assertThat(reAdded.getTicketId()).isEqualTo(ticketId);
        assertThat(reAdded.getSeatId()).isEqualTo(7L);
        assertThat(reAdded.getPrice()).isEqualByComparingTo("50");
        assertThat(reAdded.getLock()).isNotNull();
        assertThat(reAdded.getLock().getOwnerId()).isEqualTo(BUYER_ID);

        verify(actionLogRepo).deleteById(99L);
        verify(orderRepo).save(order);
        verify(policyDomainService).applyDiscountPolicy(eq(order), any(), any());
        assertThat(dto).isNotNull();
    }

    // -------------------------------------------------------------------------
    // undoLast fails cleanly when nothing was logged
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given no logged actions, when undoLast runs, then 'Nothing to undo' is thrown")
    void undoLast_NoLoggedActions_Throws() {
        ActiveOrder order = orderWithOneItem(UUID.randomUUID().toString());
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(actionLogRepo.findTopByOrderIdOrderByIdDesc(order.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.undoLast(order.getId(), USER_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nothing to undo");

        verify(actionLogRepo, never()).deleteById(any());
        verify(orderRepo, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // undoLast refuses when the seat was claimed in the meantime
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given the seat was claimed by someone else, when undoLast runs, then it refuses and leaves the log entry intact")
    void undoLast_SeatTaken_RefusesAndKeepsLog() {
        String ticketId = UUID.randomUUID().toString();
        ActiveOrder order = new ActiveOrder(BUYER_ID, EVENT_ID, 30);
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepo.isTicketLocked(ticketId)).thenReturn(true);

        OrderItem removed = new OrderItem(ticketId, 7L, UUID.randomUUID(), new BigDecimal("50"));
        OrderActionLog log = OrderActionLog.forRemovedTicket(order.getId(), removed);
        setId(log, 7L);
        when(actionLogRepo.findTopByOrderIdOrderByIdDesc(order.getId()))
                .thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.undoLast(order.getId(), USER_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("seat no longer available");

        verify(actionLogRepo, never()).deleteById(any());
        verify(orderRepo, never()).save(any());
        assertThat(order.getItems()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Reflection helper: the entity id is JPA-generated and otherwise unsettable
    // in tests. Fine to poke it for these unit tests.
    // -------------------------------------------------------------------------
    private static void setId(OrderActionLog log, long id) {
        try {
            var f = OrderActionLog.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(log, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}