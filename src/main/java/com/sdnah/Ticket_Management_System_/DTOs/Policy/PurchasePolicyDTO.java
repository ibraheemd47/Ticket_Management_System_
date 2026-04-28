package com.sdnah.Ticket_Management_System_.DTOs.Policy;

public class PurchasePolicyDTO extends PolicyDTO {

    private int minTickets;
    private int maxTickets;
    private int minAge;
    private boolean allowSingleSeatGap;

    public PurchasePolicyDTO() {
        setType("PURCHASE");
    }

    public PurchasePolicyDTO(
            int policyId,
            String description,
            int eventId,
            int minTickets,
            int maxTickets,
            int minAge,
            boolean allowSingleSeatGap) {

        super(policyId, description, eventId, "PURCHASE");
        this.minTickets = minTickets;
        this.maxTickets = maxTickets;
        this.minAge = minAge;
        this.allowSingleSeatGap = allowSingleSeatGap;
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

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public boolean isAllowSingleSeatGap() {
        return allowSingleSeatGap;
    }

    public void setAllowSingleSeatGap(boolean allowSingleSeatGap) {
        this.allowSingleSeatGap = allowSingleSeatGap;
    }
}