package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy.SellingType;

public class SellingPolicyDTO extends PolicyDTO {
    private SellingType type;

    public SellingPolicyDTO() {}

    public SellingPolicyDTO(int policyId, String description, Integer eventId, int companyId, SellingType type) {
        super(policyId, description, eventId, companyId);
        this.type = type;
    }

    // Getters and Setters
    public SellingType getType() { return type; }
    public void setType(SellingType type) { this.type = type; }
}