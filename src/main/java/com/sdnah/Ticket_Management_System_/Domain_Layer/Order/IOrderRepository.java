package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface IOrderRepository {
    void save(ActiveOrder order);
    Optional<ActiveOrder> findById(UUID orderId);
    Optional<ActiveOrder> findActiveOrder(String buyerId, UUID eventId);
    void delete(UUID orderId);
    void savePurchase(Purchase purchase);
    List<Purchase> findPurchasesByBuyer(String buyerId);
    void saveTransaction(PaymentTransaction tx);
    boolean acquireLock(Lock lock);
    void releaseLock(UUID resourceId);
    // Returns locks that expired AND have no matching active order
    // Used by ExpiredOrderService to clean up orphaned locks (crash recovery)
    List<Lock> findExpiredLocks();
    List<ActiveOrder> findExpiredOrders();
}
