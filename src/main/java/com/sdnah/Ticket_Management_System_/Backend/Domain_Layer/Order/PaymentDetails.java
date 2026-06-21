package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

/**
 * Card-payment details we carry through the checkout saga.
 *
 * <p>The "core" fields ({@code cardToken}, {@code billingName},
 * {@code paymentMethod}) are required; the WSEP-specific fields
 * ({@code cvv}, {@code expiryMonth}, {@code expiryYear}) are optional —
 * older call sites and unit tests that don't care about WSEP can keep
 * using the 3-arg constructor.
 *
 * <p>When the WSEP integration is active, the real CVV from the checkout
 * form must reach the gateway so the WSEP server can decide whether to
 * approve or decline the transaction (e.g. it declines specific CVVs to
 * exercise the rollback path).
 */
public class PaymentDetails {

    private final String cardToken;
    private final String billingName;
    private final String paymentMethod;

    // Optional — when null, the WSEP gateway falls back to defaults.
    private final String cvv;
    private final Integer expiryMonth;
    private final Integer expiryYear;

    public PaymentDetails(String cardToken, String billingName, String paymentMethod) {
        this(cardToken, billingName, paymentMethod, null, null, null);
    }

    public PaymentDetails(String cardToken,
                          String billingName,
                          String paymentMethod,
                          String cvv,
                          Integer expiryMonth,
                          Integer expiryYear) {
        if (cardToken == null || cardToken.isBlank())
            throw new IllegalArgumentException("cardToken must not be empty");
        if (billingName == null || billingName.isBlank())
            throw new IllegalArgumentException("billingName must not be empty");
        if (paymentMethod == null || paymentMethod.isBlank())
            throw new IllegalArgumentException("paymentMethod must not be empty");

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

    /** @return CVV typed by the buyer at checkout, or {@code null} if unknown. */
    public String getCvv() {
        return cvv;
    }

    /** @return card expiry month (1–12), or {@code null} if unknown. */
    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    /** @return card expiry year (4-digit), or {@code null} if unknown. */
    public Integer getExpiryYear() {
        return expiryYear;
    }
}
