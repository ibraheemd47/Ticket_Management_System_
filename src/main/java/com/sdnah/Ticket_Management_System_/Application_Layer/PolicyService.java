package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private final IPolicyRepo policyRepo;

    @Autowired
    public PolicyService(IPolicyRepo policyRepo) {
        this.policyRepo = policyRepo;
    }

    // =========================================================================
    // UC II.2.5 — Select Tickets for Event
    // =========================================================================

    public boolean checkSelectionPermission(int companyId, int eventId, boolean isMember) {
        logger.info("Check selection permission request received, companyId={}, eventId={}, isMember={}", companyId, eventId, isMember);

        boolean result = findSellingPolicy( eventId)
                .map(policy -> policy.isSelectionAllowed(isMember))
                .orElse(true);

        if (!result) {
            logger.warn("Selection rejected by selling policy, companyId={}, eventId={}, isMember={}", companyId, eventId, isMember);
        } else {
            logger.info("Selection permission approved, companyId={}, eventId={}, isMember={}", companyId, eventId, isMember);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================

    public boolean validateReservationRequest(int companyId, int eventId, int quantity, int userAge) {
        logger.info(
                "Validate reservation request received, companyId={}, eventId={}, quantity={}, userAge={}",
                companyId,
                eventId,
                quantity,
                userAge
        );

        boolean result = findPurchasePolicy( eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);

        if (!result) {
            logger.warn(
                    "Reservation rejected by purchase policy, companyId={}, eventId={}, quantity={}, userAge={}",
                    companyId,
                    eventId,
                    quantity,
                    userAge
            );
        } else {
            logger.info(
                    "Reservation approved by purchase policy, companyId={}, eventId={}, quantity={}, userAge={}",
                    companyId,
                    eventId,
                    quantity,
                    userAge
            );
        }

        return result;
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // =========================================================================

    public boolean validateFinalPurchaseConditions(int companyId, int eventId, int quantity, int userAge) {
        logger.info(
                "Validate final purchase conditions request received, companyId={}, eventId={}, quantity={}, userAge={}",
                companyId,
                eventId,
                quantity,
                userAge
        );

        boolean result = findPurchasePolicy( eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);

        if (!result) {
            logger.warn(
                    "Final purchase conditions rejected by purchase policy, companyId={}, eventId={}, quantity={}, userAge={}",
                    companyId,
                    eventId,
                    quantity,
                    userAge
            );
        } else {
            logger.info(
                    "Final purchase conditions approved, companyId={}, eventId={}, quantity={}, userAge={}",
                    companyId,
                    eventId,
                    quantity,
                    userAge
            );
        }

        return result;
    }

    public double calculateCouponDiscount(int companyId, int eventId, double currentTotal, int totalItems, String couponCode) {
        logger.info(
                "Calculate coupon discount request received, companyId={}, eventId={}, currentTotal={}, totalItems={}, couponProvided={}",
                companyId,
                eventId,
                currentTotal,
                totalItems,
                couponCode != null && !couponCode.isEmpty()
        );

        if (couponCode == null || couponCode.isEmpty()) {
            logger.info("No coupon provided, returning original total, companyId={}, eventId={}, total={}", companyId, eventId, currentTotal);
            return currentTotal;
        }

        double result = findDiscountPolicy( eventId)
                .map(policy -> policy.calculateFinalPrice(currentTotal, totalItems, couponCode))
                .orElse(currentTotal);

        logger.info(
                "Coupon discount calculation completed, companyId={}, eventId={}, originalTotal={}, finalTotal={}",
                companyId,
                eventId,
                currentTotal,
                result
        );

        return result;
    }

    public double applyGeneralDiscounts(int companyId, int eventId, double basePrice, int totalItems) {
        logger.info(
                "Apply general discounts request received, companyId={}, eventId={}, basePrice={}, totalItems={}",
                companyId,
                eventId,
                basePrice,
                totalItems
        );

        double result = findDiscountPolicy( eventId)
                .map(policy -> policy.calculateFinalPrice(basePrice, totalItems, ""))
                .orElse(basePrice);

        logger.info(
                "General discount calculation completed, companyId={}, eventId={}, originalPrice={}, finalPrice={}",
                companyId,
                eventId,
                basePrice,
                result
        );

        return result;
    }

    public boolean isConditionalDiscountSatisfied(int companyId, int eventId, int quantity) {
        logger.info(
                "Check conditional discount request received, companyId={}, eventId={}, quantity={}",
                companyId,
                eventId,
                quantity
        );

        boolean result = findDiscountPolicy( eventId)
                .map(policy -> policy.isAnyConditionalDiscountSatisfied(quantity))
                .orElse(false);

        logger.info(
                "Conditional discount check completed, companyId={}, eventId={}, quantity={}, result={}",
                companyId,
                eventId,
                quantity,
                result
        );

        return result;
    }

    // =========================================================================
    // UC II.4.3 — Change Purchase and Discount Policies
    // Not implemented in Version 1.
    // =========================================================================

    public void changePurchasePolicy() {
        logger.warn("Change purchase policy requested but UC II.4.3 is not implemented in Version 1");
        throw new UnsupportedOperationException("Changing purchase policy is not implemented in Version 1");
    }

    public void changeDiscountPolicy() {
        logger.warn("Change discount policy requested but UC II.4.3 is not implemented in Version 1");
        throw new UnsupportedOperationException("Changing discount policy is not implemented in Version 1");
    }

    // =========================================================================
    // Private helpers 
    // =========================================================================

    private Optional<SellingPolicy> findSellingPolicy(int eventId) {
        Optional<SellingPolicy> eventPolicy = policyRepo.findAll().stream()
                .filter(p -> p instanceof SellingPolicy)
                .map(p -> (SellingPolicy) p)
                .filter(p -> p.getEventId() != null && p.getEventId().equals(eventId))
                .findFirst();

        if (eventPolicy.isPresent()) return eventPolicy;

        return policyRepo.findAll().stream()
                .filter(p -> p instanceof SellingPolicy)
                .map(p -> (SellingPolicy) p)
                .filter(p -> p.getEventId() == null)
                .findFirst();
    }

    private Optional<PurchasePolicy> findPurchasePolicy( int eventId) {
        Optional<PurchasePolicy> eventPolicy = policyRepo.findAll().stream()
                .filter(p -> p instanceof PurchasePolicy)
                .map(p -> (PurchasePolicy) p)
                .filter(p -> p.getEventId() != null && p.getEventId().equals(eventId))
                .findFirst();

        if (eventPolicy.isPresent()) return eventPolicy;

        return policyRepo.findAll().stream()
                .filter(p -> p instanceof PurchasePolicy)
                .map(p -> (PurchasePolicy) p)
                .filter(p -> p.getEventId() == null)
                .findFirst();
    }

    private Optional<DiscountPolicy> findDiscountPolicy( int eventId) {
        Optional<DiscountPolicy> eventPolicy = policyRepo.findAll().stream()
                .filter(p -> p instanceof DiscountPolicy)
                .map(p -> (DiscountPolicy) p)
                .filter(p -> p.getEventId() != null && p.getEventId().equals(eventId))
                .findFirst();

        if (eventPolicy.isPresent()) return eventPolicy;

        return policyRepo.findAll().stream()
                .filter(p -> p instanceof DiscountPolicy)
                .map(p -> (DiscountPolicy) p)
                .filter(p -> p.getEventId() == null)
                .findFirst();
    }
}