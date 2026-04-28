package com.sdnah.Ticket_Management_System_.DTOs.Policy;

public class SellingPolicyDTO extends PolicyDTO {

    private String sellingType; // "REGULAR" or "LOTTERY"

    public SellingPolicyDTO() {
        setType("SELLING");
    }

    public SellingPolicyDTO(
            int policyId,
            String description,
            int eventId,
            String sellingType) {

        super(policyId, description, eventId, "SELLING");
        this.sellingType = sellingType;
    }

    public String getSellingType() {
        return sellingType;
    }

    public void setSellingType(String sellingType) {
        this.sellingType = sellingType;
    }
}