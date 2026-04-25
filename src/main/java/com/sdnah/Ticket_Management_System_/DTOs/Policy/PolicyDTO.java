package com.sdnah.Ticket_Management_System_.DTOs.Policy;


public class PolicyDTO {
    private int policyId;
    private String description;
    private int eventId;
    private String type; // : "PURCHASE", "DISCOUNT", "SELLING"

    public PolicyDTO() {}

    public PolicyDTO(int policyId, String description, int eventId, String type) {
        this.policyId = policyId;
        this.description = description;
        this.eventId = eventId;
        this.type = type;
    }

    // Getters and Setters
    public int getPolicyId() { return policyId; }
    public String getDescription() { return description; }
    public int getEventId() { return eventId; }
    public String getType() { return type; }
}