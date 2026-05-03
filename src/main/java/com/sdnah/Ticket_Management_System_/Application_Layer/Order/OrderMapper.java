package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.*;
import java.util.List;
import java.util.ArrayList;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.*;

public class OrderMapper {

    public static OrderDTO toDTO(ActiveOrder order) {

        List<OrderItemDTO> items = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            items.add(new OrderItemDTO(
                    item.getItemId(),
                    item.getTicketId(),
                    item.getSeatId(),
                    item.getAreaId(),
                    item.getPrice()));
        }

        return new OrderDTO(
                order.getId(),
                order.getBuyerId(),
                order.getEventId(),
                items,
                order.getExpiresAt(),
                order.getStatus().name(),
                order.getTotal(), // originalPrice
                order.getDiscount(), // discount
                order.getFinalPrice(), // finalPrice
                order.getAppliedCouponCode() // coupon
        );
    }

    public static OrderItemDTO toDTO(OrderItem item) {

        return new OrderItemDTO(
                item.getItemId(),
                item.getTicketId(),
                item.getSeatId(),
                item.getAreaId(),
                item.getPrice());
    }

    public static PurchaseDTO toDTO(Purchase purchase,
            List<String> ticketCodes) {

        return new PurchaseDTO(
                purchase.getPurchaseId(),
                purchase.getOrderId(),
                ticketCodes,
                purchase.getTotalPrice(),
                purchase.getPurchasedAt());
    }
}
