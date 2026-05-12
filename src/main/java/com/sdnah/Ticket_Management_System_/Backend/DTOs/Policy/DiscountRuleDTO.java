package com.sdnah.Ticket_Management_System_.Backend.DTOs.Policy;

/**
 * DTO representing the different types of discount rules found in DiscountPolicy.java.
 * This class captures data for Percentage, Conditional, and Coupon discounts.
 */
public class DiscountRuleDTO {
    
    public enum RuleType {
        PERCENTAGE,
        CONDITIONAL,
        COUPON
    }

    private RuleType ruleType;
    
    // Used for Percentage and Conditional discounts
    private Double discountPercentage;
    
    // Used for Conditional discounts (e.g., "Buy X tickets")
    private Integer requiredQuantity;
    
    // Used for Coupon discounts
    private String couponCode;
    private Double discountAmount;

    public DiscountRuleDTO() {}

    /**
     * Constructor for a Percentage Discount rule.
     */
    public static DiscountRuleDTO percentage(double percentage) {
        DiscountRuleDTO dto = new DiscountRuleDTO();
        dto.setRuleType(RuleType.PERCENTAGE);
        dto.setDiscountPercentage(percentage);
        return dto;
    }

    /**
     * Constructor for a Conditional Discount rule.
     */
    public static DiscountRuleDTO conditional(int requiredQuantity, double percentage) {
        DiscountRuleDTO dto = new DiscountRuleDTO();
        dto.setRuleType(RuleType.CONDITIONAL);
        dto.setRequiredQuantity(requiredQuantity);
        dto.setDiscountPercentage(percentage);
        return dto;
    }

    /**
     * Constructor for a Coupon Discount rule.
     */
    public static DiscountRuleDTO coupon(String code, double amount) {
        DiscountRuleDTO dto = new DiscountRuleDTO();
        dto.setRuleType(RuleType.COUPON);
        dto.setCouponCode(code);
        dto.setDiscountAmount(amount);
        return dto;
    }

    // Getters and Setters
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public Integer getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(Integer requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }
}