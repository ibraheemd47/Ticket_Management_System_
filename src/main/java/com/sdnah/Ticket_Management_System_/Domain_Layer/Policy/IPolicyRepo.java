package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface for Policy repository in the Domain Layer.
 * Defines the necessary operations for managing policies like Purchase, Discount, and Selling.
 */
public interface IPolicyRepo {

    void save(Policy policy);
    Optional<Policy> findById(int policyId);
    Collection<Policy> findAll();
    void deleteById(int policyId);
}