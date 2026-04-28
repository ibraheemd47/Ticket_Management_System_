package com.sdnah.Ticket_Management_System_.DTOs.Policy;

public class DiscountRuleDTO {

    private String ruleType; // "PERCENTAGE", "CONDITIONAL", "COUPON"
    private double discountPercentage;
    private double discountAmount;
    private Integer requiredQuantity;
    private String couponCode;

    public DiscountRuleDTO() {
    }

    public DiscountRuleDTO(
            String ruleType,
            double discountPercentage,
            double discountAmount,
            Integer requiredQuantity,
            String couponCode) {

        this.ruleType = ruleType;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.requiredQuantity = requiredQuantity;
        this.couponCode = couponCode;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Integer requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}