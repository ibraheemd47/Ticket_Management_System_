package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.*;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer>, IPolicyRepo {

    Optional<Policy> findByPolicyId(int policyId);

    List<Policy> findByEventId(UUID eventId);

    @Query("SELECT p FROM DiscountPolicy p WHERE p.eventId = :eventId")
    DiscountPolicy findDiscountPolicyByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT p FROM PurchasePolicy p WHERE p.eventId = :eventId")
    PurchasePolicy findPurchasePolicyByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT p FROM SellingPolicy p WHERE p.eventId = :eventId")
    SellingPolicy findSellingPolicyByEventId(@Param("eventId") UUID eventId);

    void deleteByPolicyId(int policyId);
}