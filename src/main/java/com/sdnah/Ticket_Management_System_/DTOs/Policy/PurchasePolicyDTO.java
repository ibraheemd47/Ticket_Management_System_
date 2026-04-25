package com.sdnah.Ticket_Management_System_.DTOs.Policy;


public class PurchasePolicyDTO {
    private int eventId;
    private int minTickets;
    private int maxTickets;
    private int minAge;
    private boolean allowSingleSeatGap;

    // Getters and Setters
    public int getEventId() { return eventId; }
    public int getMinTickets() { return minTickets; }
    public int getMaxTickets() { return maxTickets; }
    public int getMinAge() { return minAge; }
    public boolean isAllowSingleSeatGap() { return allowSingleSeatGap; }
}
