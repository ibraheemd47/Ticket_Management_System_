package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import java.util.ArrayList;
import java.util.List;

public class DiscountPolicyDTO extends PolicyDTO {

    private boolean additive;
    private List<DiscountRuleDTO> rules;

    public DiscountPolicyDTO() {
        setType("DISCOUNT");
        this.rules = new ArrayList<>();
    }

    public DiscountPolicyDTO(
            int policyId,
            String description,
            int eventId,
            boolean additive,
            List<DiscountRuleDTO> rules) {

        super(policyId, description, eventId, "DISCOUNT");
        this.additive = additive;
        this.rules = rules;
    }

    public boolean isAdditive() {
        return additive;
    }

    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    public List<DiscountRuleDTO> getRules() {
        return rules;
    }

    public void setRules(List<DiscountRuleDTO> rules) {
        this.rules = rules;
    }
}