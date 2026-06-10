package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;

public class CheckoutPresenter {

    private final ActiveOrderService orderService;

    public CheckoutPresenter(ActiveOrderService orderService) {
        this.orderService = orderService;
    }

    public PurchaseDTO checkout(UUID orderId, String token, String fullName, String cardNumber) {
        String cleanCard = cardNumber.replaceAll("\\s+", "");

        String last4 = cleanCard.length() >= 4
                ? cleanCard.substring(cleanCard.length() - 4)
                : cleanCard;

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                "CARD-" + last4,
                fullName.trim(),
                "CREDIT_CARD");

        return orderService.checkout(orderId, token, paymentDTO);
    }

    public OrderDTO applyCoupon(UUID orderId, String token, String couponCode) {
        return orderService.applyCoupon(orderId, token, couponCode.trim());
    }
}