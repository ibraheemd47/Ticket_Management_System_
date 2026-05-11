package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MIN_TICKETS")
public class MinTicketsRule extends PurchaseRule {

    @Column(name = "min_tickets")
    private int minTickets;

    protected MinTicketsRule() {}

    public MinTicketsRule(int minTickets) {
        if (minTickets <= 0) throw new IllegalArgumentException("minTickets must be positive");
        this.minTickets = minTickets;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getTicketQuantity() >= minTickets) return RuleResult.allowed();
        return RuleResult.denied(
            "You must purchase at least " + minTickets + " tickets.");
    }

    @Override
    public String describe() {
        return "Minimum tickets: " + minTickets;
    }

    public int getMinTickets() { return minTickets; }
}