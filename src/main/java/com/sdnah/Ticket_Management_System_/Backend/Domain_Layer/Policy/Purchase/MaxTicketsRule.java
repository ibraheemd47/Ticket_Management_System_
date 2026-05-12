package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MAX_TICKETS")
public class MaxTicketsRule extends PurchaseRule {

    @Column(name = "max_tickets")
    private int maxTickets;

    protected MaxTicketsRule() {}

    public MaxTicketsRule(int maxTickets) {
        if (maxTickets <= 0) throw new IllegalArgumentException("maxTickets must be positive");
        this.maxTickets = maxTickets;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getTicketQuantity() <= maxTickets) return RuleResult.allowed();
        return RuleResult.denied(
            "You may purchase at most " + maxTickets + " tickets per order.");
    }

    @Override
    public String describe() {
        return "Maximum tickets: " + maxTickets;
    }

    public int getMaxTickets() { return maxTickets; }
}