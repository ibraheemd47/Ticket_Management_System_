package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents the Discount Policy of a company or an event.
 * Primarily supports Version 1 requirements for price calculations.
 */
public class DiscountPolicy extends Policy {

    /**
     * Corresponds to Section 3: Discount Types (Discount Rules)
     * Provides a contract for various calculation logic (Percentage, Conditional, etc.).
     */
    @FunctionalInterface
    public interface DiscountRule {
        /**
         * @param price The original price of the tickets.
         * @param quantity The number of tickets being purchased.
         * @param couponCode Optional coupon code entered during checkout.
         * @return The price after applying the specific discount.
         */
        double apply(double price, int quantity, String couponCode);
    }

    // List of active discount rules. Default is empty (No Discount).
    private final List<DiscountRule> activeDiscounts = new ArrayList<>();
    
    /**
     * Corresponds to Section 4: Discount Policy and Composition
     * Determines if discounts should be cumulative or if the best one should be chosen.
     */
    private boolean isAdditive = false; 

    public DiscountPolicy(int policyId, String description) {
        super(policyId, description);
    }


    // ==========================================================
    // USE CASE II.2.8: Checkout Active Order
    // ==========================================================
    /**
     * This method implements the logic for Use Case II.2.8.
     * The system applies the discount policy to calculate the final payment amount.
     * * @param originalPrice Base price before any discounts.
     * @param quantity Number of items being purchased.
     * @param coupon The coupon code provided by the user at checkout.
     * @return Final price after applying all relevant policy rules.
     */
    public double calculateFinalPrice(double originalPrice, int quantity, String coupon) {
        validateSubtotal(originalPrice);

        // Default behavior: If no discounts are set, return the original price.
        if (activeDiscounts.isEmpty()) {
            return originalPrice;
        }

        if (isAdditive) {
            // Logic for Additive Composition: Apply all discounts sequentially.
            double currentPrice = originalPrice;
            for (DiscountRule rule : activeDiscounts) {
                currentPrice = rule.apply(currentPrice, quantity, coupon);
            }
            return currentPrice;

        } else {
            // Logic for "Best Value": Select the single best discount for the customer.
            double minPrice = originalPrice;
            for (DiscountRule rule : activeDiscounts) {
                double result = rule.apply(originalPrice, quantity, coupon);
                minPrice = Math.min(minPrice, result);
            }
            return minPrice;
        }
    }

    /**
     *  validation  of data for Version 1.
     */
    @Override
    public boolean isValid() {
        return true; // For Version 1, we assume no discount policies .
    }

    private void validateSubtotal(double subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative");
        }
    }

    /**
     * Management logic to toggle between Additive and Selection modes. - not for version 1, but for future use.
     */
    // public void setAdditive(boolean additive) {
    //     this.isAdditive = additive;
    // }

    public boolean hasDiscounts() {
        return !activeDiscounts.isEmpty();
    }

    @Override
    public String toString() {
        return "DiscountPolicy{" +
                "id=" + getPolicyId() +
                ", desc='" + getDescription() + '\'' +
                ", activeRulesCount=" + activeDiscounts.size() +
                ", isAdditive=" + isAdditive +
                '}';
    }


    // ==========================================================
    // IMPLEMENTATION OF DISCOUNT TYPES (Section 3) 
    // ==========================================================

    /**
     * 3.a: Percentage Discount (הנחה גלויה).
     * Applied as a percentage of the original price
     */
    public static class PercentageDiscount implements DiscountRule {
        private final double percentage;

        public PercentageDiscount(double percentage) {
            this.percentage = percentage;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            return price * (1 - (percentage / 100));
        }
    }

    /**
     * 3.b: Conditional Discount (הנחה מותנית).
     * Applied only if a specific condition is met, e.g., "Buy 3 get 1 free".
     */
    public static class ConditionalDiscount implements DiscountRule {
        private final int requiredQuantity;
        private final double discountPercentage;

        public ConditionalDiscount(int requiredQuantity, double discountPercentage) {
            this.requiredQuantity = requiredQuantity;
            this.discountPercentage = discountPercentage;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            // Example logic: if quantity is enough, apply discount
            if (quantity >= requiredQuantity) {
                return price * (1 - (discountPercentage / 100));
            }
            return price;
        }
    }

    /**
     * 3.c: Coupon / Hidden Discount (קוד קופון).
     * Applied only if the provided coupon code matches.
     */
    public static class CouponDiscount implements DiscountRule {
        private final String validCode;
        private final double discountAmount;

        public CouponDiscount(String code, double amount) {
            this.validCode = code;
            this.discountAmount = amount;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            // Applies discount only if the code matches during Checkout [cite: 1699]
            if (validCode.equals(couponCode)) {
                return Math.max(0, price - discountAmount);
            }
            return price;
        }
    }
    // for test
    public void addDiscount(DiscountRule rule) {
    if (rule != null) {
        this.activeDiscounts.add(rule);
    }
}
}