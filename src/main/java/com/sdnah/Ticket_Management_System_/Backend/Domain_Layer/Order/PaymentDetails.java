package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

public class PaymentDetails {

    private final String cardToken;
    private final String billingName;
    private final String paymentMethod;

    public PaymentDetails(String cardToken, String billingName , String paymentMethod ) {
        if (cardToken == null || cardToken.isBlank())
            throw new IllegalArgumentException("cardToken must not be empty");
        if (billingName == null || billingName.isBlank())
            throw new IllegalArgumentException("billingName must not be empty");
        if (paymentMethod == null || paymentMethod.isBlank())
            throw new IllegalArgumentException("paymentMethod must not be empty");

        this.cardToken = cardToken;
        this.billingName = billingName;
        this.paymentMethod=paymentMethod;
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

}
