package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;

class PurchaseTest {

    @Test
    void constructor_shouldCopyDataFromOrder() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);
        order.addItem(new OrderItem(1, 1, 1, new BigDecimal("50")));
        order.addItem(new OrderItem(2, 2, 1, new BigDecimal("30")));

        Purchase purchase = new Purchase(order);

        assertEquals(order.getId(), purchase.getOrderId());
        assertEquals(order.getBuyerId(), purchase.getBuyerId());
        assertEquals(2, purchase.getItems().size());
        assertEquals(order.getTotal(), purchase.getTotalPrice());
    }

    @Test
    void constructor_shouldThrow_whenOrderIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Purchase(null)
        );

        assertNotNull(ex);
    }

    @Test
    void purchase_shouldHaveTimestamp() {
        ActiveOrder order = new ActiveOrder("buyer1", 1, 10);

        Purchase purchase = new Purchase(order);

        assertNotNull(purchase.getPurchasedAt());
    }
}