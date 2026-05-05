package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;

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
    private final PolicyService policyService;
    private final Ticket_Domain_Service ticketDomainService;// ++
    private final TicketRepository ticketRepository;// ++

    public ActiveOrderService(ActiveOrderRepository orderRepo,
            PurchaseRepository purchaseRepo,
            PaymentTransactionRepository transactionRepo,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway,
            PolicyService policyService,
            TicketRepository ticketRepository,
            Ticket_Domain_Service ticketDomainService) {
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
        if (policyService == null)
            throw new IllegalArgumentException("policyService required");
        if (ticketRepository == null)
            throw new IllegalArgumentException("ticketRepository required");
        if (ticketDomainService == null)
            throw new IllegalArgumentException("ticketDomainService required");
        this.orderRepo = orderRepo;
        this.purchaseRepo = purchaseRepo;
        this.transactionRepo = transactionRepo;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.policyService = policyService;
        this.ticketRepository = ticketRepository;
        this.ticketDomainService = ticketDomainService;
    }

    public OrderDTO reserveTickets(String buyerId, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for user {} event {}", buyerId, eventId);

        if (orderRepo.findActiveOrder(buyerId, eventId).isPresent()) {
            logger.warn("User {} already has an active order for event {}", buyerId, eventId);
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
            double finalPrice = policyService.applyGeneralDiscounts(eventId, order.getTotal().doubleValue(),
                    order.getItems().size());
            order.updateFinalPrice(finalPrice);
            // lock is persisted with the order via JPA (@Embedded in OrderItem)
            orderRepo.save(order);

            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);

        } catch (Exception e) {
            // @Transactional rolls back all saves automatically — locks released
            logger.error("reserveTickets FAILED rollback done buyerId={}", buyerId);
            throw e;
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String buyerId) {
        logger.info("Removing item {} from order {} by user {}", itemId, orderId, buyerId);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        OrderItem item = order.removeTicket(itemId);
        // PolicyService returns finalPrice — update directly
        // if condition no longer met, PolicyService returns original total
        double finalPrice = policyService.applyGeneralDiscounts(
                order.getEventId(),
                order.getTotal().doubleValue(),
                order.getItems().size());
        order.updateFinalPrice(finalPrice);
        orderRepo.save(order);
        logger.info("Item removed successfully from order {}", orderId);
        return OrderMapper.toDTO(order);
    }

    public PurchaseDTO checkout(UUID orderId, String buyerId, PaymentDetailsDTO paymentDTO) {
        logger.info("Starting checkout for order {} by user {}", orderId, buyerId);

        ActiveOrder order = findValidOrder(orderId, buyerId);
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

    public OrderDTO getActiveOrder(String buyerId, UUID eventId) {
        logger.info("Fetching active order for user {}", buyerId);

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

    public void cancelOrder(UUID orderId, String buyerId) {
        logger.info("Cancelling order {} for user {}", orderId, buyerId);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        order.markCancelled();
        // domain clears locks and returns IDs — service releases them in repository
        for (String lockId : order.releaseAllLocks()) {
            ticketRepository.findById(UUID.fromString(lockId))
                    .ifPresent(t -> ticketDomainService.TicketAvailable( t));
        }
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

    public OrderDTO applyCoupon(UUID orderId, String buyerId, String couponCode) {
        logger.info("Applying coupon {} for user {} on order {}", couponCode, buyerId, orderId);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        try {
            // PolicyService returns finalPrice — update directly
            double finalPrice = policyService.calculateCouponDiscount(
                    order.getEventId(),
                    order.getFinalPrice().doubleValue(),
                    order.getItems().size(),
                    couponCode);
            order.updateFinalPrice(finalPrice);
            order.setAppliedCouponCode(couponCode);
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
    public List<OrderDTO> getPendingOrdersByBuyer(String buyerId) {
        logger.info("Fetching active orders for buyer {}", buyerId);
        List<ActiveOrder> pending = orderRepo.findPendingOrdersByBuyer(buyerId);
        List<OrderDTO> result = new ArrayList<>();
        for (ActiveOrder o : pending)
            result.add(OrderMapper.toDTO(o));
        logger.info("Found {} active orders for buyer {}", result.size(), buyerId);
        return result;
    }

    public OrderDTO getOrderById(UUID orderId, String buyerId) {
        logger.info("getOrderById | orderId={} buyerId={}", orderId, buyerId);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        return OrderMapper.toDTO(order);
    }

    /** completed purchases — GetAllPassedOrdersByMemberId */
    public List<PurchaseDTO> getPurchaseHistory(String buyerId) {
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
