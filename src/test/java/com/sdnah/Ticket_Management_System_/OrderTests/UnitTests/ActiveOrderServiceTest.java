package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;
// import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PolicyService;

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
    @Mock
    private NotificationService notificationService;
    // @Mock private PolicyService policyService;
    @Mock 
    private LotteryRepository lotteryRepo;
    @Mock
    private IEventRepository eventRepo;

    private ActiveOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

         // NEW by yara:
        Member mockBuyer = mock(Member.class);
        when(mockBuyer.getAge()).thenReturn(25);
        when(represnteUserService.requireMember(any())).thenReturn(mockBuyer);

        
        
        service = new ActiveOrderService(
                orderRepo,
                notificationService,
                purchaseRepo,
                txRepo,
                paymentService,
                paymentGateway,
                ticketGateway,
                ticketRepository,
                policyRepository,
                represnteUserService,
                actionLogRepo,lotteryRepo,eventRepo);
    }

    @Test
    @DisplayName("Given available tickets, when reserving tickets, then active order is created")
    void reserveTickets_shouldCreateOrder_whenTicketsAvailable() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

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
    // @Disabled("Behavior changed: existing active order now adds tickets to same
    // order")
    // @Test
    // @DisplayName("Given active order already exists, when reserving tickets, then
    // exception is thrown")
    // void reserveTickets_shouldThrow_whenActiveOrderExists() {
    // String userToken = "token-123";
    // String buyerId = UUID.randomUUID().toString();
    // UUID eventId = UUID.randomUUID();

    // when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
    // when(orderRepo.findActiveOrder(buyerId, eventId))
    // .thenReturn(Optional.of(new ActiveOrder(buyerId, eventId, 10)));

    // IllegalStateException ex = assertThrows(IllegalStateException.class,
    // () -> service.reserveTickets(userToken, eventId, List.of()));

    // assertEquals("Active order already exists", ex.getMessage());
    // verify(represnteUserService).requireMemberId(userToken);
    // verify(orderRepo).findActiveOrder(buyerId, eventId);
    // }
    @Test
    @DisplayName("Given active order already exists, when reserving another ticket, then ticket is added to existing order")
    void reserveTickets_shouldAddTicketToExistingOrder_whenActiveOrderExists() {
        String userToken = "token-123";
        String buyerId = UUID.randomUUID().toString();
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        ActiveOrder existingOrder = new ActiveOrder(buyerId, eventId, 10);

        SeatRequest seat = new SeatRequest(
                ticketId.toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"));

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.of(existingOrder));
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        when(policyRepository.findPurchasePolicyByEventId(eventId)).thenReturn(null);
        when(policyRepository.findDiscountPolicyByEventId(eventId)).thenReturn(null);

        OrderDTO result = service.reserveTickets(userToken, eventId, List.of(seat));

        assertEquals(buyerId, result.getbuyerId());
        assertEquals(eventId, result.getEventId());
        assertEquals(1, result.getItems().size());

        verify(represnteUserService).requireMemberId(userToken);
        verify(orderRepo).findActiveOrder(buyerId, eventId);
        verify(orderRepo).isTicketLocked(ticketId.toString());
        verify(orderRepo).save(existingOrder);
    }

    @Test
    @DisplayName("Given ticket already locked, when reserving tickets, then exception is thrown")
    void reserveTickets_shouldThrow_whenTicketAlreadyLocked() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.reserveTickets(userToken, eventId, List.of(seat)));

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

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(orderRepo).save(any(ActiveOrder.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.reserveTickets(userToken, eventId, List.of(seat)));
    }

    // ── Guest token support ───────────────────────────────────────────────────

    @Test
    @DisplayName("Guest token: reserveTickets uses UUID from token as buyerId without calling requireMemberId")
    void reserveTickets_guestToken_usesBuyerIdFromToken() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        String expectedBuyerId = guestUuid.toString();
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        when(orderRepo.findActiveOrder(expectedBuyerId, eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        when(policyRepository.findPurchasePolicyByEventId(eventId)).thenReturn(null);
        when(policyRepository.findDiscountPolicyByEventId(eventId)).thenReturn(null);

        OrderDTO result = service.reserveTickets(guestToken, eventId, List.of(seat));

        assertEquals(expectedBuyerId, result.getbuyerId());
        // requireMemberId must NOT be called for guest tokens
        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
    }

    @Test
    @DisplayName("Guest token: ticket already locked throws exception (same as member)")
    void reserveTickets_guestToken_ticketLocked_throws() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));

        when(orderRepo.findActiveOrder(guestUuid.toString(), eventId)).thenReturn(Optional.empty());
        when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.reserveTickets(guestToken, eventId, List.of(seat)));

        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: valid order is cancelled and locks are released")
    void cancelOrder_validOrder_cancelsSuccessfully() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        service.cancelOrder(orderId, userToken);

        verify(orderRepo).save(order);
    }

    @Test
    @DisplayName("cancelOrder: order not found throws exception")
    void cancelOrder_orderNotFound_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.cancelOrder(orderId, userToken));
    }

    @Test
    @DisplayName("cancelOrder: guest token cancels order without calling requireMemberId")
    void cancelOrder_guestToken_cancelsWithoutMemberLookup() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        String buyerId = guestUuid.toString();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        service.cancelOrder(orderId, guestToken);

        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
        verify(orderRepo).save(order);
    }

    // ── getActiveOrder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveOrder: existing order is returned as DTO")
    void getActiveOrder_orderExists_returnsDTO() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.of(order));

        OrderDTO result = service.getActiveOrder(userToken, eventId);

        assertEquals(buyerId, result.getbuyerId());
        assertEquals(eventId, result.getEventId());
    }

    @Test
    @DisplayName("getActiveOrder: no active order throws exception")
    void getActiveOrder_noOrder_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID eventId = UUID.randomUUID();

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.getActiveOrder(userToken, eventId));
    }

    // ── getPurchaseHistory ────────────────────────────────────────────────────

    @Test
    @DisplayName("getPurchaseHistory: guest token is rejected — member only")
    void getPurchaseHistory_guestToken_throws() {
        String guestToken = "GUEST_" + UUID.randomUUID();

        // requireMemberId should throw for guest token since getPurchaseHistory calls it directly
        when(represnteUserService.requireMemberId(guestToken))
                .thenThrow(new RuntimeException("Invalid or expired token"));

        assertThrows(RuntimeException.class,
                () -> service.getPurchaseHistory(guestToken));
    }

    @Test
    @DisplayName("getPurchaseHistory: member token returns purchase list")
    void getPurchaseHistory_memberToken_returnsList() {
        String userToken = "token-123";
        String buyerId = "buyer1";

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(purchaseRepo.findByBuyerId(buyerId)).thenReturn(List.of());

        List<?> result = service.getPurchaseHistory(userToken);

        assertEquals(0, result.size());
        verify(represnteUserService).requireMemberId(userToken);
        verify(purchaseRepo).findByBuyerId(buyerId);
    }

    // ── removeFromOrder ───────────────────────────────────────────────────────

    // @Test
    // @DisplayName("removeFromOrder: item removed from order and action logged")
    // void removeFromOrder_validItem_removesAndLogs() {
    //     String userToken = "token-123";
    //     String buyerId = "buyer1";
    //     UUID orderId = UUID.randomUUID();
    //     UUID itemId = UUID.randomUUID();

    //     ActiveOrder order = mock(ActiveOrder.class);
    //     OrderItem mockItem = mock(OrderItem.class);

    //     when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
    //     when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
    //     when(order.isOwnedBy(buyerId)).thenReturn(true);
    //     when(order.isExpired()).thenReturn(false);
    //     when(order.removeTicket(any(UUID.class))).thenReturn(mockItem);
    //     when(order.getId()).thenReturn(orderId);

    //     service.removeFromOrder(orderId, itemId, userToken);

    //     verify(order).removeTicket(itemId);
    //     verify(actionLogRepo).save(any());
    //     verify(orderRepo).save(order);
    // }
    //start target\site\jacoco\index.html 

    @Test
    @DisplayName("removeFromOrder: order not owned by buyer throws exception")
    void removeFromOrder_notOwner_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder("other-buyer", eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(Exception.class,
                () -> service.removeFromOrder(orderId, UUID.randomUUID(), userToken));
    }

    // ── undoLast ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("undoLast: no action log entry throws 'Nothing to undo'")
    void undoLast_noActionLog_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(actionLogRepo.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.undoLast(orderId, userToken));

        assertEquals("Nothing to undo", ex.getMessage());
    }

    // ── getPendingOrdersByBuyer ───────────────────────────────────────────────

    @Test
    @DisplayName("getPendingOrdersByBuyer: member token returns list from repo")
    void getPendingOrdersByBuyer_memberToken_returnsList() {
        String userToken = "token-123";
        String buyerId = "buyer1";

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findPendingOrdersByBuyer(buyerId)).thenReturn(List.of());

        List<OrderDTO> result = service.getPendingOrdersByBuyer(userToken);

        assertEquals(0, result.size());
        verify(orderRepo).findPendingOrdersByBuyer(buyerId);
    }

    @Test
    @DisplayName("getPendingOrdersByBuyer: guest token resolves UUID as buyerId")
    void getPendingOrdersByBuyer_guestToken_resolvesUuid() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        String expectedBuyerId = guestUuid.toString();

        when(orderRepo.findPendingOrdersByBuyer(expectedBuyerId)).thenReturn(List.of());

        List<OrderDTO> result = service.getPendingOrdersByBuyer(guestToken);

        assertEquals(0, result.size());
        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
        verify(orderRepo).findPendingOrdersByBuyer(expectedBuyerId);
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById: returns DTO for valid owner")
    void getOrderById_validOwner_returnsDTO() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        OrderDTO result = service.getOrderById(orderId, userToken);

        assertEquals(buyerId, result.getbuyerId());
        assertEquals(eventId, result.getEventId());
    }

    @Test
    @DisplayName("getOrderById: order belonging to another buyer throws exception")
    void getOrderById_wrongOwner_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder("other-buyer", eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(Exception.class,
                () -> service.getOrderById(orderId, userToken));
    }

    @Test
    @DisplayName("getOrderById: order not found throws exception")
    void getOrderById_notFound_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.getOrderById(orderId, userToken));
    }

    // ── Constructor validation ─────────────────────────────────────────────────

    @Test
    @DisplayName("Constructor: null orderRepo throws IllegalArgumentException")
    void constructor_nullOrderRepo_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(null, notificationService, purchaseRepo, txRepo,
                    paymentService, paymentGateway, ticketGateway, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null purchaseRepo throws IllegalArgumentException")
    void constructor_nullPurchaseRepo_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, notificationService, null, txRepo,
                    paymentService, paymentGateway, ticketGateway, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null paymentGateway throws IllegalArgumentException")
    void constructor_nullPaymentGateway_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, notificationService, purchaseRepo, txRepo,
                    paymentService, null, ticketGateway, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null ticketGateway throws IllegalArgumentException")
    void constructor_nullTicketGateway_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, notificationService, purchaseRepo, txRepo,
                    paymentService, paymentGateway, null, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null represnteUserService throws IllegalArgumentException")
    void constructor_nullUserService_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, notificationService, purchaseRepo, txRepo,
                    paymentService, paymentGateway, ticketGateway, ticketRepository,
                    policyRepository, null, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null paymentService throws IllegalArgumentException")
    void constructor_nullPaymentService_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, notificationService, purchaseRepo, txRepo,
                    null, paymentGateway, ticketGateway, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: null notificationService throws IllegalArgumentException")
    void constructor_nullNotificationService_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ActiveOrderService(orderRepo, null, purchaseRepo, txRepo,
                    paymentService, paymentGateway, ticketGateway, ticketRepository,
                    policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo));
    }

    @Test
    @DisplayName("Constructor: all valid dependencies — service created successfully")
    void constructor_allValid_createsService() {
        ActiveOrderService s = new ActiveOrderService(
                orderRepo, notificationService, purchaseRepo, txRepo,
                paymentService, paymentGateway, ticketGateway, ticketRepository,
                policyRepository, represnteUserService, actionLogRepo, lotteryRepo, eventRepo);
        assertThrows(Exception.class,
                () -> s.getActiveOrder("token", UUID.randomUUID())); // proves service is wired
    }

    // ── applyCoupon ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("applyCoupon: invalid coupon code throws IllegalStateException")
    void applyCoupon_invalidCoupon_throwsIllegalState() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // policyRepository returns null → applyDiscounts throws IllegalArgumentException
        // → service wraps it as IllegalStateException("Coupon is not valid")
        assertThrows(IllegalStateException.class,
                () -> service.applyCoupon(orderId, userToken, "INVALID"));
    }

    @Test
    @DisplayName("applyCoupon: guest token resolves buyerId without calling requireMemberId")
    void applyCoupon_guestToken_resolvesBuyerIdFromToken() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        String buyerId = guestUuid.toString();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // coupon will fail (no policy), but the important thing is no requireMemberId call
        assertThrows(Exception.class,
                () -> service.applyCoupon(orderId, guestToken, "COUPON"));

        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
    }

    // ── getActiveOrdersCount / getPurchasesTodayCount ─────────────────────────

    @Test
    @DisplayName("getActiveOrdersCount: delegates to orderRepo.countActiveOrders")
    void getActiveOrdersCount_delegatesToRepo() {
        when(orderRepo.countActiveOrders()).thenReturn(7L);

        int count = service.getActiveOrdersCount();

        assertEquals(7, count);
        verify(orderRepo).countActiveOrders();
    }

    @Test
    @DisplayName("getActiveOrdersCount: returns 0 when no active orders")
    void getActiveOrdersCount_noOrders_returnsZero() {
        when(orderRepo.countActiveOrders()).thenReturn(0L);

        assertEquals(0, service.getActiveOrdersCount());
    }

    @Test
    @DisplayName("getPurchasesTodayCount: delegates to purchaseRepo.countPurchasesSince")
    void getPurchasesTodayCount_delegatesToRepo() {
        when(purchaseRepo.countPurchasesSince(any())).thenReturn(3L);

        int count = service.getPurchasesTodayCount();

        assertEquals(3, count);
        verify(purchaseRepo).countPurchasesSince(any());
    }

    // ── addTicketToOrder ──────────────────────────────────────────────────────

    @Test
    @DisplayName("addTicketToOrder: locked ticket throws IllegalStateException")
    void addTicketToOrder_lockedTicket_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));
        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepo.isTicketLocked(seat.getTicketId())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.addTicketToOrder(orderId, userToken, seat));
    }

    @Test
    @DisplayName("addTicketToOrder: order not owned by buyer throws exception")
    void addTicketToOrder_wrongOwner_throws() {
        String userToken = "token-123";
        String buyerId = "buyer1";
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder("other-buyer", eventId, 10);

        when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(Exception.class,
                () -> service.addTicketToOrder(orderId, userToken,
                        new SeatRequest(UUID.randomUUID().toString(), 1L, UUID.randomUUID(), new BigDecimal("50"))));
    }

    @Test
    @DisplayName("addTicketToOrder: guest token resolves buyerId without requireMemberId")
    void addTicketToOrder_guestToken_resolvesBuyerIdFromToken() {
        UUID guestUuid = UUID.randomUUID();
        String guestToken = "GUEST_" + guestUuid;
        String buyerId = guestUuid.toString();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SeatRequest seat = new SeatRequest(ticketId.toString(), 1L, UUID.randomUUID(), new BigDecimal("50"));
        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepo.isTicketLocked(seat.getTicketId())).thenReturn(false);

        service.addTicketToOrder(orderId, guestToken, seat);

        verify(represnteUserService, org.mockito.Mockito.never()).requireMemberId(any());
    }
}
