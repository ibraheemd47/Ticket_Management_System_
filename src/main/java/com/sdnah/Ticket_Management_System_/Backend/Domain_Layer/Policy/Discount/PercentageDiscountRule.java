package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PERCENTAGE")
public class PercentageDiscountRule extends DiscountRule {

    @Column(name = "percentage")
    private double percentage;

    @Column(name = "description")
    private String description;

    protected PercentageDiscountRule() {}

    public PercentageDiscountRule(double percentage, String description) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        this.percentage  = percentage;
        this.description = description != null ? description : percentage + "% discount";
    }

    @Override public double apply(DiscountContext context) { return percentage; }
    @Override public String describe()                     { return description; }

    public double getPercentage()  { return percentage; }
    public String getDescription() { return description; }
}