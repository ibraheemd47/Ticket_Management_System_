package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;

public class Checkout_Domain_Service {

    public double validateCheckoutAndCalculateFinalPrice(
            ActiveOrder order,
            PurchasePolicy purchasePolicy,
            DiscountPolicy discountPolicy,
            int userAge,
            String couponCode) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }

        if (order.getStatus() != ActiveOrder.Status.ACTIVE) {
            throw new IllegalStateException("Only active order can be checked out");
        }

        if (order.isExpired()) {
            throw new IllegalStateException("Order expired");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout empty order");
        }

        int quantity = order.getItems().size();

        if (purchasePolicy != null &&
                !purchasePolicy.validatePurchase(quantity, false)) {
            throw new IllegalStateException("Checkout rejected by purchase policy");
        }

        double basePrice = order.getTotal().doubleValue();

        if (discountPolicy == null) {
            return basePrice;
        }

        return discountPolicy.calculateFinalPrice(basePrice, quantity, couponCode);
    }
}