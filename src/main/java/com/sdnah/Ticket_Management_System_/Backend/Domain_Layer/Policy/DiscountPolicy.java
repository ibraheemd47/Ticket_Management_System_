package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("DISCOUNT")
public class DiscountPolicy extends Policy {

    @Column(name = "is_additive")
    private boolean isAdditive = false;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "policy_id")
    private List<DiscountRuleEntity> activeDiscounts = new ArrayList<>();

    protected DiscountPolicy() {
        // JPA
    }

    public DiscountPolicy(int policyId, String description, UUID eventId) {
        super(policyId, description, eventId);
    }

    // ================= Persisted Rule Entity =================

    @Entity
    @Table(name = "discount_rules")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(name = "rule_type")
    public static abstract class DiscountRuleEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        protected DiscountRuleEntity() {}

        public Long getId() { return id; }

        public abstract double apply(double price, int quantity, String couponCode);

        public boolean isConditionMet(int quantity) {
            return true;
        }
    }

    // ================= Functional Interface (kept for backward compatibility) =================

    @FunctionalInterface
    public interface DiscountRule {
        double apply(double price, int quantity, String couponCode);

        default boolean isConditionMet(int quantity) {
            return true;
        }
    }

    // ================= Discount Types (Entities) =================

    @Entity
    @DiscriminatorValue("PERCENTAGE")
    public static class PercentageDiscount extends DiscountRuleEntity implements DiscountRule {

        @Column(name = "percentage")
        private double percentage;

        protected PercentageDiscount() {}

        public PercentageDiscount(double percentage) {
            this.percentage = percentage;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            return price * (1 - percentage / 100);
        }
    }

    @Entity
    @DiscriminatorValue("CONDITIONAL")
    public static class ConditionalDiscount extends DiscountRuleEntity implements DiscountRule {

        @Column(name = "required_quantity")
        private int requiredQuantity;

        @Column(name = "discount_percentage")
        private double discountPercentage;

        protected ConditionalDiscount() {}

        public ConditionalDiscount(int requiredQuantity, double discountPercentage) {
            this.requiredQuantity = requiredQuantity;
            this.discountPercentage = discountPercentage;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            return isConditionMet(quantity)
                    ? price * (1 - discountPercentage / 100)
                    : price;
        }

        @Override
        public boolean isConditionMet(int quantity) {
            return quantity >= requiredQuantity;
        }
    }

    @Entity
    @DiscriminatorValue("COUPON")
    public static class CouponDiscount extends DiscountRuleEntity implements DiscountRule {

        @Column(name = "coupon_code")
        private String code;

        @Column(name = "discount_amount")
        private double discount;

        protected CouponDiscount() {}

        public CouponDiscount(String code, double discount) {
            this.code = code;
            this.discount = discount;
        }

        @Override
        public double apply(double price, int quantity, String couponCode) {
            return code.equals(couponCode) ? Math.max(0, price - discount) : price;
        }
    }

    // ================= Business Logic (unchanged) =================

    public boolean isAnyConditionalDiscountSatisfied(int quantity) {
        for (DiscountRuleEntity rule : activeDiscounts) {
            if (rule instanceof ConditionalDiscount && rule.isConditionMet(quantity)) {
                return true;
            }
        }
        return false;
    }

    public double calculateFinalPrice(double originalPrice, int quantity, String coupon) {
        if (activeDiscounts.isEmpty()) return originalPrice;

        if (isAdditive) {
            double price = originalPrice;
            for (DiscountRuleEntity rule : activeDiscounts) {
                price = rule.apply(price, quantity, coupon);
            }
            return price;
        } else {
            double minPrice = originalPrice;
            for (DiscountRuleEntity rule : activeDiscounts) {
                minPrice = Math.min(minPrice, rule.apply(originalPrice, quantity, coupon));
            }
            return minPrice;
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void addDiscount(DiscountRuleEntity rule) {
        if (rule != null) activeDiscounts.add(rule);
    }

    public boolean isAdditive() {
        return isAdditive;
    }

    public void setAdditive(boolean additive) {
        isAdditive = additive;
    }

    public List<DiscountRuleEntity> getActiveDiscounts() {
        return activeDiscounts;
    }
}