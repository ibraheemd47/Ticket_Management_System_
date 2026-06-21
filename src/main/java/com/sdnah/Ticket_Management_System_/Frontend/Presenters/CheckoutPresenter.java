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

    /**
     * Submit the checkout against the order service. Returns the resulting
     * {@link PurchaseDTO} on success and rethrows the original exception on
     * failure so the view can surface a specific decline / network message to
     * the user.
     *
     * <p>Plumbing-wise this is the layer where the raw card data captured by
     * the Vaadin form turns into a backend DTO: the full PAN, the CVV the
     * user typed, and the {@code MM/YY} expiry split into month + year. The
     * CVV in particular has to reach WSEP unaltered so its "decline this
     * CVV" behaviour can fire (and trigger the saga's refund-side path).
     */
    public PurchaseDTO checkout(UUID orderId,
                                String token,
                                String fullName,
                                String cardNumber,
                                String cvv,
                                String expiryMmYy) {

        String cleanCard = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");
        Integer month = null;
        Integer year  = null;
        if (expiryMmYy != null && expiryMmYy.contains("/")) {
            String[] parts = expiryMmYy.split("/", 2);
            try {
                month = Integer.parseInt(parts[0].trim());
                int parsedYear = Integer.parseInt(parts[1].trim());
                year = parsedYear < 100 ? 2000 + parsedYear : parsedYear; // accept "26" or "2026"
            } catch (NumberFormatException ignored) {
                // leave both null → WSEP gateway uses its fallback defaults
            }
        }

        // Reject obviously-invalid or already-expired cards before we even
        // hit WSEP. WSEP doesn't always decline past-dated cards itself, so
        // without this guard the user could check out with e.g. 01/20.
        if (month != null && year != null) {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Card expiry month must be between 01 and 12.");
            }
            java.time.YearMonth cardExpiry = java.time.YearMonth.of(year, month);
            java.time.YearMonth thisMonth  = java.time.YearMonth.now();
            if (cardExpiry.isBefore(thisMonth)) {
                throw new IllegalArgumentException("Card has expired. Please use a valid card.");
            }
        }

        PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                cleanCard,                    // pass the full PAN — WSEP expects card_number
                fullName == null ? "" : fullName.trim(),
                "CREDIT_CARD",
                cvv == null ? null : cvv.trim(),
                month,
                year);

        // Rethrow on failure so the caller can show a specific decline message.
        return orderService.checkout(orderId, token, paymentDTO);
    }

    /** Backward-compatible overload used by callers that don't yet capture CVV. */
    public PurchaseDTO checkout(UUID orderId, String token, String fullName, String cardNumber) {
        return checkout(orderId, token, fullName, cardNumber, null, null);
    }

    public OrderDTO applyCoupon(UUID orderId, String token, String couponCode) {
        return orderService.applyCoupon(orderId, token, couponCode.trim());
    }
}
