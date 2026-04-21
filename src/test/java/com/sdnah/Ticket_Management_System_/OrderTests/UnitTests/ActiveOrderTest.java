package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;

class ActiveOrderTest {

    @Test
    void createOrder_shouldBeActive() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);

        assertEquals(ActiveOrder.Status.ACTIVE, order.getStatus());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void addItem_shouldIncreaseSize() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);
        OrderItem item = new OrderItem(1, 1, 1, new BigDecimal("50"));

        order.addItem(item);

        assertEquals(1, order.getItems().size());
    }

    @Test
    void getTotal_shouldSumPrices() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);

        order.addItem(new OrderItem(1, 1, 1, new BigDecimal("50")));
        order.addItem(new OrderItem(2, 2, 1, new BigDecimal("30")));

        assertEquals(new BigDecimal("80"), order.getTotal());
    }

    @Test
    void isOwnedBy_shouldReturnTrueForCorrectBuyer() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);

        assertTrue(order.isOwnedBy("buyer1"));
        assertFalse(order.isOwnedBy("buyer2"));
    }

    @Test
    void removeItem_shouldRemoveCorrectItem() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);
        OrderItem item = new OrderItem(1, 1, 1, new BigDecimal("50"));

        order.addItem(item);
        order.removeItem(item.getItemId());

        assertEquals(0, order.getItems().size());
    }
}