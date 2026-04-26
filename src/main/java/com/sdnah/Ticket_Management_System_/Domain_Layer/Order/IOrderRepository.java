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

    void releaseLock(String resourceId);

    // Returns locks that expired AND have no matching active order
    // Used by ExpiredOrderService to clean up orphaned locks
    List<Lock> findExpiredLocks();

    List<ActiveOrder> findExpiredOrders();

    List<Purchase> findPurchasesByEventId(UUID eventId);

    List<ActiveOrder> findPendingOrdersByBuyer(String buyerId);
}
