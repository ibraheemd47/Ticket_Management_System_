package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderActionLog;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;

@Service
@Transactional
public class ActiveOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderService.class);
    private static final int TTL_MINUTES = 10;
    private final ActiveOrderRepository orderRepo;
    private final PurchaseRepository purchaseRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final PaymentService paymentService;
    private final ITicketSupplierGateway ticketGateway;
    private final Ticket_Domain_Service ticketDomainService;
    private final OrderPolicyDomainService orderPolicyDomainService;
    private final CheckoutDomainService checkoutDomainService;
    private final OrderActionLogRepository actionLogRepo;
    private IrepresnteUserService represnteUserService;
    private final NotificationService notificationService;
    // עולה בכל שריון מוצלח, מתאפס כל דקה
    private final AtomicInteger reservationsLastMinute = new AtomicInteger(0);
    // snapshot של הדקה הקודמת — זה מה שה-UI מציג
    private final AtomicInteger reservationRateSnapshot = new AtomicInteger(0);

    @Autowired  
    private LotteryRepository lotteryRepository;

    public ActiveOrderService(ActiveOrderRepository orderRepo,
            NotificationService notificationService,
            PurchaseRepository purchaseRepo,
            PaymentTransactionRepository transactionRepo,
            PaymentService paymentService,
            IPaymentGateway paymentGateway,
            ITicketSupplierGateway ticketGateway,
            TicketRepository ticketRepository,
            PolicyRepository policyRepository,
            IrepresnteUserService represnteUserService,
            OrderActionLogRepository actionLogRepo) {
        if (orderRepo == null)
            throw new IllegalArgumentException("orderRepo required");
        if (purchaseRepo == null)
            throw new IllegalArgumentException("purchaseRepo required");
        if (transactionRepo == null)
            throw new IllegalArgumentException("transactionRepo required");
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        if (ticketRepository == null)
            throw new IllegalArgumentException("ticketRepository required");
        if (policyRepository == null)
            throw new IllegalArgumentException("policyRepository required");
        if (represnteUserService == null)
            throw new IllegalArgumentException("represnteUserService required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (actionLogRepo == null)
            throw new IllegalArgumentException("actionLogRepo required");
        if (notificationService == null)
            throw new IllegalArgumentException("notificationService required");

        this.orderRepo = orderRepo;
        this.purchaseRepo = purchaseRepo;
        this.transactionRepo = transactionRepo;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.represnteUserService = represnteUserService;
        this.ticketDomainService = new Ticket_Domain_Service(ticketRepository);
        this.orderPolicyDomainService = new OrderPolicyDomainService(policyRepository, lotteryRepository);
        this.checkoutDomainService = new CheckoutDomainService(paymentGateway, ticketGateway, ticketDomainService);
        this.actionLogRepo = actionLogRepo;
        this.notificationService = notificationService;
        

        
    }

    public synchronized OrderDTO reserveTickets(String userToken, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for userToken {} event {}", userToken, eventId);
        String buyerId = represnteUserService.requireMemberId(userToken);
        if (orderRepo.findActiveOrder(buyerId, eventId).isPresent()) {
            logger.warn("User {} already has an active order for event {}", buyerId, eventId);
            throw new IllegalStateException("Active order already exists");
        }
        ActiveOrder order = new ActiveOrder(buyerId, eventId, TTL_MINUTES);
        try {
            List<Boolean> lockedStatuses = seats.stream().map(seat -> orderRepo.isTicketLocked(seat.getTicketId()))
                    .collect(Collectors.toList());
            List<String> reservedTicketIds = order.reserveTickets(seats, buyerId, lockedStatuses);
            orderRepo.save(order);
            ticketDomainService.lockAllTickets(order, reservedTicketIds);
            orderPolicyDomainService.validateAndApplyDiscounts(order, null);
            orderRepo.save(order);
            reservationsLastMinute.incrementAndGet();//new for Reservation Rate
            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);
        } catch (Exception e) {
            logger.error("reserveTickets FAILED rollback done userToken={}", userToken);
            throw e;
        }
    }

    public OrderDTO addTicketToOrder(UUID orderId, String userToken, SeatRequest seat) {
        logger.info("addTicketToOrder | orderId={} userToken={}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        try {
            boolean isLocked = orderRepo.isTicketLocked(seat.getTicketId());
            String ticketId = order.addTicketToOrder(seat, buyerId, isLocked);
            orderRepo.save(order);
            ticketDomainService.lockAllTickets(order, List.of(ticketId));
            orderPolicyDomainService.validateAndApplyDiscounts(order, order.getAppliedCouponCode());
            orderRepo.save(order);
            logger.info("addTicketToOrder SUCCESS orderId={}", orderId);
            return OrderMapper.toDTO(order);
        } catch (Exception e) {
            logger.error("addTicketToOrder FAILED | orderId={} error={}", orderId, e.getMessage());
            throw e;
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String userToken) {
        logger.info("Removing item {} from order {} by user {}", itemId, orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        OrderItem removed = order.removeTicket(itemId);

        // Record the action so the user can undo it (re-acquire the same seat).
        actionLogRepo.save(OrderActionLog.forRemovedTicket(order.getId(), removed));

        orderPolicyDomainService.applyDiscounts(order, order.getAppliedCouponCode());
        orderRepo.save(order);
        logger.info("Item removed successfully from order {}", orderId);
        return OrderMapper.toDTO(order);
    }

    /**
     * Reverses the most recent action recorded against {@code orderId}. The
     * caller must own the order. Currently supports REMOVE_TICKET (re-adds the
     * seat); other action types throw until they are retrofitted.
     */
    public OrderDTO undoLast(UUID orderId, String userToken) {
        logger.info("Undo requested for order {} by user {}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);

        OrderActionLog last = actionLogRepo.findTopByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> {
                    logger.warn("Undo failed — no actions logged for order {}", orderId);
                    return new IllegalStateException("Nothing to undo");
                });

        switch (last.getType()) {
            case REMOVE_TICKET -> undoRemoveTicket(order, buyerId, last);
            case ADD_TICKET, APPLY_COUPON -> {
                throw new IllegalStateException(
                        "Undo for " + last.getType() + " is not wired up yet");
            }
        }

        orderPolicyDomainService.applyDiscounts(order, order.getAppliedCouponCode());
        orderRepo.save(order);
        actionLogRepo.deleteById(last.getId());
        logger.info("Undid action {} on order {}", last.getType(), orderId);
        return OrderMapper.toDTO(order);
    }

    private void undoRemoveTicket(ActiveOrder order, String buyerId, OrderActionLog log) {
        if (orderRepo.isTicketLocked(log.getTicketId())) {
            logger.warn("Cannot undo remove on order {} — ticket {} is no longer free",
                    order.getId(), log.getTicketId());
            throw new IllegalStateException("Cannot undo: seat no longer available");
        }
        Lock lock = new Lock(log.getTicketId(), buyerId, order.getExpiresAt());
        order.addTicket(log.getTicketId(), log.getSeatId(),
                log.getAreaId(), log.getPrice(), lock);
        ticketDomainService.lockAllTickets(order, List.of(log.getTicketId()));
    }

    public PurchaseDTO checkout(UUID orderId, String userToken, PaymentDetailsDTO paymentDTO) {
        logger.info("Starting checkout for order {} by user {}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        orderPolicyDomainService.validatePurchasePolicy(order);

        PaymentDetails details = new PaymentDetails(paymentDTO.getCardToken(), paymentDTO.getBillingName(),
                paymentDTO.getPaymentMethod());
                
        try {
            CheckoutDomainService.CheckoutResult result = checkoutDomainService.checkout(order, details);
            purchaseRepo.save(result.getPurchase());
            paymentService.saveTransaction(result.getTransaction());
            orderRepo.save(order);
            //notify buyer about successful purchase
            notificationService.notifyPurchaseSuccess(
                    buyerId,
                    order.getEventId().toString()
            );
            logger.info("checkout SUCCESS orderId={} purchaseId={}",
                    orderId,
                    result.getPurchase().getPurchaseId());
            return OrderMapper.toDTO(result.getPurchase(), result.getTicketCodes());
        
        } catch (Exception e) {
            logger.error("checkout FAILED | orderId={} error={}", orderId, e.getMessage());
            orderRepo.save(order);
            throw e;
        }
    }

    public OrderDTO getActiveOrder(String userToken, UUID eventId) {
        logger.info("Fetching active order for user {}", userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);

        return orderRepo.findActiveOrder(buyerId, eventId)
                .map(order -> {
                    logger.info("Active order found {}", order.getId());
                    return OrderMapper.toDTO(order);
                })
                .orElseThrow(() -> {
                    logger.warn("No active order found for user {}", buyerId);
                    return new IllegalStateException("No active order");
                });
    }

    public void cancelOrder(UUID orderId, String userToken) {
        logger.info("Cancelling order {} for user {}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        try {
            ticketDomainService.releaseAllTickets(order.cancel());
            orderRepo.save(order);
            logger.info("Order {} cancelled successfully", orderId);
        } catch (Exception e) {
            logger.error("cancelOrder FAILED | orderId={} error={}", orderId, e.getMessage());
            throw e;
        }
    }

    public OrderDTO applyCoupon(UUID orderId, String userToken, String couponCode) {
        logger.info("Applying coupon {} for user {} on order {}", couponCode, userToken, orderId);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        try {
            orderPolicyDomainService.applyDiscounts(order, couponCode);
            orderRepo.save(order);
            logger.info("Coupon applied successfully for user {} on order {}", buyerId, orderId);
            return OrderMapper.toDTO(order);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid coupon {} orderId={} error={}", couponCode, orderId, e.getMessage());
            throw new IllegalStateException("Coupon is not valid");
        }
    }

    public List<PurchaseDTO> getPurchasesByEventId(UUID eventId) {
        logger.info("Fetching purchases for event {}", eventId);
        List<PurchaseDTO> result = OrderMapper.purchaseToDTOList(purchaseRepo.findByEventId(eventId));
        logger.info("Found {} purchases for event {}", result.size(), eventId);
        return result;
    }

    public List<OrderDTO> getPendingOrdersByBuyer(String userToken) {
        String buyerId = represnteUserService.requireMemberId(userToken);
        logger.info("Fetching active orders for buyer {}", buyerId);
        List<OrderDTO> result = OrderMapper.toDTOList(orderRepo.findPendingOrdersByBuyer(buyerId));
        logger.info("Found {} active orders for buyer {}", result.size(), buyerId);
        return result;
    }

    public OrderDTO getOrderById(UUID orderId, String userToken) {
        logger.info("getOrderById | orderId={} userToken={}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        return OrderMapper.toDTO(order);
    }

    public List<PurchaseDTO> getPurchaseHistory(String userToken) {
        String buyerId = represnteUserService.requireMemberId(userToken);
        logger.info("Fetching purchase history for user {}", buyerId);
        List<PurchaseDTO> result = OrderMapper.purchaseToDTOList(purchaseRepo.findByBuyerId(buyerId));
        logger.info("Purchase history fetched successfully for user {}", buyerId);
        return result;
    }

    // מספר הזמנות פעילות כרגע
    public int getActiveOrdersCount() {
        return (int) orderRepo.findAll().stream()
                .filter(o -> !o.isExpired())
                .count();
    }
 
    // מספר רכישות שהושלמו היום
    public int getPurchasesTodayCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return (int) purchaseRepo.findAll().stream()
                .filter(p -> p.getPurchasedAt() != null && p.getPurchasedAt().isAfter(startOfDay))
                .count();
    }
    // קצב שריונות בדקה האחרונה
    public int getReservationRate() {
        return reservationRateSnapshot.get();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredOrders() {
        logger.info("Running expired orders cleanup");
        try {
            List<ActiveOrder> expiredOrders = orderRepo.findExpiredOrders();
            ticketDomainService.expireOrders(expiredOrders);
            orderRepo.saveAll(expiredOrders);
            logger.info("Expired orders cleanup done. count={}", expiredOrders.size());
        } catch (Exception e) {
            logger.error("releaseExpiredOrders FAILED | error={}", e.getMessage());
        }
        // II.6.5 — מעדכן snapshot של קצב השריונות ומאפס את המונה
        reservationRateSnapshot.set(reservationsLastMinute.getAndSet(0));
        logger.info("Reservation rate updated. rate={}/min", reservationRateSnapshot.get());
    }

    private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
        logger.info("Checking order {} for user {}", orderId, buyerId);
        ActiveOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order {} not found", orderId);
                    return new IllegalArgumentException("Order not found");
                });
        if (!order.isOwnedBy(buyerId)) {
            logger.warn("User {} tried to access order {} which is not theirs", buyerId, orderId);
            throw new IllegalStateException("Order does not belong to user");
        }

        if (order.isExpired()) {
            logger.warn("Order {} is expired", orderId);
            throw new IllegalStateException("Order expired");
        }
        logger.info("Order {} is valid for user {}", orderId, buyerId);
        return order;
    }

}
