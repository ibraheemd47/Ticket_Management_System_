package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscountPolicyDTO extends PolicyDTO {
    private List<DiscountRuleDTO> activeDiscounts;
    private boolean isAdditive;

    public DiscountPolicyDTO() {
        this.activeDiscounts = new ArrayList<>();
    }

    public DiscountPolicyDTO(int policyId, String description, UUID eventId,
                             List<DiscountRuleDTO> activeDiscounts, boolean isAdditive) {
        super(policyId, description, eventId);
        this.activeDiscounts = activeDiscounts != null ? activeDiscounts : new ArrayList<>();
        this.isAdditive = isAdditive;
    }

    // Getters and Setters
    public List<DiscountRuleDTO> getActiveDiscounts() { return activeDiscounts; }
    public void setActiveDiscounts(List<DiscountRuleDTO> activeDiscounts) { this.activeDiscounts = activeDiscounts; }

    public boolean isAdditive() { return isAdditive; }
    public void setAdditive(boolean additive) { isAdditive = additive; }
}