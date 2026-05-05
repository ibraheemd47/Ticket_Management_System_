package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.OrderMapper;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderItemDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;

class OrderMapperTest {

    @Test
    void toDTO_shouldMapActiveOrderCorrectly() {
        UUID eventId = UUID.randomUUID();
        ActiveOrder order = new ActiveOrder("buyer1", eventId, 10);

        order.addTicket(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"),
                null
        );

        OrderDTO dto = OrderMapper.toDTO(order);

        assertEquals(order.getId(), dto.getOrderId());
        assertEquals(order.getBuyerId(), dto.getBuyerId());
        assertEquals(order.getEventId(), dto.getEventId());
        assertEquals(1, dto.getItems().size());
        assertEquals(order.getTotal(), dto.getOriginalPrice());
        assertEquals(order.getDiscount(), dto.getDiscount());
        assertEquals(order.getFinalPrice(), dto.getFinalPrice());
        assertEquals(order.getAppliedCouponCode(), dto.getAppliedCouponCode());
    }

    @Test
    void toDTO_shouldMapOrderItemCorrectly() {
        OrderItem item = new OrderItem(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50")
        );

        OrderItemDTO dto = OrderMapper.toDTO(item);

        assertEquals(item.getItemId(), dto.getItemId());
        assertEquals(item.getTicketId(), dto.getTicketId());
        assertEquals(item.getSeatId(), dto.getSeatId());
        assertEquals(item.getAreaId(), dto.getAreaId());
        assertEquals(item.getPrice(), dto.getPrice());
    }

    @Test
    void toDTO_shouldMapPurchaseCorrectly() {
        UUID eventId = UUID.randomUUID();
        ActiveOrder order = new ActiveOrder("buyer1", eventId, 10);

        order.addTicket(
                "ticket1",
                1L,
                UUID.randomUUID(),
                new BigDecimal("50"),
                null
        );

        Purchase purchase = new Purchase(order);

        List<String> ticketCodes = List.of("code1", "code2");

        PurchaseDTO dto = OrderMapper.toDTO(purchase, ticketCodes);

        assertEquals(purchase.getPurchaseId(), dto.getPurchaseId());
        assertEquals(purchase.getOrderId(), dto.getOrderId());
        assertEquals(ticketCodes, dto.getTicketCodes());
        assertEquals(purchase.getTotalPrice(), dto.getFinalPrice());
        assertEquals(purchase.getPurchasedAt(), dto.getPurchasedAt());
    }
}