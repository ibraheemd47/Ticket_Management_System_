package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount;

import jakarta.persistence.*;

// =========================================================================
// Base abstraction for all discount rules.
// =========================================================================
@Entity
@Table(name = "discount_rules")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rule_type")
public abstract class DiscountRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    // Returns discount percentage
    public abstract double apply(DiscountContext context);

    public abstract String describe();

    public boolean isValid() {
        return true;
    }
}