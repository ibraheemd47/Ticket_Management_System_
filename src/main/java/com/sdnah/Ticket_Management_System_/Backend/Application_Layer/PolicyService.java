package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepo;

    @Autowired
    public PolicyService(PolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    // =========================================================================
    // UC II.2.5 — Select Tickets for Event
    // =========================================================================
    public boolean checkSelectionPermission(int companyId, UUID eventId, boolean isMember) {
        logger.info("Check selection permission, companyId={}, eventId={}, isMember={}",
                companyId, eventId, isMember);
        boolean result = findSellingPolicy(eventId)
                .map(policy -> policy.isSelectionAllowed(isMember))
                .orElse(true);
        if (!result) logger.warn("Selection rejected, eventId={}", eventId);
        return result;
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================
    public boolean validateReservationRequest(int companyId, UUID eventId,
                                              int quantity, int userAge) {
        logger.info("Validate reservation, companyId={}, eventId={}, quantity={}, age={}",
                companyId, eventId, quantity, userAge);
        boolean result = findPurchasePolicy(eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);
        if (!result) logger.warn("Reservation rejected, eventId={}", eventId);
        return result;
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (purchase validation)
    // =========================================================================
    public boolean validateFinalPurchaseConditions(int companyId, UUID eventId,
                                                    int quantity, int userAge) {
        logger.info("Validate final purchase, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);
        return findPurchasePolicy(eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (coupon discount)
    // =========================================================================
    public double calculateCouponDiscount(int companyId, UUID eventId, double currentTotal,
                                          int totalItems, String couponCode) {
        logger.info("Calculate coupon discount, companyId={}, eventId={}", companyId, eventId);
        if (couponCode == null || couponCode.isBlank()) return currentTotal;

        DiscountContext context = new DiscountContext(
                totalItems, LocalDateTime.now(), couponCode, currentTotal, eventId);

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeFinalPrice(currentTotal, context))
                .orElse(currentTotal);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (general discounts, no coupon)
    // =========================================================================
    public double applyGeneralDiscounts(int companyId, UUID eventId,
                                        double basePrice, int totalItems) {
        logger.info("Apply general discounts, companyId={}, eventId={}", companyId, eventId);

        DiscountContext context = new DiscountContext(
                totalItems, LocalDateTime.now(), null, basePrice, eventId);

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeFinalPrice(basePrice, context))
                .orElse(basePrice);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (conditional discount check)
    // =========================================================================
    public boolean isConditionalDiscountSatisfied(int companyId, UUID eventId, int quantity) {
        logger.info("Check conditional discount, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);

        DiscountContext context = new DiscountContext(
                quantity, LocalDateTime.now(), null, 0.0, eventId);

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeDiscount(context) > 0.0)
                .orElse(false);
    }

    // =========================================================================
    // UC II.4.3 — Add a discount rule to an event policy
    // =========================================================================
    public void addDiscountRule(UUID eventId, DiscountRule newRule) {
        logger.info("Add discount rule, eventId={}, rule={}", eventId, newRule.describe());

        DiscountPolicy policy = findDiscountPolicy(eventId).orElseThrow(() -> {
            logger.error("No discount policy found for eventId={}", eventId);
            return new IllegalArgumentException("No discount policy found for eventId=" + eventId);
        });

        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Discount rule added, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Replace all discount rules for an event policy
    // =========================================================================
    public void setDiscountRules(UUID eventId, List<DiscountRule> rules, boolean isAdditive) {
        logger.info("Set discount rules, eventId={}, count={}, additive={}",
                eventId, rules.size(), isAdditive);

        DiscountPolicy policy = findDiscountPolicy(eventId).orElseThrow(() -> {
            logger.error("No discount policy found for eventId={}", eventId);
            return new IllegalArgumentException("No discount policy found for eventId=" + eventId);
        });

        policy.setRules(rules, isAdditive);
        policyRepo.savePolicy(policy);
        logger.info("Discount rules set, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Clear all discount rules for an event policy
    // =========================================================================
    public void clearDiscountRules(UUID eventId) {
        logger.info("Clear discount rules, eventId={}", eventId);

        DiscountPolicy policy = findDiscountPolicy(eventId).orElseThrow(() -> {
            logger.error("No discount policy found for eventId={}", eventId);
            return new IllegalArgumentException("No discount policy found for eventId=" + eventId);
        });

        policy.clearRules();
        policyRepo.savePolicy(policy);
        logger.info("Discount rules cleared, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Add a purchase rule to an event policy
    // =========================================================================
    public void addPurchaseRule(UUID eventId, PurchaseRule newRule) {
        logger.info("Add purchase rule, eventId={}, rule={}", eventId, newRule.describe());

        PurchasePolicy policy = findPurchasePolicy(eventId).orElseThrow(() -> {
            logger.error("No purchase policy found for eventId={}", eventId);
            return new IllegalArgumentException("No purchase policy found for eventId=" + eventId);
        });

        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rule added, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Replace all purchase rules for an event policy
    // =========================================================================
    public void setPurchaseRules(UUID eventId, List<PurchaseRule> rules,
                                  PurchasePolicy.Operator operator) {
        logger.info("Set purchase rules, eventId={}, count={}, operator={}",
                eventId, rules.size(), operator);

        PurchasePolicy policy = findPurchasePolicy(eventId).orElseThrow(() -> {
            logger.error("No purchase policy found for eventId={}", eventId);
            return new IllegalArgumentException("No purchase policy found for eventId=" + eventId);
        });

        policy.setRules(rules, operator);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rules set, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Clear all purchase rules for an event policy
    // =========================================================================
    public void clearPurchaseRules(UUID eventId) {
        logger.info("Clear purchase rules, eventId={}", eventId);

        PurchasePolicy policy = findPurchasePolicy(eventId).orElseThrow(() -> {
            logger.error("No purchase policy found for eventId={}", eventId);
            return new IllegalArgumentException("No purchase policy found for eventId=" + eventId);
        });

        policy.clearRules();
        policyRepo.savePolicy(policy);
        logger.info("Purchase rules cleared, eventId={}", eventId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Optional<SellingPolicy> findSellingPolicy(UUID eventId) {
        Object result = policyRepo.findSellingPolicyByEventId(eventId);
        if (result == null) return Optional.empty();
        if (result instanceof Optional) return (Optional<SellingPolicy>) result;
        return Optional.of((SellingPolicy) result);
    }

    private Optional<PurchasePolicy> findPurchasePolicy(UUID eventId) {
        Object result = policyRepo.findPurchasePolicyByEventId(eventId);
        if (result == null) return Optional.empty();
        if (result instanceof Optional) return (Optional<PurchasePolicy>) result;
        return Optional.of((PurchasePolicy) result);
    }

    private Optional<DiscountPolicy> findDiscountPolicy(UUID eventId) {
        Object result = policyRepo.findDiscountPolicyByEventId(eventId);
        if (result == null) return Optional.empty();
        if (result instanceof Optional) return (Optional<DiscountPolicy>) result;
        return Optional.of((DiscountPolicy) result);
    }
}