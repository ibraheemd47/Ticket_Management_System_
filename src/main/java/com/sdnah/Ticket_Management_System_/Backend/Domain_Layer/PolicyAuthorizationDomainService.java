package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;

public class PolicyAuthorizationDomainService {

    private void requireActiveMember(Member actor) {
        if (actor == null || !actor.isActive()) {
            throw new RuntimeException("Member must be active to perform this action");
        }
    }

    private void requireCompany(Company company) {
        if (company == null) {
            throw new RuntimeException("Company is required");
        }
    }

    private void requirePolicy(Policy policy) {
        if (policy == null) {
            throw new RuntimeException("Policy is required");
        }
    }

    private void assertPolicyBelongsToCompany(Policy policy, UUID companyId) {
        requirePolicy(policy);

        if (companyId == null) {
            throw new RuntimeException("Company id is required");
        }

        if (!companyId.equals(policy.getCompanyId())) {
            throw new RuntimeException(
                    "Policy does not belong to company " + companyId);
        }
    }

    public void assertCanModifyDiscountPolicy(Member actor, Company company, Policy policy) {
        requireActiveMember(actor);
        requireCompany(company);
        assertPolicyBelongsToCompany(policy, company.getCompanyId());

        if (company.isOwner(actor.getMemberId())) {
            return;
        }

        throw new RuntimeException(
                "Only company owners or authorized managers can modify discount policies");
    }

    public void assertCanModifyPurchasePolicy(Member actor, Company company, Policy policy) {
        requireActiveMember(actor);
        requireCompany(company);
        assertPolicyBelongsToCompany(policy, company.getCompanyId());

        if (company.isOwner(actor.getMemberId())) {
            return;
        }

        throw new RuntimeException(
                "Only company owners or authorized managers can modify purchase policies");
    }
}