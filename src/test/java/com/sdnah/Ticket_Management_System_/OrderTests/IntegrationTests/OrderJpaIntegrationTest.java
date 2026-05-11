package com.sdnah.Ticket_Management_System_.OrderTests.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;

@DataJpaTest
@ActiveProfiles("test")
class OrderJpaIntegrationTest {

    @Autowired
    private ActiveOrderRepository activeOrderRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @BeforeEach
    void cleanDb() {
        paymentTransactionRepository.deleteAll();
        purchaseRepository.deleteAll();
        activeOrderRepository.deleteAll();
    }

    @Test
    void saveAndFindById_shouldReturnSavedOrder() {
        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);

        activeOrderRepository.saveAndFlush(order);

        Optional<ActiveOrder> result = activeOrderRepository.findById(order.getId());

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void delete_shouldRemoveOrder() {
        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);

        activeOrderRepository.saveAndFlush(order);

        activeOrderRepository.delete(order);
        activeOrderRepository.flush();

        assertTrue(activeOrderRepository.findById(order.getId()).isEmpty());
    }

    @Test
    void findActiveOrder_shouldReturnMatchingActiveOrder() {
        String buyerId = "buyer-1";
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, 10);

        activeOrderRepository.saveAndFlush(order);

        Optional<ActiveOrder> result = activeOrderRepository.findActiveOrder(buyerId, eventId);

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void findPendingOrdersByBuyer_shouldReturnActiveOrders() {
        String buyerId = "buyer-1";

        ActiveOrder order = new ActiveOrder(buyerId, UUID.randomUUID(), 10);

        activeOrderRepository.saveAndFlush(order);

        List<ActiveOrder> result = activeOrderRepository.findPendingOrdersByBuyer(buyerId);

        assertEquals(1, result.size());
        assertEquals(order.getId(), result.get(0).getId());
    }

    @Test
    void findExpiredOrders_shouldReturnExpiredOrders() {
        ActiveOrder expired = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);

        ReflectionTestUtils.setField(
                expired,
                "expiresAt",
                LocalDateTime.now().minusMinutes(5));

        ActiveOrder active = new ActiveOrder("buyer-2", UUID.randomUUID(), 10);

        activeOrderRepository.save(expired);
        activeOrderRepository.save(active);
        activeOrderRepository.flush();

        List<ActiveOrder> result = activeOrderRepository.findExpiredOrders();

        assertTrue(result.stream().anyMatch(o -> o.getId().equals(expired.getId())));
        assertFalse(result.stream().anyMatch(o -> o.getId().equals(active.getId())));
    }

    @Test
    void savePurchaseAndFindByBuyer_shouldReturnBuyerHistory() {
        ActiveOrder order = new ActiveOrder("buyer-1", UUID.randomUUID(), 10);
        order.addTicket(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(50),
                null);

        activeOrderRepository.saveAndFlush(order);

        Purchase purchase = new Purchase(order);

        purchaseRepository.saveAndFlush(purchase);

        List<Purchase> result = purchaseRepository.findByBuyerId("buyer-1");

        assertEquals(1, result.size());
        assertEquals(purchase.getPurchaseId(), result.get(0).getPurchaseId());
    }

    @Test
    void savePurchaseAndFindByEventId_shouldReturnEventPurchases() {
        UUID eventId = UUID.randomUUID();

        ActiveOrder order = new ActiveOrder("buyer-1", eventId, 10);
        order.addTicket(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(50),
                null);

        activeOrderRepository.saveAndFlush(order);

        Purchase purchase = new Purchase(order);

        purchaseRepository.saveAndFlush(purchase);

        List<Purchase> result = purchaseRepository.findByEventId(eventId);

        assertEquals(1, result.size());
        assertEquals(purchase.getPurchaseId(), result.get(0).getPurchaseId());
    }

    @Test
    void saveTransaction_shouldPersistTransaction() {
        PaymentTransaction tx = new PaymentTransaction(
                "tx-" + UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                PaymentTransaction.Status.SUCCESS);

        paymentTransactionRepository.saveAndFlush(tx);

        Optional<PaymentTransaction> result = paymentTransactionRepository.findById(tx.getTransactionId());

        assertTrue(result.isPresent());
        assertEquals(tx.getTransactionId(), result.get().getTransactionId());
    }

    @Test
    void isTicketLocked_shouldReturnTrueWhenTicketExistsInActiveOrder() {
        UUID eventId = UUID.randomUUID();
        String ticketId = UUID.randomUUID().toString();

        ActiveOrder order = new ActiveOrder("buyer-1", eventId, 10);

        order.addTicket(
                ticketId,
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(50),
                new Lock(ticketId, "buyer-1", LocalDateTime.now().plusMinutes(10)));

        activeOrderRepository.saveAndFlush(order);

        assertTrue(activeOrderRepository.isTicketLocked(ticketId));
    }
}