package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

public class PaymentDetails {

    private final String cardToken;
    private final String billingName;

    public PaymentDetails(String cardToken, String billingName) {
        if (cardToken == null || cardToken.isBlank())
            throw new IllegalArgumentException("cardToken must not be empty");
        if (billingName == null || billingName.isBlank())
            throw new IllegalArgumentException("billingName must not be empty");
        this.cardToken   = cardToken;
        this.billingName = billingName;
    }
 
    public String getCardToken(){
        return cardToken; 
    }
    public String getBillingName() { 
        return billingName; 
    }
    
}
