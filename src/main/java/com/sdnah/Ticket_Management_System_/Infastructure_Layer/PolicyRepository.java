package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import org.springframework.stereotype.Repository;

import java.util.Collection;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Repository
public class PolicyRepository implements IPolicyRepo {
    private final ConcurrentHashMap<Integer, Policy> policies = new ConcurrentHashMap<>();

    @Override
    public void save(Policy policy) {
        policies.put(policy.getPolicyId(), policy);
    }

    @Override
    public Optional<Policy> findById(int id) {
        return Optional.ofNullable(policies.get(id));
    }

    @Override
    public Collection<Policy> findAll() {
        return policies.values();
    }

    @Override
    public void deleteById(int id) {
        policies.remove(id);
    }
}