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

    public boolean checkSelectionPermission(int eventId, boolean isMember) {
        logger.info("Check selection permission request received, eventId={}, isMember={}", eventId, isMember);

        boolean result = findSellingPolicyByEventId(eventId)
                .map(policy -> policy.isSelectionAllowed(isMember))
                .orElse(true);

        if (!result) {
            logger.warn("Selection rejected by selling policy, eventId={}, isMember={}", eventId, isMember);
        } else {
            logger.info("Selection permission approved, eventId={}, isMember={}", eventId, isMember);
        }

        return result;
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================

    public boolean validateReservationRequest(int eventId, int quantity, int userAge) {
        logger.info(
                "Validate reservation request received, eventId={}, quantity={}, userAge={}",
                eventId,
                quantity,
                userAge
        );

        boolean result = findPurchasePolicyByEventId(eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);

        if (!result) {
            logger.warn(
                    "Reservation rejected by purchase policy, eventId={}, quantity={}, userAge={}",
                    eventId,
                    quantity,
                    userAge
            );
        } else {
            logger.info(
                    "Reservation approved by purchase policy, eventId={}, quantity={}, userAge={}",
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

    public boolean validateFinalPurchaseConditions(int eventId, int quantity, int userAge) {
        logger.info(
                "Validate final purchase conditions request received, eventId={}, quantity={}, userAge={}",
                eventId,
                quantity,
                userAge
        );

        boolean result = findPurchasePolicyByEventId(eventId)
                .map(policy -> policy.validatePurchase(quantity, userAge, false))
                .orElse(true);

        if (!result) {
            logger.warn(
                    "Final purchase conditions rejected by purchase policy, eventId={}, quantity={}, userAge={}",
                    eventId,
                    quantity,
                    userAge
            );
        } else {
            logger.info(
                    "Final purchase conditions approved, eventId={}, quantity={}, userAge={}",
                    eventId,
                    quantity,
                    userAge
            );
        }

        return result;
    }

    public double calculateCouponDiscount(int eventId, double currentTotal, int totalItems, String couponCode) {
        logger.info(
                "Calculate coupon discount request received, eventId={}, currentTotal={}, totalItems={}, couponProvided={}",
                eventId,
                currentTotal,
                totalItems,
                couponCode != null && !couponCode.isEmpty()
        );

        if (couponCode == null || couponCode.isEmpty()) {
            logger.info("No coupon provided, returning original total, eventId={}, total={}", eventId, currentTotal);
            return currentTotal;
        }

        double result = findDiscountPolicyByEventId(eventId)
                .map(policy -> policy.calculateFinalPrice(currentTotal, totalItems, couponCode))
                .orElse(currentTotal);

        logger.info(
                "Coupon discount calculation completed, eventId={}, originalTotal={}, finalTotal={}",
                eventId,
                currentTotal,
                result
        );

        return result;
    }

    public double applyGeneralDiscounts(int eventId, double basePrice, int totalItems) {
        logger.info(
                "Apply general discounts request received, eventId={}, basePrice={}, totalItems={}",
                eventId,
                basePrice,
                totalItems
        );

        double result = findDiscountPolicyByEventId(eventId)
                .map(policy -> policy.calculateFinalPrice(basePrice, totalItems, ""))
                .orElse(basePrice);

        logger.info(
                "General discount calculation completed, eventId={}, originalPrice={}, finalPrice={}",
                eventId,
                basePrice,
                result
        );

        return result;
    }

    public boolean isConditionalDiscountSatisfied(int eventId, int quantity) {
        logger.info(
                "Check conditional discount request received, eventId={}, quantity={}",
                eventId,
                quantity
        );

        boolean result = findDiscountPolicyByEventId(eventId)
                .map(policy -> policy.isAnyConditionalDiscountSatisfied(quantity))
                .orElse(false);

        logger.info(
                "Conditional discount check completed, eventId={}, quantity={}, result={}",
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

    private Optional<SellingPolicy> findSellingPolicyByEventId(int eventId) {
        return policyRepo.findAll().stream()
                .filter(policy -> policy.getEventId() == eventId)
                .filter(policy -> policy instanceof SellingPolicy)
                .map(policy -> (SellingPolicy) policy)
                .findFirst();
    }

    private Optional<PurchasePolicy> findPurchasePolicyByEventId(int eventId) {
        return policyRepo.findAll().stream()
                .filter(policy -> policy.getEventId() == eventId)
                .filter(policy -> policy instanceof PurchasePolicy)
                .map(policy -> (PurchasePolicy) policy)
                .findFirst();
    }

    private Optional<DiscountPolicy> findDiscountPolicyByEventId(int eventId) {
        return policyRepo.findAll().stream()
                .filter(policy -> policy.getEventId() == eventId)
                .filter(policy -> policy instanceof DiscountPolicy)
                .map(policy -> (DiscountPolicy) policy)
                .findFirst();
    }
}



// public class PolicyService {
//   private final IPolicyRepo policyRepo;

//     @Autowired
//     public PolicyService(IPolicyRepo policyRepo) {
//         this.policyRepo = policyRepo;
//     }


//     // --- II.2.5: Select Tickets for Event ---
//     public boolean checkSelectionPermission(int eventId, boolean isMember) {
//         // Retrieve all policies for the event and filter for SellingPolicy
//         return policyRepo.findAll().stream()
//                 .filter(p -> p.getEventId() == eventId && p instanceof SellingPolicy)
//                 .map(p -> ((SellingPolicy) p).isSelectionAllowed(isMember))
//                 .findFirst()
//                 .orElse(true); 
//     }


//     // --- II.2.4: Reserve Tickets in Active Order ---
//     public boolean validateReservationRequest(int eventId, int quantity, int userAge) {
//             return policyRepo.findAll().stream()
//                     .filter(p -> p.getEventId() == eventId && p instanceof PurchasePolicy)
//                     .map(p -> ((PurchasePolicy) p).validatePurchase(quantity, userAge, false))
//                     .findFirst()
//                     .orElse(true);
//         }

//     // --- II.2.8: Checkout Active Order ---

//     //Validate Purchase Conditions.
//     public boolean validateFinalPurchaseConditions(int eventId, int quantity, int userAge) {
//         return policyRepo.findAll().stream()
//                 .filter(p -> p.getEventId() == eventId && p instanceof PurchasePolicy)
//                 .map(p -> ((PurchasePolicy) p).validatePurchase(quantity, userAge, false))
//                 .findFirst()
//                 .orElse(true); // Default to allowed if no policy exists
//     }

//     public double calculateCouponDiscount(int eventId, double currentTotal, int totalItems, String couponCode) {
//         if (couponCode == null || couponCode.isEmpty()) {
//             return currentTotal;
//         }
//         return policyRepo.findAll().stream()
//                 .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
//                 .map(p -> ((DiscountPolicy) p).calculateFinalPrice(currentTotal, totalItems, couponCode))
//                 .findFirst()
//                 .orElse(currentTotal);
//     }
   

//     public double applyGeneralDiscounts(int eventId, double basePrice, int totalItems) {
//         return policyRepo.findAll().stream()
//                 .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
//                 .map(p -> ((DiscountPolicy) p).calculateFinalPrice(basePrice, totalItems, "")) // No coupon code for general discounts
//                 .findFirst()
//                 .orElse(basePrice);
//     }

//     //new:
//     public boolean isConditionalDiscountSatisfied(int eventId, int quantity) 
//     {
//         return policyRepo.findAll().stream()
//                 .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
//                 .map(p -> ((DiscountPolicy) p).isAnyConditionalDiscountSatisfied(quantity))
//                 .findFirst()
//                 .orElse(false);
//     }

// }

    
