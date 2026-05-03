package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;

@Repository
public class OrderRepositoryImpl implements IOrderRepository {
    private final ConcurrentHashMap<UUID, ActiveOrder> orders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Purchase> purchases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PaymentTransaction> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public void save(ActiveOrder order) {
        ActiveOrder existing = orders.get(order.getId());
        if (existing != null && existing.getVersion() != order.getVersion()) {
            throw new RuntimeException("Version conflict for order: " + order.getId());
        }
        order.setVersion(order.getVersion() + 1);
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<ActiveOrder> findById(UUID orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public Optional<ActiveOrder> findActiveOrder(String buyerId, UUID eventId) {
        for (ActiveOrder order : orders.values()) {
            if (order.getBuyerId().equals(buyerId) && order.getEventId().equals(eventId)
                    && !order.isExpired()) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    @Override
    public void delete(UUID orderId) {
        orders.remove(orderId);
    }

    @Override
    public void savePurchase(Purchase purchase) {
        purchases.put(purchase.getPurchaseId(), purchase);
    }

    @Override
    public List<Purchase> findPurchasesByBuyer(String buyerId) {
        List<Purchase> result = new ArrayList<>();

        for (Purchase purchase : purchases.values()) {
            if (purchase.getBuyerId().equals(buyerId)) {
                result.add(purchase);
            }
        }

        return result;
    }

    @Override
    public void saveTransaction(PaymentTransaction tx) {
        transactions.put(tx.getTransactionId(), tx);
    }

    @Override
    public boolean acquireLock(Lock lock) {
        return locks.putIfAbsent(lock.getResourceId(), lock) == null;
    }

    @Override
    public void releaseLock(String resourceId) {
        locks.remove(resourceId);
    }

    @Override
    public List<Lock> findExpiredLocks() {
        Set<String> activeTicketIds = new HashSet<>();

        for (ActiveOrder order : orders.values()) {
            for (OrderItem item : order.getItems()) {
                activeTicketIds.add(item.getLockResourceId());
            }
        }
        List<Lock> result = new ArrayList<>();

        for (Lock lock : locks.values()) {
            if (lock.isExpired() || !activeTicketIds.contains(lock.getResourceId())) {
                result.add(lock);
            }
        }
        return result;
    }

    @Override
    public List<ActiveOrder> findExpiredOrders() {
        List<ActiveOrder> result = new ArrayList<>();

        for (ActiveOrder order : orders.values()) {
            if (order.isExpired()) {
                result.add(order);
            }
        }

        return result;
    }

    @Override
    public List<Purchase> findPurchasesByEventId(UUID eventId) {
        List<Purchase> result = new ArrayList<>();
        for (Purchase p : purchases.values()) {
            if (p.getEventId().equals(eventId))
                result.add(p);
        }
        return result;
    }

    /**
     * Returns all ACTIVE (not yet purchased, not expired, not cancelled) orders for
     * a given buyer.
     */
    @Override
    public List<ActiveOrder> findPendingOrdersByBuyer(String buyerId) {
        List<ActiveOrder> result = new ArrayList<>();
        for (ActiveOrder order : orders.values()) {
            if (order.getBuyerId().equals(buyerId)
                    && order.getStatus() == ActiveOrder.Status.ACTIVE
                    && !order.isExpired()) {
                result.add(order);
            }
        }
        return result;
    }

}
