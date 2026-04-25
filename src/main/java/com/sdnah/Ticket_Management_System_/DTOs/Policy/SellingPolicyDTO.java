package com.sdnah.Ticket_Management_System_.DTOs.Policy;


public class SellingPolicyDTO {
    private int eventId;
    private String sellingType; // "REGULAR" או "LOTTERY"

    public SellingPolicyDTO() {}

    public SellingPolicyDTO(int eventId, String sellingType) {
        this.eventId = eventId;
        this.sellingType = sellingType;
    }

    // Getters and Setters
    public int getEventId() { return eventId; }
    public String getSellingType() { return sellingType; }
}