package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("QUANTITY")
public class QuantityConditionalDiscountRule extends DiscountRule {

    @Column(name = "min_tickets")
    private int minTickets;

    @Column(name = "percentage")
    private double percentage;

    protected QuantityConditionalDiscountRule() {}

    public QuantityConditionalDiscountRule(int minTickets, double percentage) {
        if (minTickets <= 0)
            throw new IllegalArgumentException("minTickets must be positive");
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        this.minTickets = minTickets;
        this.percentage = percentage;
    }

    @Override
    public double apply(DiscountContext context) {
        return context.getTicketQuantity() >= minTickets ? percentage : 0.0;
    }

    @Override
    public String describe() {
        return percentage + "% discount when purchasing " + minTickets + " or more tickets";
    }

    public int    getMinTickets() { return minTickets; }
    public double getPercentage() { return percentage; }
}