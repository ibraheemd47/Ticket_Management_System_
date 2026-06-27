package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> {

    Optional<Policy> findByPolicyId(int policyId);

    // =========================================================
    // Safe "all policies" queries
    // Do NOT query Policy directly here because subclasses have rootRule.
    // =========================================================

    default List<Policy> findByEventId(UUID eventId) {
        List<Policy> policies = new ArrayList<>();

        findSellingPolicyByEventId(eventId).ifPresent(policies::add);
        findPurchasePolicyByEventId(eventId).ifPresent(policies::add);
        findDiscountPolicyByEventId(eventId).ifPresent(policies::add);

        return policies;
    }

    default List<Policy> findByCompanyIdAndEventIdIsNull(UUID companyId) {
        List<Policy> policies = new ArrayList<>();

        findSellingPolicyByCompanyIdAndEventIdIsNull(companyId).ifPresent(policies::add);
        findPurchasePolicyByCompanyIdAndEventIdIsNull(companyId).ifPresent(policies::add);
        findDiscountPolicyByCompanyIdAndEventIdIsNull(companyId).ifPresent(policies::add);

        return policies;
    }


    // =========================================================
    // Event policies — query each concrete subclass directly
    // =========================================================

    
    Optional<DiscountPolicy> findDiscountPolicyByEventId(@Param("eventId") UUID eventId);

    Optional<PurchasePolicy> findPurchasePolicyByEventId(@Param("eventId") UUID eventId);

   
    Optional<SellingPolicy> findSellingPolicyByEventId(@Param("eventId") UUID eventId);

//     // =========================================================
//     // Company policies — query each concrete subclass directly
//     // =========================================================

    Optional<DiscountPolicy> findDiscountPolicyByCompanyIdAndEventIdIsNull(
            @Param("companyId") UUID companyId);

    Optional<PurchasePolicy> findPurchasePolicyByCompanyIdAndEventIdIsNull(
            @Param("companyId") UUID companyId);

    Optional<SellingPolicy> findSellingPolicyByCompanyIdAndEventIdIsNull(
            @Param("companyId") UUID companyId);

    // @org.springframework.transaction.annotation.Transactional
    // @Modifying
    // void deleteByPolicyId(int policyId);
    @org.springframework.transaction.annotation.Transactional
    default void deleteByPolicyId(int policyId) {
        findById(policyId).ifPresent(this::delete);
    }

    default <S extends Policy> S savePolicy(S policy) {
        return saveAndFlush(policy);
    }
}