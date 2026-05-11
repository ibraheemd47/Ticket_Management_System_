package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderActionLog;

@Repository
public interface OrderActionLogRepository extends JpaRepository<OrderActionLog, Long> {

    /** Most recent action for the given order, or empty if the order has none. */
    Optional<OrderActionLog> findTopByOrderIdOrderByIdDesc(UUID orderId);
}