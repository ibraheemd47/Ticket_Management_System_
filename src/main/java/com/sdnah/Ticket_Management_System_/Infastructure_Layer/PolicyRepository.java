package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

@Repository
public class PolicyRepository implements IPolicyRepo {

    private final Map<Integer, Policy> store = new HashMap<>();

    @Override
    public void save(Policy policy) {
        store.put(policy.getPolicyId(), policy);
    }

    @Override
    public Optional<Policy> findById(int policyId) {
        return Optional.ofNullable(store.get(policyId));
    }

    @Override
    public Collection<Policy> findAll() {
        return store.values();
    }

    @Override
    public void deleteById(int policyId) {
        store.remove(policyId);
    }
}