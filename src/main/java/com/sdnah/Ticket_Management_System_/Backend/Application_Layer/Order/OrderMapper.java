package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.util.List;
import java.util.ArrayList;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.*;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.*;

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
                order.getbuyerId(),
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

    public static List<OrderDTO> toDTOList(List<ActiveOrder> orders) {
        List<OrderDTO> result = new ArrayList<>();
        for (ActiveOrder o : orders)
            result.add(toDTO(o));
        return result;
    }

    public static List<PurchaseDTO> purchaseToDTOList(List<Purchase> purchases) {
        List<PurchaseDTO> result = new ArrayList<>();

        for (Purchase p : purchases) {
            List<String> ticketIds = p.getItems()
                    .stream()
                    .map(OrderItem::getTicketId)
                    .toList();

            result.add(toDTO(p, ticketIds));
        }

        return result;
    }
}
