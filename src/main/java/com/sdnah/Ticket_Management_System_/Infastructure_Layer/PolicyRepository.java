package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> , IPolicyRepo {

    // ── Basic queries ─────────────────────────────────────────────

    Optional<Policy> findByPolicyId(int policyId);

    List<Policy> findByCompanyId(int companyId);

    //List<Policy> findByEventId(UUID eventId);
    // 1. השאילתה המקורית שמחזירה את כל סוגי המדיניות של האירוע (רשימה)
    List<Policy> findByEventId(UUID eventId);
    // 2. שאילתה ספציפית ששולפת רק את מדיניות ההנחות
    // Spring Data JPA יבצע סינון אוטומטי לפי הטיפוס DiscountPolicy
    DiscountPolicy findDiscountPolicyByEventId(UUID eventId);
    PurchasePolicy findPurchasePolicyByEventId(UUID eventId);

    // ── Main query used in PolicyService ─────────────────────────

    List<Policy> findByCompanyIdAndEventId(int companyId, UUID eventId);


    // ── Delete ───────────────────────────────────────────────────

    void deleteByPolicyId(int policyId);
}