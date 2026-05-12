package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> {

       // ── Basic queries ──────────────────────────────────────────────────────────
    Optional<Policy> findByPolicyId(int policyId);
    List<Policy>     findByEventId(UUID eventId);
 
    // ── By eventId (event-scoped policy) ──────────────────────────────────────
    Optional<DiscountPolicy> findDiscountPolicyByEventId(UUID eventId);
    Optional<PurchasePolicy> findPurchasePolicyByEventId(UUID eventId);
    Optional<SellingPolicy>  findSellingPolicyByEventId(UUID eventId);
 
    // ── By companyId (company-scoped policy, eventId = null) ──────────────────
    Optional<DiscountPolicy> findDiscountPolicyByCompanyIdAndEventIdIsNull(int companyId);
    Optional<PurchasePolicy> findPurchasePolicyByCompanyIdAndEventIdIsNull(int companyId);
    Optional<SellingPolicy>  findSellingPolicyByCompanyIdAndEventIdIsNull(int companyId);
 
    // ── Delete ────────────────────────────────────────────────────────────────
    void deleteByPolicyId(int policyId);

    // ── Save any Policy subtype ───────────────────────────────────
    default <S extends Policy> S savePolicy(S policy) {
        return save(policy);
    }
}