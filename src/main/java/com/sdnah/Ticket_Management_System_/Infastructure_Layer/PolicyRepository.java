package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> {

    // ── Basic queries ─────────────────────────────────────────────

    Optional<Policy> findByPolicyId(int policyId);

    List<Policy> findByCompanyId(int companyId);

    List<Policy> findByEventId(Integer eventId);


    // ── Main query used in PolicyService ─────────────────────────

    List<Policy> findByCompanyIdAndEventId(int companyId, Integer eventId);


    // ── Delete ───────────────────────────────────────────────────

    void deleteByPolicyId(int policyId);
}