package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.vaadin.flow.component.notification.Notification;

public class CheckoutPresenter {

    private final ActiveOrderService orderService;

    public CheckoutPresenter(ActiveOrderService orderService) {
        this.orderService = orderService;
    }

    public PurchaseDTO checkout(UUID orderId, String token, String fullName, String cardNumber) {
        PurchaseDTO purchase = null;
        String cleanCard = cardNumber.replaceAll("\\s+", "");

        String last4 = cleanCard.length() >= 4
                ? cleanCard.substring(cleanCard.length() - 4)
                : cleanCard;

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "CARD-" + last4,
                fullName.trim(),
                "CREDIT_CARD");
        try {
            purchase = orderService.checkout(orderId, token, paymentDTO);
        } catch (Exception e) {
            Notification.show("Checkout failed: " + e.getMessage());
        }
        if (purchase != null) {
            Notification.show("Checkout successful! Order ID: " + purchase.getOrderId());
        } else {
            Notification.show("Checkout failed. Please check your payment details and try again.");
        }
        return purchase;
    }

    public OrderDTO applyCoupon(UUID orderId, String token, String couponCode) {
        OrderDTO updatedOrder = null;
        try {
             updatedOrder = orderService.applyCoupon(orderId, token, couponCode.trim());
        } catch (Exception e) {
            Notification.show("Failed to apply coupon: " + e.getMessage());
        }
        if (updatedOrder != null) {
            Notification.show("Coupon applied successfully!");
        }else {
            Notification.show("Invalid coupon code.");
        }
        return updatedOrder;
    }
}