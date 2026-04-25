package com.sdnah.Ticket_Management_System_.DTOs.Policy;


import java.util.List;

public class DiscountPolicyDTO {
    private int eventId;
    private boolean isAdditive;
    private List<DiscountRuleDTO> rules;

    public static class DiscountRuleDTO {
        private String ruleType; // "CONDITIONAL" או "COUPON"
        private double discountPercentage;
        private Integer requiredQuantity; // עבור הנחה מותנית
        private String couponCode; // עבור הנחת קופון
        
        // Getters and Setters
    }

    // Getters and Setters
    public int getEventId() { return eventId; }
    public boolean isAdditive() { return isAdditive; }
    public List<DiscountRuleDTO> getRules() { return rules; }
}