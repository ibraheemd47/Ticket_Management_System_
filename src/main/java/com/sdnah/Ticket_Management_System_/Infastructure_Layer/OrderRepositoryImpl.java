package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import java.util.Optional;

public class OrderRepositoryImpl implements IOrderRepository {
    private final ConcurrentHashMap<UUID, ActiveOrder> orders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Purchase> purchases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PaymentTransaction> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public void save(ActiveOrder order) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public Optional<ActiveOrder> findById(UUID orderId) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public Optional<ActiveOrder> findActiveOrder(String buyerId, UUID eventId) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public void delete(UUID orderId) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public void savePurchase(Purchase purchase) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public List<Purchase> findPurchasesByBuyer(String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public void saveTransaction(PaymentTransaction tx) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public boolean acquireLock(Lock lock) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public void releaseLock(String resourceId) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public List<Lock> findExpiredLocks() {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    @Override
    public List<ActiveOrder> findExpiredOrders() {
        throw new UnsupportedOperationException("Not implemented yet");   
    }




    
}
