package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PURCHASE")
public class PurchasePolicy extends Policy {

    @Column(name = "min_tickets")
    private int minTickets = 1;

    @Column(name = "max_tickets")
    private int maxTickets = Integer.MAX_VALUE;

    @Column(name = "allow_single_seat_gap")
    private boolean allowSingleSeatGap = true;

    @Column(name = "min_age")
    private int minAge = 0;

    protected PurchasePolicy() {
        // JPA
    }

    public PurchasePolicy(int policyId, String description, UUID eventId) {
        super(policyId, description, eventId);
    }

    public boolean validatePurchase(int quantity, boolean createsSingleGap) {
        if (quantity < minTickets || quantity > maxTickets) return false;
        if (!allowSingleSeatGap && createsSingleGap) return false;
        return true;
    }

    @Override
    public boolean isValid() {
        return minTickets <= maxTickets && minAge >= 0;
    }

    public int getMinTickets() {
        return minTickets;
    }

    public void setMinTickets(int minTickets) {
        this.minTickets = minTickets;
    }

    public int getMaxTickets() {
        return maxTickets;
    }

    public void setMaxTickets(int maxTickets) {
        this.maxTickets = maxTickets;
    }

    public boolean isAllowSingleSeatGap() {
        return allowSingleSeatGap;
    }

    public void setAllowSingleSeatGap(boolean allowSingleSeatGap) {
        this.allowSingleSeatGap = allowSingleSeatGap;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }
}