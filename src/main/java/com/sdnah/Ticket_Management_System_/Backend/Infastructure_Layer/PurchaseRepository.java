package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.UUID;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    List<Purchase> findByBuyerId(String buyerId);

    List<Purchase> findByEventId(UUID eventId);

    /** Count purchases completed after a given timestamp — avoids loading all rows. */
    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.purchasedAt >= :since")
    long countPurchasesSince(@Param("since") LocalDateTime since);
}