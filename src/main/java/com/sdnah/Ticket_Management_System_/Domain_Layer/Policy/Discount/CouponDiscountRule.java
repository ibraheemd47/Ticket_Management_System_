package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("COUPON")
public class CouponDiscountRule extends DiscountRule {

    @Column(name = "percentage")
    private double percentage;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "expiry")
    private LocalDateTime expiry;   // null = never expires

    protected CouponDiscountRule() {}

    public CouponDiscountRule(double percentage, String couponCode, LocalDateTime expiry) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        if (couponCode == null || couponCode.isBlank())
            throw new IllegalArgumentException("couponCode must not be blank");
        this.percentage = percentage;
        this.couponCode = couponCode.trim();
        this.expiry     = expiry;
    }

    public CouponDiscountRule(double percentage, String couponCode) {
        this(percentage, couponCode, null);
    }

    @Override
    public double apply(DiscountContext context) {
        if (!context.hasCoupon())                                            return 0.0;
        if (!couponCode.equalsIgnoreCase(context.getCouponCode().trim()))    return 0.0;
        if (expiry != null && context.getPurchaseTime().isAfter(expiry))     return 0.0;
        return percentage;
    }

    @Override
    public String describe() {
        String base = percentage + "% discount with coupon \"" + couponCode + "\"";
        return expiry != null ? base + " (valid until " + expiry + ")" : base;
    }

    public double        getPercentage() { return percentage; }
    public String        getCouponCode() { return couponCode; }
    public LocalDateTime getExpiry()     { return expiry; }
}