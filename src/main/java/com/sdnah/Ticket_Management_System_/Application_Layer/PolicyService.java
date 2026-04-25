package com.sdnah.Ticket_Management_System_.Application_Layer;

import org.springframework.beans.factory.annotation.Autowired;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import java.util.Optional;

public class PolicyService {
  private final IPolicyRepo policyRepo;

    @Autowired
    public PolicyService(IPolicyRepo policyRepo) {
        this.policyRepo = policyRepo;
    }


    // --- II.2.5: Select Tickets for Event ---
    public boolean checkSelectionPermission(int eventId, boolean isMember) {
        // Retrieve all policies for the event and filter for SellingPolicy
        return policyRepo.findAll().stream()
                .filter(p -> p.getEventId() == eventId && p instanceof SellingPolicy)
                .map(p -> ((SellingPolicy) p).isSelectionAllowed(isMember))
                .findFirst()
                .orElse(true); 
    }


    // --- II.2.4: Reserve Tickets in Active Order ---
    public boolean validateReservationRequest(int eventId, int quantity, int userAge) {
            return policyRepo.findAll().stream()
                    .filter(p -> p.getEventId() == eventId && p instanceof PurchasePolicy)
                    .map(p -> ((PurchasePolicy) p).validatePurchase(quantity, userAge, false))
                    .findFirst()
                    .orElse(true);
        }

    // --- II.2.8: Checkout Active Order ---

    //Validate Purchase Conditions.
    public boolean validateFinalPurchaseConditions(int eventId, int quantity, int userAge) {
        return policyRepo.findAll().stream()
                .filter(p -> p.getEventId() == eventId && p instanceof PurchasePolicy)
                .map(p -> ((PurchasePolicy) p).validatePurchase(quantity, userAge, false))
                .findFirst()
                .orElse(true); // Default to allowed if no policy exists
    }

    public double calculateCouponDiscount(int eventId, double currentTotal, int totalItems, String couponCode) {
        if (couponCode == null || couponCode.isEmpty()) {
            return currentTotal;
        }
        return policyRepo.findAll().stream()
                .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
                .map(p -> ((DiscountPolicy) p).calculateFinalPrice(currentTotal, totalItems, couponCode))
                .findFirst()
                .orElse(currentTotal);
    }
   

    public double applyGeneralDiscounts(int eventId, double basePrice, int totalItems) {
        return policyRepo.findAll().stream()
                .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
                .map(p -> ((DiscountPolicy) p).calculateFinalPrice(basePrice, totalItems, "")) // No coupon code for general discounts
                .findFirst()
                .orElse(basePrice);
    }

    //new:
    public boolean isConditionalDiscountSatisfied(int eventId, int quantity) 
    {
        return policyRepo.findAll().stream()
                .filter(p -> p.getEventId() == eventId && p instanceof DiscountPolicy)
                .map(p -> ((DiscountPolicy) p).isAnyConditionalDiscountSatisfied(quantity))
                .findFirst()
                .orElse(false);
    }


    //old:

      // --- II.2.5: Select Tickets for Event ---
//    public boolean checkSelectionPermission(int policyId, boolean isMember) {
//         return policyRepo.findById(policyId)
//                 .filter(p -> p instanceof SellingPolicy)
//                 .map(p -> ((SellingPolicy) p).isSelectionAllowed(isMember))
//                 .orElse(true); // Default to true if no specific selling policy exists
//     }
// --- II.2.4: Reserve Tickets in Active Order ---
//    public boolean validateReservationRequest(int policyId, int quantity, int userAge) {
//         return policyRepo.findById(policyId)
//                 .filter(p -> p instanceof PurchasePolicy)
//                 .map(p -> ((PurchasePolicy) p).validatePurchase(quantity, userAge, false))
//                 .orElse(true);
//     }
// --- II.2.8: Checkout Active Order ---
//    public double processCheckout(int purchasePolicyId, int discountPolicyId, double basePrice, 
//                                   int quantity, int userAge, String couponCode) {
        
//         // 1. Final verification of purchase policy 
//         Optional<Policy> purchasePolicy = policyRepo.findById(purchasePolicyId);
//         if (purchasePolicy.isPresent() && purchasePolicy.get() instanceof PurchasePolicy) {
//             if (!((PurchasePolicy) purchasePolicy.get()).validatePurchase(quantity, userAge, false)) {
//                 return -1.0; // Indicates policy violation at checkout
//             }
//         }

//         // 2. Apply discount logic 
//         return policyRepo.findById(discountPolicyId)
//                 .filter(p -> p instanceof DiscountPolicy)
//                 .map(p -> ((DiscountPolicy) p).calculateFinalPrice(basePrice, quantity, couponCode))
//                 .orElse(basePrice);

//     }

}
