package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

import jakarta.transaction.Transactional;

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
    private final IPolicyRepo policyRepository;

    public ActiveOrderService(ActiveOrderRepository orderRepo,
            PurchaseRepository purchaseRepo,
            PaymentTransactionRepository transactionRepo,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway,
            TicketRepository ticketRepository,
            Ticket_Domain_Service ticketDomainService, IPolicyRepo policyRepo,
            OrderPolicyDomainService orderPolicyDomainService) {
        if (orderRepo == null)
            throw new IllegalArgumentException("orderRepo required");
        if (purchaseRepo == null)
            throw new IllegalArgumentException("purchaseRepo required");
        if (transactionRepo == null)
            throw new IllegalArgumentException("transactionRepo required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        if (ticketRepository == null)
            throw new IllegalArgumentException("ticketRepository required");
        if (ticketDomainService == null)
            throw new IllegalArgumentException("ticketDomainService required");
        this.orderRepo = orderRepo;
        this.purchaseRepo = purchaseRepo;
        this.transactionRepo = transactionRepo;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.ticketRepository = ticketRepository;
        this.ticketDomainService = ticketDomainService;

        // new:
        this.orderPolicyDomainService = orderPolicyDomainService;
        this.policyRepository = policyRepo;

    }

    public synchronized OrderDTO reserveTickets(String userToken, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for user {} event {}", userToken, eventId);

        if (orderRepo.findActiveOrder(userToken, eventId).isPresent()) {
            logger.warn("User {} already has an active order for event {}", userToken, eventId);
            throw new IllegalStateException("Active order already exists");
        }

        ActiveOrder order = new ActiveOrder(userToken, eventId, TTL_MINUTES);

        try {
            for (SeatRequest seat : seats) {
                Lock lock = new Lock(
                        seat.getTicketId(),
                        buyerId,
                        LocalDateTime.now().plusMinutes(TTL_MINUTES)
                );

<<<<<<< HEAD
                order.addTicket(
                        seat.getTicketId(),
                        seat.getSeatId(),
                        seat.getAreaId(),
                        seat.getPrice(),
                        lock
                );
            }

            double finalPrice = policyService.applyGeneralDiscounts(
                    eventId,
                    order.getTotal().doubleValue(),
                    order.getItems().size()
            );

            order.updateFinalPrice(finalPrice);

            orderRepo.saveAndFlush(order);

            for (SeatRequest seat : seats) {
                ticketRepository.findById(UUID.fromString(seat.getTicketId()))
                        .ifPresent(t -> ticketDomainService.TicketLocked(buyerId, t));
            }
=======
                Lock lock = new Lock(seat.getTicketId(), userToken,
                        LocalDateTime.now().plusMinutes(TTL_MINUTES));
                order.addTicket(seat.getTicketId(), seat.getSeatId(),
                        seat.getAreaId(), seat.getPrice(), lock);

                // save immediately so lock is visible in DB to other transactions
                orderRepo.save(order);

                // notify EVENT aggregate — mark ticket as LOCKED
                ticketRepository.findById(UUID.fromString(seat.getTicketId()))
                        .ifPresent(t -> ticketDomainService.TicketLocked(t));
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
>>>>>>> main

            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.warn("Ticket already reserved for buyerId={}", buyerId);
            throw new IllegalStateException("Ticket already reserved");
        } catch (Exception e) {
<<<<<<< HEAD
            logger.error("reserveTickets FAILED rollback done buyerId={}", buyerId);
=======
            // @Transactional rolls back all saves automatically — locks released
            logger.error("reserveTickets FAILED rollback done userToken={}", userToken);
>>>>>>> main
            throw e;
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String userToken) {
        logger.info("Removing item {} from order {} by user {}", itemId, orderId, userToken);
        ActiveOrder order = findValidOrder(orderId, userToken);
        OrderItem item = order.removeTicket(itemId);
        // PolicyService returns finalPrice — update directly
        // if condition no longer met, PolicyService returns original total

        // double finalPrice = policyService.applyGeneralDiscounts(
        // order.getEventId(),
        // order.getTotal().doubleValue(),
        // order.getItems().size());
        // order.updateFinalPrice(finalPrice);

        // new after domain service refactor:
        // שליפת המדיניות מחדש כדי לוודא שהנחות מותנות עדיין תקפות
        DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        orderPolicyDomainService.applyDiscountPolicy(order, policy, order.getAppliedCouponCode());
        // ----------------------------------------

        orderRepo.save(order);
        logger.info("Item removed successfully from order {}", orderId);
        return OrderMapper.toDTO(order);
    }

    public PurchaseDTO checkout(UUID orderId, String userToken, PaymentDetailsDTO paymentDTO) {
        logger.info("Starting checkout for order {} by user {}", orderId, userToken);

        ActiveOrder order = findValidOrder(orderId, userToken);

        // new after domain service refactor:
        /// validate purchase policy before charging
        PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);

        PaymentDetails details = new PaymentDetails(paymentDTO.getCardToken(), paymentDTO.getBillingName(),
                paymentDTO.getPaymentMethod());

        // 1. חיוב תשלום
        var tx = paymentService.charge(orderId, order.getFinalPrice(), details);
        if (!tx.isSuccessful()) {
            logger.warn("Payment FAILED for orderId={}", orderId);
            throw new IllegalStateException("Payment failed");
        }
        List<Ticketcode> codes;
        try {
            // 2. הנפקת כרטיסים
            codes = ticketGateway.issueTickets(orderId, order.getItems());
        } catch (Exception e) {
            // 3. כשל בהנפקה — refund אוטומטי (כללי יושרה)
            logger.error("Ticket issuance FAILED for orderId={}, issuing refund", orderId);
            paymentService.refund(tx.getTransactionId());
            throw new IllegalStateException("Ticket issuance failed, payment refunded");
        }
        // 4. שמור Purchase
        Purchase purchase = new Purchase(order);
        purchaseRepo.save(purchase);
        transactionRepo.save(tx);
        // 5. שחרר locks + סמן הזמנה כ-COMPLETED
        // domain clears locks and returns IDs — service releases them in repository
        for (String lockId : order.releaseAllLocks()) {
            ticketRepository.findById(UUID.fromString(lockId))
                    .ifPresent(t -> ticketDomainService.TicketSold(order, t));
        }
        order.markCompleted();
        orderRepo.save(order);
        // 6. בנה רשימת קודי כרטיסים לתשובה
        List<String> ticketCodes = new ArrayList<>();
        for (Ticketcode tc : codes)
            ticketCodes.add(tc.getCode());

        logger.info("checkout SUCCESS orderId={} purchaseId={}", orderId, purchase.getPurchaseId());
        return OrderMapper.toDTO(purchase, ticketCodes);
    }

    public OrderDTO getActiveOrder(String userToken, UUID eventId) {
        logger.info("Fetching active order for user {}", userToken);

        return orderRepo.findActiveOrder(userToken, eventId)
                .map(order -> {
                    logger.info("Active order found {}", order.getId());
                    return OrderMapper.toDTO(order);
                })
                .orElseThrow(() -> {
                    logger.warn("No active order found for user {}", userToken);
                    return new IllegalStateException("No active order");
                });
    }

<<<<<<< HEAD
    public void cancelOrder(UUID orderId, String buyerId) {
        logger.info("Cancelling order {} for user {}", orderId, buyerId);

        ActiveOrder order = findValidOrder(orderId, buyerId);

=======
    public void cancelOrder(UUID orderId, String userToken) {
        logger.info("Cancelling order {} for user {}", orderId, userToken);
        ActiveOrder order = findValidOrder(orderId, userToken);
        order.markCancelled();
        // domain clears locks and returns IDs — service releases them in repository
>>>>>>> main
        for (String lockId : order.releaseAllLocks()) {
            ticketRepository.findById(UUID.fromString(lockId))
                    .ifPresent(t -> ticketDomainService.TicketAvailable(t));
        }

        order.markCancelled();

        // IMPORTANT:
        // Because ticket_id is UNIQUE in order_items,
        // we must delete the cancelled order so its OrderItems are removed too.
        orderRepo.delete(order);
        orderRepo.flush();

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
        ActiveOrder order = findValidOrder(orderId, userToken);
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
            logger.info("Coupon applied successfully for user {} on order {}", userToken, orderId);
            return OrderMapper.toDTO(order);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coupon {} for user {} on order {}", couponCode, userToken, orderId);
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
        logger.info("Fetching active orders for buyer {}", userToken);
        List<ActiveOrder> pending = orderRepo.findPendingOrdersByBuyer(userToken);
        List<OrderDTO> result = new ArrayList<>();
        for (ActiveOrder o : pending)
            result.add(OrderMapper.toDTO(o));
        logger.info("Found {} active orders for buyer {}", result.size(), userToken);
        return result;
    }

    public OrderDTO getOrderById(UUID orderId, String userToken) {
        logger.info("getOrderById | orderId={} userToken={}", orderId, userToken);
        ActiveOrder order = findValidOrder(orderId, userToken);
        return OrderMapper.toDTO(order);
    }

    /** completed purchases — GetAllPassedOrdersByMemberId */
    public List<PurchaseDTO> getPurchaseHistory(String userToken) {
        logger.info("Fetching purchase history for user {}", userToken);
        List<Purchase> purchases = purchaseRepo.findByuserToken(userToken);
        List<PurchaseDTO> result = new ArrayList<>();
        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }
        logger.info("Purchase history fetched successfully for user {}", userToken);
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
        for (ActiveOrder order : expiredOrders) {
            order.markExpired();
            for (String lockId : order.releaseAllLocks()) {
                ticketRepository.findById(UUID.fromString(lockId))
                        .ifPresent(t -> ticketDomainService.TicketAvailable(t));
            }
            orderRepo.save(order);
            logger.info("Expired order {} released", order.getId());
        }
        logger.info("Expired orders cleanup done. count={}", expiredOrders.size());
    }

    private ActiveOrder findValidOrder(UUID orderId, String userToken) {
        logger.info("Checking order {} for user {}", orderId, userToken);
        ActiveOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order {} not found", orderId);
                    return new IllegalArgumentException("Order not found");
                });
        if (!order.isOwnedBy(userToken)) {
            logger.warn("User {} tried to access order {} which is not theirs", userToken, orderId);
            throw new IllegalStateException("Order does not belong to user");
        }

        if (order.isExpired()) {
            logger.warn("Order {} is expired", orderId);
            throw new IllegalStateException("Order expired");
        }
        logger.info("Order {} is valid for user {}", orderId, userToken);
        return order;
    }

}
