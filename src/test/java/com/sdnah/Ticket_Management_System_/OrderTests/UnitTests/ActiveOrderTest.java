package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;

class ActiveOrderTest {

    @Test
    void createOrder_shouldBeActive() {
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        assertEquals(ActiveOrder.Status.ACTIVE, order.getStatus());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void addTicket_shouldIncreaseSize() {
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        order.addTicket("1", 1L, UUID.randomUUID(), new BigDecimal("50"), null);

        assertEquals(1, order.getItems().size());
    }

    @Test
    void getTotal_shouldSumPrices() {
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        order.addTicket("1", 1L, UUID.randomUUID(), new BigDecimal("50"), null);
        order.addTicket("2", 2L, UUID.randomUUID(), new BigDecimal("30"), null);

        assertEquals(new BigDecimal("80"), order.getTotal());
    }

    @Test
    void isOwnedBy_shouldReturnTrueForCorrectBuyer() {
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        assertTrue(order.isOwnedBy("buyer1"));
        assertFalse(order.isOwnedBy("buyer2"));
    }

    @Test
    void removeItem_shouldRemoveCorrectItem() {
        ActiveOrder order = new ActiveOrder("buyer1", UUID.randomUUID(), 10);

        OrderItem item = order.addTicket("1", 1L, UUID.randomUUID(), new BigDecimal("50"), null);

        order.removeItem(item.getItemId());

        assertEquals(0, order.getItems().size());
    }
}