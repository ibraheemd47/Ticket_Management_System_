package com.sdnah.Ticket_Management_System_.OrderTests.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderRepositoryImpl;

class OrderRepositoryTest {

    @Test
    void saveAndFindById_shouldReturnSavedOrder() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);
        repository.save(order);

        Optional<ActiveOrder> result = repository.findById(order.getId());

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void delete_shouldRemoveOrder() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);
        repository.save(order);

        repository.delete(order.getId());

        assertTrue(repository.findById(order.getId()).isEmpty());
    }

    @Test
    void findActiveOrder_shouldReturnMatchingActiveOrder() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        String buyerId = "buyer-1";
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);
        repository.save(order);

        Optional<ActiveOrder> result = repository.findActiveOrder(buyerId, eventId);

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void savePurchaseAndFindPurchasesByBuyer_shouldReturnBuyerHistory() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);
        Purchase purchase = new Purchase(order);

        repository.savePurchase(purchase);

        List<Purchase> result = repository.findPurchasesByBuyer("buyer-1");

        assertEquals(1, result.size());
        assertEquals(purchase.getPurchaseId(), result.get(0).getPurchaseId());
    }

    @Test
    void saveTransaction_shouldPersistTransaction() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        PaymentTransaction tx = new PaymentTransaction(
                "tx-1",
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                PaymentTransaction.Status.SUCCESS
        );

        assertDoesNotThrow(() -> repository.saveTransaction(tx));
    }

    @Test
    void acquireLock_shouldSucceedWhenResourceIsFree() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        Lock lock = new Lock("ticket-1", "buyer-1", LocalDateTime.now().plusMinutes(10));

        assertTrue(repository.acquireLock(lock));
    }

    @Test
    void acquireLock_shouldFailWhenResourceAlreadyLocked() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        Lock first = new Lock("ticket-1", "buyer-1", LocalDateTime.now().plusMinutes(10));
        Lock second = new Lock("ticket-1", "buyer-2", LocalDateTime.now().plusMinutes(10));

        assertTrue(repository.acquireLock(first));
        assertFalse(repository.acquireLock(second));
    }

    @Test
    void releaseLock_shouldRemoveExistingLock() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        String ticketId = "ticket-1";

        Lock lock = new Lock(ticketId, "buyer-1", LocalDateTime.now().plusMinutes(10));
        repository.acquireLock(lock);

        repository.releaseLock(ticketId);

        Lock newLock = new Lock(ticketId, "buyer-2", LocalDateTime.now().plusMinutes(10));

        assertTrue(repository.acquireLock(newLock));
    }

    @Test
    void findExpiredOrders_shouldReturnExpiredOrders() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        ActiveOrder expired = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);
        expired.markExpired();

        ActiveOrder active = new ActiveOrder("buyer-2", UUID.randomUUID(), 10);

        repository.save(expired);
        repository.save(active);

        List<ActiveOrder> result = repository.findExpiredOrders();

        assertTrue(result.stream().anyMatch(o -> o.getId().equals(expired.getId())));
        assertFalse(result.stream().anyMatch(o -> o.getId().equals(active.getId())));
    }

    @Test
    void findExpiredLocks_shouldReturnExpiredOrOrphanLocks() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();

        Lock expiredLock = new Lock(
                "ticket-expired",
                "buyer-1",
                LocalDateTime.now().minusMinutes(1)
        );

        Lock orphanValidLock = new Lock(
                "ticket-orphan",
                "buyer-2",
                LocalDateTime.now().plusMinutes(10)
        );

        repository.acquireLock(expiredLock);
        repository.acquireLock(orphanValidLock);

        List<Lock> result = repository.findExpiredLocks();

        assertTrue(result.stream()
                .anyMatch(lock -> lock.getResourceId().equals("ticket-expired")));

        assertTrue(result.stream()
                .anyMatch(lock -> lock.getResourceId().equals("ticket-orphan")));
    }
}