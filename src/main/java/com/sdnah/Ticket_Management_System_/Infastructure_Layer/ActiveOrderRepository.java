package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;

import jakarta.persistence.LockModeType;

@Repository
public interface ActiveOrderRepository extends JpaRepository<ActiveOrder, UUID> {
    @Query("SELECT o FROM ActiveOrder o WHERE o.buyerId = :buyerId AND o.eventId = :eventId AND o.status = 'ACTIVE' AND o.expiresAt > CURRENT_TIMESTAMP")
    Optional<ActiveOrder> findActiveOrder(@Param("buyerId") String buyerId,
            @Param("eventId") UUID eventId);

    @Query("SELECT o FROM ActiveOrder o WHERE o.buyerId = :buyerId AND o.status = 'ACTIVE' AND o.expiresAt > CURRENT_TIMESTAMP")
    List<ActiveOrder> findPendingOrdersByBuyer(@Param("buyerId") String buyerId);

    @Query("SELECT o FROM ActiveOrder o WHERE o.status = 'ACTIVE' AND o.expiresAt < CURRENT_TIMESTAMP")
    List<ActiveOrder> findExpiredOrders();

    /** Check if a ticket is already locked in any active order */
    @Query("SELECT COUNT(i) > 0 FROM ActiveOrder o JOIN o.items i WHERE i.ticketId = :ticketId AND i.lock IS NOT NULL AND o.status = 'ACTIVE' AND o.expiresAt > CURRENT_TIMESTAMP")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean isTicketLocked(@Param("ticketId") String ticketId);

}
