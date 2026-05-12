package com.sdnah.Ticket_Management_System_.Backend.endpoints;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;

@RestController
@RequestMapping("/api/CheckoutPage")
public class CheckoutPage {
    private final ActiveOrderService activeOrderService;

    public CheckoutPage(ActiveOrderService activeOrderService) {
        this.activeOrderService = activeOrderService;
    }

    @GetMapping("/ShowPriceDetails")
    public ResponseEntity<OrderDTO> ShowOrderPriceDetails(@RequestParam UUID orderId,
            @RequestParam String buyerId) {

        try {
            OrderDTO order = activeOrderService.getOrderById(orderId, buyerId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ApplyDiscountCode")
    public ResponseEntity<OrderDTO> ApplyDiscountCode(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestParam String discountCode) {

        try {
            OrderDTO order = activeOrderService.applyCoupon(orderId, buyerId, discountCode);
            return ResponseEntity.ok(order);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // Version 1: all payment methods go to same checkout (proxy payment)
    // A proxy payment service is used to simulate payment processing.
    // Real integrations will be added in future versions.

    @PostMapping("/PaywithCreditCardButton")
    public ResponseEntity<PurchaseDTO> payWithCreditCard(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        paymentDetails.setPaymentMethod("CREDIT_CARD");
        return handleCheckout(orderId, buyerId, paymentDetails);
    }

    @PostMapping("/PaywithPaypalButton")
    public ResponseEntity<PurchaseDTO> payWithPaypal(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        paymentDetails.setPaymentMethod("PAYPAL");
        return handleCheckout(orderId, buyerId, paymentDetails);
    }

    @PostMapping("/PaywithApplePayButton")
    public ResponseEntity<PurchaseDTO> payWithApplePay(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        paymentDetails.setPaymentMethod("APPLE_PAY");
        return handleCheckout(orderId, buyerId, paymentDetails);
    }

    @PostMapping("/PaywithGooglePayButton")
    public ResponseEntity<PurchaseDTO> payWithGooglePay(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        paymentDetails.setPaymentMethod("GOOGLE_PAY");
        return handleCheckout(orderId, buyerId, paymentDetails);
    }

    @PostMapping("/PaywithBitButton")
    public ResponseEntity<PurchaseDTO> payWithBit(
            @RequestParam UUID orderId,
            @RequestParam String buyerId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        paymentDetails.setPaymentMethod("BIT");
        return handleCheckout(orderId, buyerId, paymentDetails);
    }

    private ResponseEntity<PurchaseDTO> handleCheckout(
            UUID orderId,
            String buyerId,
            PaymentDetailsDTO paymentDetails) {

        try {
            return ResponseEntity.ok(
                    activeOrderService.checkout(orderId, buyerId, paymentDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
