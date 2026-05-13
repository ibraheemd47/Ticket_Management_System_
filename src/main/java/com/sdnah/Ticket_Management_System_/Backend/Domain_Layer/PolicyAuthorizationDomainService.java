package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;

public class PolicyAuthorizationDomainService {

    // ── Shared guard ──────────────────────────────────────────────────────────

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

    // ── Policy belongs to company ─────────────────────────────────────────────

    /**
     * Verifies that the policy belongs to the given company.
     * Prevents an owner of company A from modifying policies of company B.
     */
    private void assertPolicyBelongsToCompany(Policy policy, int companyId) {
        requirePolicy(policy);
        if (policy.getCompanyId() != companyId) {
            throw new RuntimeException(
                "Policy does not belong to company " + companyId);
        }
    }

    // ── UC II.4.3 — Change Discount Policy ───────────────────────────────────

    /**
     * Asserts that the actor can modify the discount policy of an event.
     * Allowed: Owner OR Manager with MANAGE_POLICIES permission.
     */
    public void assertCanModifyDiscountPolicy(Member actor, Company company, Policy policy) {
        requireActiveMember(actor);
        requireCompany(company);
        assertPolicyBelongsToCompany(policy, company.getCompanyId());

        if (company.isOwner(actor.getMemberId())) {
            return; // owners always allowed
        }

        throw new RuntimeException(
            "Only company owners or authorized managers can modify discount policies");
    }

    // ── UC II.4.3 — Change Purchase Policy ───────────────────────────────────

    /**
     * Asserts that the actor can modify the purchase policy of an event.
     * Same rules as discount policy.
     */
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