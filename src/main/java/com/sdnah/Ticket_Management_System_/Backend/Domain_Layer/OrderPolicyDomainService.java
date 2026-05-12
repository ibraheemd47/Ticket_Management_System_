package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

@Component
public class OrderPolicyDomainService {

    private final PolicyRepository policyRepository;
    private final IEventRepository eventRepository;

    public OrderPolicyDomainService(PolicyRepository policyRepository,
                                    IEventRepository eventRepository) {
        if (policyRepository == null)
            throw new IllegalArgumentException("policyRepository required");
        if (eventRepository == null)
            throw new IllegalArgumentException("eventRepository required");
        this.policyRepository = policyRepository;
        this.eventRepository  = eventRepository;
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
    // Private: find policies — event-first, company-fallback via Event
    // =========================================================================

    private PurchasePolicy findPurchasePolicy(ActiveOrder order) {
        if (order.getEventId() != null) {
            Optional<PurchasePolicy> ep =
                    policyRepository.findPurchasePolicyByEventId(order.getEventId());
            if (ep.isPresent()) return ep.get();
        }

        Integer companyId = getCompanyIdFromEvent(order);
        if (companyId != null) {
            return policyRepository
                    .findPurchasePolicyByCompanyIdAndEventIdIsNull(companyId)
                    .orElse(null);
        }

        return null; // no restrictions
    }

    private DiscountPolicy findDiscountPolicy(ActiveOrder order) {
        if (order.getEventId() != null) {
            Optional<DiscountPolicy> ep =
                    policyRepository.findDiscountPolicyByEventId(order.getEventId());
            if (ep.isPresent()) return ep.get();
        }

        Integer companyId = getCompanyIdFromEvent(order);
        if (companyId != null) {
            return policyRepository
                    .findDiscountPolicyByCompanyIdAndEventIdIsNull(companyId)
                    .orElse(null);
        }

        return null; // no discount
    }

    /**
     * Gets the companyId by loading the Event associated with the order.
     * Returns null if the event is not found.
     */
    private Integer getCompanyIdFromEvent(ActiveOrder order) {
        if (order.getEventId() == null) return null;
        return eventRepository.findById(order.getEventId())
                .map(event -> event.getCompanyId().intValue())
                .orElse(null);
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

        if (policy == null) return;

        int quantity = order.getItems().size();
        if (!policy.validatePurchase(quantity, false)) {
            throw new IllegalStateException("Order rejected by purchase policy");
        }
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