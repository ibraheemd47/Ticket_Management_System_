package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.LotteryEntry;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

@Component
public class OrderPolicyDomainService {

    private final PolicyRepository policyRepository;
    private final LotteryRepository lotteryRepository;
    

    public OrderPolicyDomainService(PolicyRepository policyRepository, LotteryRepository lotteryRepository) {
        if (policyRepository == null)
            throw new IllegalArgumentException("policyRepository required");
        this.policyRepository = policyRepository;
        this.lotteryRepository = lotteryRepository;
    }

    // =========================================================================
    // UC II.2.4 + II.2.8 — Validate purchase + apply discounts
    // =========================================================================
    public void validateAndApplyDiscounts(ActiveOrder order, String couponCode) {
        validateOrder(order);
        validatePurchasePolicy(order, findPurchasePolicy(order));
        applyDiscountPolicy(order, findDiscountPolicy(order), couponCode);
    }

    // =========================================================================
    // UC II.2.8 — Apply discounts only (used in applyCoupon / removeFromOrder)
    // =========================================================================
    public void applyDiscounts(ActiveOrder order, String couponCode) {
        validateOrder(order);
        applyDiscountPolicy(order, findDiscountPolicy(order), couponCode);
    }

    // =========================================================================
    // UC II.2.4 — Validate purchase policy only
    // =========================================================================
    public void validatePurchasePolicy(ActiveOrder order) {
        validateOrder(order);
        validatePurchasePolicy(order, findPurchasePolicy(order));
    }

    // =========================================================================
    // Private: find policies by eventId only
    // Every event has its own policy — no company fallback needed.
    // =========================================================================

    private PurchasePolicy findPurchasePolicy(ActiveOrder order) {
        Object result = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        if (result == null) return null;
        if (result instanceof java.util.Optional) return ((java.util.Optional<PurchasePolicy>) result).orElse(null);
        return (PurchasePolicy) result;
    }

    private DiscountPolicy findDiscountPolicy(ActiveOrder order) {
        Object result = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        if (result == null) return null;
        if (result instanceof java.util.Optional) return ((java.util.Optional<DiscountPolicy>) result).orElse(null);
        return (DiscountPolicy) result;
    }

    // =========================================================================
    // UC II.2.8 — Apply discount policy to order
    // =========================================================================
    public void applyDiscountPolicy(ActiveOrder order, DiscountPolicy policy, String couponCode) {
        validateOrder(order);
        double originalTotal = order.getTotal().doubleValue();
        int    quantity      = order.getItems().size();

        if (policy == null) {
            order.updateFinalPrice(originalTotal);
            return;
        }

        DiscountContext context = new DiscountContext(
                quantity,
                LocalDateTime.now(),
                couponCode,
                originalTotal,
                order.getEventId()
        );

        double finalPrice = policy.computeFinalPrice(originalTotal, context);
        order.updateFinalPrice(finalPrice);

        if (couponCode != null && !couponCode.isBlank()) {
            order.setAppliedCouponCode(couponCode);
        }
    }

    // =========================================================================
    // UC II.2.4 — Validate purchase policy against order
    // =========================================================================
    public void validatePurchasePolicy(ActiveOrder order, PurchasePolicy policy) {
        validateOrder(order);

        if (policy == null) return; // no restrictions

        int quantity = order.getItems().size();
        if (!policy.validatePurchase(quantity, false)) {
            throw new IllegalStateException("Order rejected by purchase policy");
        }
    }


    // =========================================================================
    // Selling policy validation (for lottery access code)
    // =========================================================================
    public void validateSellingPolicy(ActiveOrder order, String memberId) {
        Object result = policyRepository.findSellingPolicyByEventId(order.getEventId());
        SellingPolicy policy = toSellingPolicy(result);

        if (policy == null || policy.getType() == SellingPolicy.SellingType.REGULAR) {
            return; 
        }

        // LOTTERY — בדוק accessCode תקף
        lotteryRepository.findByEventId(order.getEventId()).stream()
            .flatMap(l -> l.getEntries().stream())
            .filter(e -> e.getMemberId().equals(memberId))
            .filter(LotteryEntry::isAccessCodeValid)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No valid lottery access code for this event"));
    }

    private SellingPolicy toSellingPolicy(Object result) {
        if (result == null) return null;
        if (result instanceof Optional) return ((Optional<SellingPolicy>) result).orElse(null);
        return (SellingPolicy) result;
    }

    // =========================================================================
    // Shared order validation
    // =========================================================================
    private void validateOrder(ActiveOrder order) {
        if (order == null)
            throw new IllegalArgumentException("Order is required");
        if (order.getStatus() != ActiveOrder.Status.ACTIVE)
            throw new IllegalStateException("Order is not active");
        if (order.isExpired())
            throw new IllegalStateException("Order has expired");
        if (order.getItems().isEmpty())
            throw new IllegalStateException("Order is empty");
    }
}