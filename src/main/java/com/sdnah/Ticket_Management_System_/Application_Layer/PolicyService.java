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

        if (!result) {
            logger.warn("Selection rejected by selling policy, eventId={}", eventId);
        }

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
                .map(policy -> policy.validatePurchase(quantity, false))
                .orElse(true);

        if (!result) {
            logger.warn("Reservation rejected by purchase policy, eventId={}", eventId);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // =========================================================================
    public boolean validateFinalPurchaseConditions(int companyId, UUID eventId, int quantity) {
        logger.info("Validate final purchase, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);

        return findPurchasePolicy(eventId)
                .map(policy -> policy.validatePurchase(quantity, false))
                .orElse(true);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // Calculates discount using the generic DiscountContext.
    // =========================================================================
    public double calculateCouponDiscount(int companyId, UUID eventId, double currentTotal,
                                          int totalItems, String couponCode) {
        logger.info("Calculate coupon discount, companyId={}, eventId={}", companyId, eventId);

        if (couponCode == null || couponCode.isBlank()) {
            return currentTotal;
        }

        DiscountContext context = buildDiscountContext(
                companyId,
                eventId,
                currentTotal,
                totalItems,
                couponCode,
                null,
                null,
                null
        );

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeFinalPrice(currentTotal, context))
                .orElse(currentTotal);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // Applies visible/general discounts without coupon.
    // =========================================================================
    public double applyGeneralDiscounts(int companyId, UUID eventId,
                                        double basePrice, int totalItems) {
        logger.info("Apply general discounts, companyId={}, eventId={}", companyId, eventId);

        DiscountContext context = buildDiscountContext(
                companyId,
                eventId,
                basePrice,
                totalItems,
                null,
                null,
                null,
                null
        );

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeFinalPrice(basePrice, context))
                .orElse(basePrice);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // Checks whether any conditional discount is active.
    // =========================================================================
    public boolean isConditionalDiscountSatisfied(int companyId, UUID eventId, int quantity) {
        logger.info("Check conditional discount, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);

        DiscountContext context = buildDiscountContext(
                companyId,
                eventId,
                0.0,
                quantity,
                null,
                null,
                null,
                null
        );

        return findDiscountPolicy(eventId)
                .map(policy -> policy.computeDiscount(context) > 0.0)
                .orElse(false);
    }

    // =========================================================================
    // UC II.4.3 — Change Purchase and Discount Policies
    // Adds a discount rule to an event policy.
    // =========================================================================
    public void addDiscountRule(UUID eventId, DiscountRule newRule) {
        if (newRule == null) {
            throw new IllegalArgumentException("newRule must not be null");
        }

        logger.info("Add discount rule, eventId={}, rule={}", eventId, newRule.describe());

        DiscountPolicy policy = findDiscountPolicy(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No discount policy found for eventId=" + eventId));

        policy.addRule(newRule);
        policyRepo.save(policy);

        logger.info("Discount rule added successfully, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Change Purchase and Discount Policies
    // Replaces all discount rules for an event policy.
    // =========================================================================
    public void setDiscountRules(UUID eventId, List<DiscountRule> rules, boolean isAdditive) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }

        logger.info("Set discount rules, eventId={}, rules={}, additive={}",
                eventId, rules.size(), isAdditive);

        DiscountPolicy policy = findDiscountPolicy(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No discount policy found for eventId=" + eventId));

        policy.setRules(rules, isAdditive);
        policyRepo.save(policy);

        logger.info("Discount policy updated successfully, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Change Purchase and Discount Policies
    // Clears all discount rules for an event policy.
    // =========================================================================
    public void clearDiscountRules(UUID eventId) {
        logger.info("Clear discount rules, eventId={}", eventId);

        DiscountPolicy policy = findDiscountPolicy(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No discount policy found for eventId=" + eventId));

        policy.clearRules();
        policyRepo.save(policy);

        logger.info("Discount rules cleared, eventId={}", eventId);
    }

    // =========================================================================
    // Private helper: builds the generic context used by discount rules.
    // =========================================================================
    private DiscountContext buildDiscountContext(int companyId,
                                                 UUID eventId,
                                                 double originalPrice,
                                                 int quantity,
                                                 String couponCode,
                                                 String ticketType,
                                                 Integer buyerId,
                                                 Integer buyerAge) {
        return new DiscountContext(
                quantity,
                LocalDateTime.now(),
                couponCode,
                eventId,
                UUID.nameUUIDFromBytes(String.valueOf(companyId).getBytes()),
                originalPrice,
                ticketType,
                buyerId,
                buyerAge
        );
    }

    private Optional<SellingPolicy> findSellingPolicy(UUID eventId) {
        return policyRepo.findSellingPolicyByEventId2(eventId);
    }

    private Optional<PurchasePolicy> findPurchasePolicy(UUID eventId) {
        return policyRepo.findPurchasePolicyByEventId2(eventId);
    }

    private Optional<DiscountPolicy> findDiscountPolicy(UUID eventId) {
        return policyRepo.findDiscountPolicyByEventId2(eventId);
    }
}