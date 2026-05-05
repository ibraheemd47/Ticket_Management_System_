// package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.atomic.AtomicInteger;

// import org.springframework.stereotype.Repository;

// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

// /**
//  * Temporary in-memory {@link IPolicyRepo} implementation. Provides enough of
//  * a working bean for User / Event / Ticket / Order Spring tests to load the
//  * application context. Once the Policy team annotates their domain classes
//  * as JPA entities, switch back to a Spring Data JPA-backed implementation
//  * and delete this file together with the stub {@link PolicyRepository}.
//  */
// @Repository
// public class InMemoryPolicyRepo implements IPolicyRepo {

    // private final ConcurrentHashMap<Integer, Policy> byId = new ConcurrentHashMap<>();
    // private final AtomicInteger sequence = new AtomicInteger(1);

    // @Override
    // public Optional<Policy> findByPolicyId(int policyId) {
    //     return Optional.ofNullable(byId.get(policyId));
    // }

    // @Override
    // public List<Policy> findByEventId(UUID eventId) {
    //     if (eventId == null) {
    //         return new ArrayList<>();
    //     }
    //     List<Policy> out = new ArrayList<>();
    //     for (Policy p : byId.values()) {
    //         if (eventId.equals(p.getEventId())) {
    //             out.add(p);
    //         }
    //     }
    //     return out;
    // }

    // @Override
    // public DiscountPolicy findDiscountPolicyByEventId(UUID eventId) {
    //     return findByEventIdAndType(eventId, DiscountPolicy.class);
    // }

    // @Override
    // public PurchasePolicy findPurchasePolicyByEventId(UUID eventId) {
    //     return findByEventIdAndType(eventId, PurchasePolicy.class);
    // }

    // @Override
    // public SellingPolicy findSellingPolicyByEventId(UUID eventId) {
    //     return findByEventIdAndType(eventId, SellingPolicy.class);
    // }

    // @Override
    // public void deleteByPolicyId(int policyId) {
    //     byId.remove(policyId);
    // }

    // @Override
    // public Policy save(Policy policy) {
    //     if (policy == null) {
    //         return null;
    //     }
    //     byId.put(policy.getPolicyId(), policy);
    //     return policy;
    // }

    // private <T extends Policy> T findByEventIdAndType(UUID eventId, Class<T> type) {
    //     if (eventId == null) {
    //         return null;
    //     }
    //     for (Policy p : byId.values()) {
    //         if (eventId.equals(p.getEventId()) && type.isInstance(p)) {
    //             return type.cast(p);
    //         }
    //     }
    //     return null;
    // }
// }
