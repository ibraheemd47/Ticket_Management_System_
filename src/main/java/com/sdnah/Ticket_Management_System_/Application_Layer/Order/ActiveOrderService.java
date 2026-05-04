package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;

@Service
public class ActiveOrderService {
   
    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderService.class);
    private static final int TTL_MINUTES = 10;
    private final IOrderRepository orderRepository;
    private final PaymentService paymentService;
    private final ITicketSupplierGateway ticketGateway;
   // private final PolicyService policyService;


    //new:
    private final OrderPolicyDomainService orderPolicyDomainService;
    private final IPolicyRepo policyRepository;

    public ActiveOrderService(IOrderRepository orderRepository,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway,IPolicyRepo policyRepo,OrderPolicyDomainService orderPolicyDomainService) {
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        // if (policyService == null)
        //     throw new IllegalArgumentException("policyService required");
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        //this.policyService = policyService;
        this.orderPolicyDomainService = orderPolicyDomainService;
        this.policyRepository = policyRepo;
    }

    public OrderDTO reserveTickets(String buyerId, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for user {} event {}", buyerId, eventId);

        if (orderRepository.findActiveOrder(buyerId, eventId).isPresent()) {
            logger.warn("User {} already has an active order for event {}", buyerId, eventId);
            throw new IllegalStateException("Active order already exists");
        }

        ActiveOrder order = new ActiveOrder(buyerId, eventId, TTL_MINUTES);
        List<String> lockedTickets = new ArrayList<>();

        try {
            for (SeatRequest seat : seats) {
                // create lock for each ticket
                Lock lock = new Lock(seat.getTicketId(), buyerId, LocalDateTime.now().plusMinutes(TTL_MINUTES));
                boolean acquired = orderRepository.acquireLock(lock);
                if (!acquired) {
                    logger.warn("Ticket {} is already reserved", seat.getTicketId());
                    throw new IllegalStateException("Ticket already reserved: " + seat.getTicketId());
                }
                // keep track of locked tickets (for rollback if needed)
                lockedTickets.add(seat.getTicketId());
                // create order item
                order.addTicket(seat.getTicketId(), seat.getSeatId(), seat.getAreaId(), seat.getPrice(), lock);
            }
            // PolicyService returns finalPrice — update directly
            // double finalPrice = policyService.applyGeneralDiscounts(eventId, order.getTotal().doubleValue(),
            //         order.getItems().size());
            //           order.updateFinalPrice(finalPrice);

            //new after domain service refactor:
            PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
            orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);
            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, null);


            orderRepository.save(order);

            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);

        } catch (Exception e) {
            for (String ticketId : lockedTickets) {
                orderRepository.releaseLock(ticketId);
            }
            logger.error("Reservation failed, rollback executed for user {}", buyerId);
            throw e;
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String buyerId) {
        logger.info("Removing item {} from order {} by user {}", itemId, orderId, buyerId);

        ActiveOrder order = findValidOrder(orderId, buyerId);
        // domain finds, validates and removes — returns item for lock release
        OrderItem item = order.removeTicket(itemId);
        orderRepository.releaseLock(item.getLockResourceId());
        // PolicyService returns finalPrice — update directly
        // if condition no longer met, PolicyService returns original total


        // double finalPrice = policyService.applyGeneralDiscounts(
        //         order.getEventId(),
        //         order.getTotal().doubleValue(),
        //         order.getItems().size());
        // order.updateFinalPrice(finalPrice);

        //new after domain service refactor:
        // שליפת המדיניות מחדש כדי לוודא שהנחות מותנות עדיין תקפות
        DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        orderPolicyDomainService.applyDiscountPolicy(order, policy, order.getAppliedCouponCode());
        // ----------------------------------------


        orderRepository.save(order);
        logger.info("Item removed successfully from order {}", orderId);
        return OrderMapper.toDTO(order);
    }

    public PurchaseDTO checkout(UUID orderId, String buyerId, PaymentDetailsDTO paymentDTO) {
        logger.info("Starting checkout for order {} by user {}", orderId, buyerId);

        ActiveOrder order = findValidOrder(orderId, buyerId);

        /// validate purchase policy before charging
        
        


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
        orderRepository.savePurchase(purchase);
        // 5. שחרר locks + סמן הזמנה כ-COMPLETED
        // domain clears locks and returns IDs — service releases them in repository
        for (String lockId : order.releaseAllLocks())
            orderRepository.releaseLock(lockId);
        order.markCompleted();
        orderRepository.save(order);
        // 6. בנה רשימת קודי כרטיסים לתשובה
        List<String> ticketCodes = new ArrayList<>();
        for (Ticketcode tc : codes)
            ticketCodes.add(tc.getCode());

        logger.info("checkout SUCCESS orderId={} purchaseId={}", orderId, purchase.getPurchaseId());
        return OrderMapper.toDTO(purchase, ticketCodes);
    }

    public OrderDTO getActiveOrder(String buyerId, UUID eventId) {
        logger.info("Fetching active order for user {}", buyerId);

        return orderRepository.findActiveOrder(buyerId, eventId)
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
        for (String lockId : order.releaseAllLocks())
            orderRepository.releaseLock(lockId);

        orderRepository.save(order);
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
            // double finalPrice = policyService.calculateCouponDiscount(
            //         order.getEventId(),
            //         order.getFinalPrice().doubleValue(),
            //         order.getItems().size(),
            //         couponCode);
            // order.updateFinalPrice(finalPrice);
            // order.setAppliedCouponCode(couponCode);

            //new after domain service refactor:
            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, couponCode);
            // -------------------------------------------------------


            orderRepository.save(order);
            logger.info("Coupon applied successfully for user {} on order {}", buyerId, orderId);
            return OrderMapper.toDTO(order);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coupon {} for user {} on order {}", couponCode, buyerId, orderId);
            throw new IllegalStateException("Coupon is not valid");
        }
    }

    public List<PurchaseDTO> getPurchasesByEventId(UUID eventId) {
        logger.info("Fetching purchases for event {}", eventId);
        List<Purchase> purchases = orderRepository.findPurchasesByEventId(eventId);
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
        List<ActiveOrder> pending = orderRepository.findPendingOrdersByBuyer(buyerId);
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
        List<Purchase> purchases = orderRepository.findPurchasesByBuyer(buyerId);
        List<PurchaseDTO> result = new ArrayList<>();
        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }
        logger.info("Purchase history fetched successfully for user {}", buyerId);
        return result;
    }

    private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
        logger.info("Checking order {} for user {}", orderId, buyerId);
        ActiveOrder order = orderRepository.findById(orderId)
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
