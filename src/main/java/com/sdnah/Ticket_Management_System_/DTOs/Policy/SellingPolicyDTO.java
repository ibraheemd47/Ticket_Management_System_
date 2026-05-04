package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy.SellingType;

public class SellingPolicyDTO extends PolicyDTO {
    private SellingType sellingType;

    public SellingPolicyDTO() {}

    public SellingPolicyDTO(int policyId, String description, UUID eventId, SellingType type) {
        super(policyId, description, eventId);
        this.sellingType = type;
    }

    // Getters and Setters
    public SellingType getSellingType() { return sellingType; }
    public void setSellingType(SellingType sellingType) { this.sellingType = sellingType; }
}