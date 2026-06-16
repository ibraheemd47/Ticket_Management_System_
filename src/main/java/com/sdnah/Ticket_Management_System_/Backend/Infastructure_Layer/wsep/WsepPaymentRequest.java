package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

/**
 * Form parameters for a WSEP {@code pay} request. Matches the spec fields
 * 1-for-1 so {@link WsepClient#pay} can map them directly into the POST body.
 *
 * @param amount      total amount as a string (e.g. "1000")
 * @param currency    ISO currency code (e.g. "USD")
 * @param cardNumber  PAN of the card being charged
 * @param month       expiry month, 1-12
 * @param year        4-digit expiry year
 * @param holder      cardholder name as printed on the card
 * @param cvv         3- or 4-digit security code
 * @param holderId    national/customer id of the cardholder
 */
public record WsepPaymentRequest(
        String amount,
        String currency,
        String cardNumber,
        int    month,
        int    year,
        String holder,
        String cvv,
        String holderId) {
}
