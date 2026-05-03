package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy.SellingType;

public class SellingPolicyDTO extends PolicyDTO {
    private SellingType sellingType;

    public SellingPolicyDTO() {}

    public SellingPolicyDTO(int policyId, String description, Integer eventId, SellingType type) {
        super(policyId, description, eventId);
        this.sellingType = type;
    }

    // Getters and Setters
    public SellingType getType() { return sellingType; }
    public void setType(SellingType type) { this.sellingType = type; }
}