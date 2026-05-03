package com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs;

public class PaymentDetailsDTO {
    private final String cardToken;
    private final String billingName;
    private String paymentMethod;

    public PaymentDetailsDTO(String cardToken,
            String billingName,
            String paymentMethod) {

        this.cardToken = cardToken;
        this.billingName = billingName;
        this.paymentMethod = paymentMethod;
    }

    public String getCardToken() {
        return cardToken;
    }

    public String getBillingName() {
        return billingName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
