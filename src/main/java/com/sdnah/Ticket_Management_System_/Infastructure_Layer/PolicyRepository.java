package com.sdnah.Ticket_Management_System_.Infastructure_Layer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountPolicy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer>  {

    // ── Basic queries ─────────────────────────────────────────────
    Optional<Policy> findByPolicyId(int policyId);
    List<Policy> findByEventId(UUID eventId);
    DiscountPolicy findDiscountPolicyByEventId(UUID eventId);
    PurchasePolicy findPurchasePolicyByEventId(UUID eventId);
    SellingPolicy findSellingPolicyByEventId(UUID eventId);


    Optional<DiscountPolicy> findDiscountPolicyByEventId2(UUID eventId);
    Optional<PurchasePolicy> findPurchasePolicyByEventId2(UUID eventId);
    Optional<SellingPolicy> findSellingPolicyByEventId2(UUID eventId);



    void deleteByPolicyId(int policyId);
}