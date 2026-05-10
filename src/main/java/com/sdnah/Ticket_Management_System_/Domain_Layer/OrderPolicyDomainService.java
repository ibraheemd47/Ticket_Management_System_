package com.sdnah.Ticket_Management_System_.Domain_Layer;

import org.springframework.stereotype.Component;


import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

@Component
public class OrderPolicyDomainService {

    private final PolicyRepository policyRepository;

    public OrderPolicyDomainService(PolicyRepository policyRepository) {
        if (policyRepository == null)
            throw new IllegalArgumentException("policyRepository required");
        this.policyRepository = policyRepository;
    }

    public void validateAndApplyDiscounts(ActiveOrder order, String couponCode) {
        validateOrder(order);
        PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        validatePurchasePolicy(order, purchasePolicy);
        DiscountPolicy discountPolicy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        applyDiscountPolicy(order, discountPolicy, couponCode);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (Discount Calculation)
    // Also used in:
    // UC II.2.4 — Reserve Tickets in Active Order
    // UC II.2.6 — Modify Active Order (remove/update items)
    // =========================================================================
    public void applyDiscountPolicy(ActiveOrder order, DiscountPolicy policy, String couponCode) {
        validateOrder(order);
        double originalTotal = order.getTotal().doubleValue();
        int quantity = order.getItems().size();

        // If no discount policy exists → no discount is applied
        if (policy == null) {
            order.updateFinalPrice(originalTotal);
            return;
        }

        // Calculate final price according to discount policy rules
        double finalPrice = policy.calculateFinalPrice(originalTotal, quantity, couponCode);
        order.updateFinalPrice(finalPrice);

        // Store coupon code if provided
        if (couponCode != null && !couponCode.isBlank()) {
            order.setAppliedCouponCode(couponCode);
        }
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================
    public void validatePurchasePolicy(ActiveOrder order, PurchasePolicy policy) {
        validateOrder(order);

        // If no purchase policy exists → allow by default
        if (policy == null) {
            return;
        }
        int quantity = order.getItems().size();
        boolean allowed = policy.validatePurchase(quantity, false);
        if (!allowed) {
            throw new IllegalStateException("Order rejected by purchase policy");
        }
    }

    // =========================================================================
    // Shared validation for all order-related use cases
    // =========================================================================
    private void validateOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }

        if (order.getStatus() != ActiveOrder.Status.ACTIVE) {
            throw new IllegalStateException("Order is not active");
        }

        if (order.isExpired()) {
            throw new IllegalStateException("Order has expired");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Order is empty");
        }
    }
}