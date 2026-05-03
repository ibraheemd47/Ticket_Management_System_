package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.OrderRepositoryImpl;

class OrderRepositoryImplTest {

    @Test
    void saveAndFindById_shouldReturnSavedOrder() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        repo.save(order);

        assertTrue(repo.findById(order.getId()).isPresent());
        assertEquals(order.getId(), repo.findById(order.getId()).get().getId());
    }

    @Test
    void delete_shouldRemoveOrder() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        repo.save(order);
        repo.delete(order.getId());

        assertTrue(repo.findById(order.getId()).isEmpty());
    }

    @Test
    void findActiveOrder_shouldReturnOrderForBuyerAndEvent() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        UUID eventId = UUID.randomUUID();
        ActiveOrder order = new ActiveOrder("buyer1", eventId, 10);

        repo.save(order);

        assertTrue(repo.findActiveOrder("buyer1", eventId).isPresent());
    }

    @Test
    void acquireLock_shouldReturnFalse_whenLockAlreadyExists() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        Lock lock = new Lock("ticket1", "buyer1", LocalDateTime.now().plusMinutes(5));

        assertTrue(repo.acquireLock(lock));
        assertFalse(repo.acquireLock(lock));
    }

    @Test
    void releaseLock_shouldAllowLockAgain() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        Lock lock = new Lock("ticket1", "buyer1", LocalDateTime.now().plusMinutes(5));

        repo.acquireLock(lock);
        repo.releaseLock("ticket1");

        assertTrue(repo.acquireLock(lock));
    }

    @Test
    void savePurchase_shouldBeFoundByBuyer() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        UUID eventId = UUID.randomUUID();
        ActiveOrder order = new ActiveOrder("buyer1", eventId, 10);
        order.addTicket("ticket1", 1L, UUID.randomUUID(), new BigDecimal("50"), null);

        Purchase purchase = new Purchase(order);
        repo.savePurchase(purchase);

        assertEquals(1, repo.findPurchasesByBuyer("buyer1").size());
    }

    @Test
    void findPendingOrdersByBuyer_shouldReturnOnlyActiveOrders() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        repo.save(order);

        assertEquals(1, repo.findPendingOrdersByBuyer("buyer1").size());
        assertEquals(0, repo.findPendingOrdersByBuyer("buyer2").size());
    }
}