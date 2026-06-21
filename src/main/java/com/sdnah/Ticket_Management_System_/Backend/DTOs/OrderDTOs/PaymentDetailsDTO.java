package com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs;

/**
 * Carrier of card-payment data from the Vaadin checkout form to
 * {@code ActiveOrderService.checkout}. The CVV / expiry are optional in
 * the DTO so older endpoints / tests aren't broken, but the WSEP gateway
 * uses them when present so failing-CVV cases (988 / 986 / etc.) actually
 * reach the external system.
 */
public class PaymentDetailsDTO {
    private final String cardToken;
    private final String billingName;
    private String paymentMethod;

    private final String cvv;
    private final Integer expiryMonth;
    private final Integer expiryYear;

    /** Legacy 3-arg constructor — no CVV / expiry. */
    public PaymentDetailsDTO(String cardToken,
            String billingName,
            String paymentMethod) {
        this(cardToken, billingName, paymentMethod, null, null, null);
    }

    /** Full constructor — carries CVV + expiry through to the WSEP gateway. */
    public PaymentDetailsDTO(String cardToken,
                             String billingName,
                             String paymentMethod,
                             String cvv,
                             Integer expiryMonth,
                             Integer expiryYear) {
        this.cardToken = cardToken;
        this.billingName = billingName;
        this.paymentMethod = paymentMethod;
        this.cvv = cvv;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
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

    public String getCvv() {
        return cvv;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }
}
