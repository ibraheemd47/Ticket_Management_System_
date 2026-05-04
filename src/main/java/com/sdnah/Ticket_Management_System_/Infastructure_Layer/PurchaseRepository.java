package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    List<Purchase> findByBuyerId(String buyerId);

    List<Purchase> findByEventId(UUID eventId);
}