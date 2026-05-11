package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

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
        logger.info("Check selection permission request received, companyId={}, eventId={}, isMember={}",
                companyId, eventId, isMember);

        boolean result = findSellingPolicy(eventId)
                .map(policy -> policy.isSelectionAllowed(isMember))
                .orElse(true);

        if (!result) {
            logger.warn("Selection rejected by selling policy, companyId={}, eventId={}, isMember={}",
                    companyId, eventId, isMember);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================
    public boolean validateReservationRequest(int companyId, UUID eventId, int quantity, int userAge) {
        logger.info("Validate reservation request received, companyId={}, eventId={}, quantity={}, userAge={}",
                companyId, eventId, quantity, userAge);

        boolean result = findPurchasePolicy(eventId)
                .map(policy -> policy.validatePurchase(quantity, false))
                .orElse(true);

        if (!result) {
            logger.warn("Reservation rejected by purchase policy, companyId={}, eventId={}, quantity={}, userAge={}",
                    companyId, eventId, quantity, userAge);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // =========================================================================
    public boolean validateFinalPurchaseConditions(int companyId, UUID eventId, int quantity) {
        logger.info("Validate final purchase conditions request received, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);

        boolean result = findPurchasePolicy(eventId)
                .map(policy -> policy.validatePurchase(quantity, false))
                .orElse(true);

        if (!result) {
            logger.warn("Final purchase conditions rejected by purchase policy, companyId={}, eventId={}, quantity={}",
                    companyId, eventId, quantity);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (Coupon Discount)
    // =========================================================================
    public double calculateCouponDiscount(int companyId, UUID eventId, double currentTotal,
                                          int totalItems, String couponCode) {

        logger.info("Calculate coupon discount request received, companyId={}, eventId={}, totalItems={}",
                companyId, eventId, totalItems);

        if (couponCode == null || couponCode.isBlank()) {
            return currentTotal;
        }

        return findDiscountPolicy(eventId)
                .map(policy -> policy.calculateFinalPrice(currentTotal, totalItems, couponCode))
                .orElse(currentTotal);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (General Discounts)
    // =========================================================================
    public double applyGeneralDiscounts(int companyId, UUID eventId, double basePrice, int totalItems) {

        logger.info("Apply general discounts request received, companyId={}, eventId={}, basePrice={}, totalItems={}",
                companyId, eventId, basePrice, totalItems);

        return findDiscountPolicy(eventId)
                .map(policy -> policy.calculateFinalPrice(basePrice, totalItems, ""))
                .orElse(basePrice);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order (Conditional Discount Check)
    // =========================================================================
    public boolean isConditionalDiscountSatisfied(int companyId, UUID eventId, int quantity) {

        logger.info("Check conditional discount request received, companyId={}, eventId={}, quantity={}",
                companyId, eventId, quantity);

        return findDiscountPolicy(eventId)
                .map(policy -> policy.isAnyConditionalDiscountSatisfied(quantity))
                .orElse(false);
    }

    // =========================================================================
    // UC II.4.3 — Change Purchase and Discount Policies (Not implemented)
    // =========================================================================
    public void changePurchasePolicy() {
        logger.warn("Change purchase policy requested but not implemented");
        throw new UnsupportedOperationException("Not implemented");
    }

    public void changeDiscountPolicy() {
        logger.warn("Change discount policy requested but not implemented");
        throw new UnsupportedOperationException("Not implemented");
    }

    // =========================================================================
    // Private helpers (no more findAll)
    // =========================================================================

    private Optional<SellingPolicy> findSellingPolicy(UUID eventId) {
        return Optional.ofNullable(policyRepo.findSellingPolicyByEventId(eventId));
    }

    private Optional<PurchasePolicy> findPurchasePolicy(UUID eventId) {
        return Optional.ofNullable(policyRepo.findPurchasePolicyByEventId(eventId));
    }

    private Optional<DiscountPolicy> findDiscountPolicy(UUID eventId) {
        return Optional.ofNullable(policyRepo.findDiscountPolicyByEventId(eventId));
    }
}