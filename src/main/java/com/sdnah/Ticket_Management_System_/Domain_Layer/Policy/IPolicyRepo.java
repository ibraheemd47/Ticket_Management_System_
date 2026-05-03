package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPolicyRepo {
    Optional<Policy> findByPolicyId(int policyId);

    List<Policy> findByEventId(UUID eventId);

    DiscountPolicy findDiscountPolicyByEventId(UUID eventId);

    PurchasePolicy findPurchasePolicyByEventId(UUID eventId);
    
    SellingPolicy findSellingPolicyByEventId(UUID eventId);

    void deleteByPolicyId(int policyId);

    Policy save(Policy policy);

    Optional<Policy> findAll();

}