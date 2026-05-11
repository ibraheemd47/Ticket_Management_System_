package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Checkout_Domain_Service;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderActionLog;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OrderActionLogRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

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
    private final Ticket_Domain_Service ticketDomainService;// ++
    private final TicketRepository ticketRepository;// ++

    // new:
    private final OrderPolicyDomainService orderPolicyDomainService;
    private final PolicyRepository policyRepository;
    private final OrderActionLogRepository actionLogRepo;
    private final CheckoutDomainService checkoutDomainService;

    // private final userOrderDomainService userOrderDomainService;

    private IrepresnteUserService represnteUserService;

    public ActiveOrderService(ActiveOrderRepository orderRepo,
            PurchaseRepository purchaseRepo,
            PaymentTransactionRepository transactionRepo,
            PaymentService paymentService,
            IPaymentGateway paymentGateway,
            ITicketSupplierGateway ticketGateway,
            TicketRepository ticketRepository,
            PolicyRepository policyRepository,
            OrderPolicyDomainService orderPolicyDomainService,
            IrepresnteUserService represnteUserService,
            OrderActionLogRepository actionLogRepo) {
        if (orderRepo == null)
            throw new IllegalArgumentException("orderRepo required");
        if (purchaseRepo == null)
            throw new IllegalArgumentException("purchaseRepo required");
        if (transactionRepo == null)
            throw new IllegalArgumentException("transactionRepo required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        if (ticketRepository == null)
            throw new IllegalArgumentException("ticketRepository required");
        if (actionLogRepo == null)
            throw new IllegalArgumentException("actionLogRepo required");

        this.orderRepo = orderRepo;
        this.purchaseRepo = purchaseRepo;
        this.transactionRepo = transactionRepo;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.ticketRepository = ticketRepository;
        this.ticketDomainService = new Ticket_Domain_Service(ticketRepository);
        this.represnteUserService = represnteUserService;
        this.orderPolicyDomainService = orderPolicyDomainService;
        this.policyRepository = policyRepository;
        this.actionLogRepo = actionLogRepo;
        this.checkoutDomainService = new CheckoutDomainService(paymentGateway, ticketGateway);
    }

    public synchronized OrderDTO reserveTickets(String userToken, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for userToken {} event {}", userToken, eventId);

        String buyerId = represnteUserService.requireMemberId(userToken);

        if (orderRepo.findActiveOrder(buyerId, eventId).isPresent()) {
            logger.warn("User {} already has an active order for event {}", userToken, eventId);
            throw new IllegalStateException("Active order already exists");
        }

        ActiveOrder order = new ActiveOrder(buyerId, eventId, TTL_MINUTES);

        try {
            for (SeatRequest seat : seats) {
                // check if ticket is already locked in DB
                if (orderRepo.isTicketLocked(seat.getTicketId())) {
                    logger.warn("Ticket already locked: {}", seat.getTicketId());
                    throw new IllegalStateException("Ticket already reserved: " + seat.getTicketId());
                }

                Lock lock = new Lock(seat.getTicketId(), buyerId,
                        LocalDateTime.now().plusMinutes(TTL_MINUTES));
                order.addTicket(seat.getTicketId(), seat.getSeatId(),
                        seat.getAreaId(), seat.getPrice(), lock);

                // save immediately so lock is visible in DB to other transactions
                orderRepo.save(order);

                // notify EVENT aggregate — mark ticket as LOCKED
                ticketRepository.findById(UUID.fromString(seat.getTicketId()))
                        .ifPresent(t -> ticketDomainService.TicketLocked(buyerId, t));
            }
            // PolicyService returns finalPrice — update directly
            // double finalPrice = policyService.applyGeneralDiscounts(eventId,
            // order.getTotal().doubleValue(),
            // order.getItems().size());
            // order.updateFinalPrice(finalPrice);

            // new after domain service refactor:
            PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
            orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);
            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, null);

            orderRepo.save(order);

            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);

        } catch (Exception e) {
            // @Transactional rolls back all saves automatically — locks released
            logger.error("reserveTickets FAILED rollback done userToken={}", userToken);
            throw e;
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String userToken) {
        logger.info("Removing item {} from order {} by user {}", itemId, orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        OrderItem removed = order.removeTicket(itemId);

        // The OrderItem reference is the lock-of-record (orderRepo.isTicketLocked
        // checks for it). Removing the item releases the lock; no ticket-entity
        // side effect needed here.
        actionLogRepo.save(OrderActionLog.forRemovedTicket(order.getId(), removed));

        // Re-evaluate conditional discounts with the new item count.
        DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        orderPolicyDomainService.applyDiscountPolicy(order, policy, order.getAppliedCouponCode());

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

        DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        orderPolicyDomainService.applyDiscountPolicy(order, policy, order.getAppliedCouponCode());

        orderRepo.save(order);
        actionLogRepo.deleteById(last.getId());
        logger.info("Undid action {} on order {}", last.getType(), orderId);
        return OrderMapper.toDTO(order);
    }

    private void undoRemoveTicket(ActiveOrder order, String buyerId, OrderActionLog log) {
        if (orderRepo.isTicketLocked(log.getTicketId())) {
            // Someone else (or even this user via another order) grabbed the seat
            // between the remove and the undo — we can't honestly re-attach it.
            logger.warn("Cannot undo remove on order {} — ticket {} is no longer free",
                    order.getId(), log.getTicketId());
            throw new IllegalStateException("Cannot undo: seat no longer available");
        }
        Lock lock = new Lock(log.getTicketId(), buyerId,
                LocalDateTime.now().plusMinutes(TTL_MINUTES));
        order.addTicket(log.getTicketId(), log.getSeatId(),
                log.getAreaId(), log.getPrice(), lock);
        // Re-adding the OrderItem is itself the lock — orderRepo.isTicketLocked
        // will return true on this ticketId from now on. No ticket-entity touch.
    }

    public PurchaseDTO checkout(UUID orderId, String userToken, PaymentDetailsDTO paymentDTO) {
        logger.info("Starting checkout for order {} by user {}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);

        ActiveOrder order = findValidOrder(orderId, buyerId);

        // Validate purchase policy before charging — fast-fail before the saga
        // touches any external systems.
        PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);

        PaymentDetails details = new PaymentDetails(paymentDTO.getCardToken(), paymentDTO.getBillingName(),
                paymentDTO.getPaymentMethod());

        try {
            // Saga: charge → issue tickets → finalize order. Any step failure
            // triggers LIFO compensation (refund + cancel) inside the service.
            CheckoutDomainService.CheckoutResult result = checkoutDomainService.checkout(order, details);

            purchaseRepo.save(result.getPurchase());
            transactionRepo.save(result.getTransaction());

            // Mirror the COMPLETED items into the ticket aggregate so the
            // entity status follows the order state.
            for (OrderItem item : result.getPurchase().getItems()) {
                ticketDomainService.markTicketAsSold(order, item.getTicketId());
            }

            orderRepo.save(order);
            logger.info("checkout SUCCESS orderId={} purchaseId={}",
                    orderId, result.getPurchase().getPurchaseId());
            return OrderMapper.toDTO(result.getPurchase(), result.getTicketCodes());

        } catch (RuntimeException e) {
            logger.error("checkout FAILED | orderId={} error={}", orderId, e.getMessage());
            // Saga already ran its compensations (refund issued, order cancelled).
            // Persist the cancelled state so a follow-up read sees it.
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
        order.markCancelled();
        ticketDomainService.releaseAllTickets(order.releaseAllLocks());
        orderRepo.save(order);
        logger.info("Order {} cancelled successfully", orderId);
    }

    /**
     * Applies a coupon discount on top of the current finalPrice (after conditional
     * discount).
     * Called when user enters a coupon code in the checkout page to preview the
     * final price.
     * If items are added or removed after applying the coupon, the coupon is not
     * recalculated —
     * the user should re-enter the coupon code to reapply it.
     */

    public OrderDTO applyCoupon(UUID orderId, String userToken, String couponCode) {
        logger.info("Applying coupon {} for user {} on order {}", couponCode, userToken, orderId);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        try {
            // PolicyService returns finalPrice — update directly
            // double finalPrice = policyService.calculateCouponDiscount(
            // order.getEventId(),
            // order.getFinalPrice().doubleValue(),
            // order.getItems().size(),
            // couponCode);
            // order.updateFinalPrice(finalPrice);
            // order.setAppliedCouponCode(couponCode);

            // new after domain service refactor:
            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, couponCode);
            // -------------------------------------------------------

            orderRepo.save(order);
            logger.info("Coupon applied successfully for user {} on order {}", buyerId, orderId);
            return OrderMapper.toDTO(order);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coupon {} for user {} on order {}", couponCode, buyerId, orderId);
            throw new IllegalStateException("Coupon is not valid");
        }
    }

    public List<PurchaseDTO> getPurchasesByEventId(UUID eventId) {
        logger.info("Fetching purchases for event {}", eventId);
        List<Purchase> purchases = purchaseRepo.findByEventId(eventId);
        List<PurchaseDTO> result = new ArrayList<>();
        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }
        logger.info("Found {} purchases for event {}", result.size(), eventId);
        return result;
    }

    /**
     * Returns all ACTIVE (in-progress, not yet purchased) orders for a buyer.
     * Used for GetAllUpcomingOrdersByMemberId.
     */
    public List<OrderDTO> getPendingOrdersByBuyer(String userToken) {
        String buyerId = represnteUserService.requireMemberId(userToken);
        logger.info("Fetching active orders for buyer {}", buyerId);
        List<ActiveOrder> pending = orderRepo.findPendingOrdersByBuyer(buyerId);
        List<OrderDTO> result = new ArrayList<>();
        for (ActiveOrder o : pending)
            result.add(OrderMapper.toDTO(o));
        logger.info("Found {} active orders for buyer {}", result.size(), buyerId);
        return result;
    }

    public OrderDTO getOrderById(UUID orderId, String userToken) {
        logger.info("getOrderById | orderId={} userToken={}", orderId, userToken);
        String buyerId = represnteUserService.requireMemberId(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        return OrderMapper.toDTO(order);
    }

    /** completed purchases — GetAllPassedOrdersByMemberId */
    public List<PurchaseDTO> getPurchaseHistory(String userToken) {
        String buyerId = represnteUserService.requireMemberId(userToken);
        logger.info("Fetching purchase history for user {}", buyerId);
        List<Purchase> purchases = purchaseRepo.findByBuyerId(buyerId);
        List<PurchaseDTO> result = new ArrayList<>();
        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }
        logger.info("Purchase history fetched successfully for user {}", buyerId);
        return result;
    }

    /**
     * Runs every minute to release expired orders.
     * Marks expired ACTIVE orders as EXPIRED, clears locks,
     * and marks tickets as AVAILABLE in EVENT aggregate.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredOrders() {
        logger.info("Running expired orders cleanup");
        List<ActiveOrder> expiredOrders = orderRepo.findExpiredOrders();
        ticketDomainService.expireOrders(expiredOrders);
        orderRepo.saveAll(expiredOrders);
        logger.info("Expired orders cleanup done. count={}", expiredOrders.size());
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