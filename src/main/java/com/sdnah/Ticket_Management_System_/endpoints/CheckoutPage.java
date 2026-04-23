package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/CheckoutPage")
public class CheckoutPage {
    @GetMapping("/ShowPriceDetailsByTicketId")
    public ResponseEntity<String> ShowPriceDetailsByTicketId(Long ticketId) {
        return ResponseEntity.ok("show price details by ticket ID endpoint!");
    }
    @PostMapping("/ApplyDiscountCode")
    public ResponseEntity<String> ApplyDiscountCode(String discountCode) {
        return ResponseEntity.ok("apply discount code endpoint!");
    }
    @PostMapping("/PaywithCreditCardButton")
    public ResponseEntity<String> PaywithCreditCardButton() {
        return ResponseEntity.ok("pay with credit card button endpoint!");
    }
    @PostMapping("/PaywithPaypalButton")
    public ResponseEntity<String> PaywithPaypalButton() {
        return ResponseEntity.ok("pay with paypal button endpoint!");
    }
    @PostMapping("/PaywithApplePayButton")
    public ResponseEntity<String> PaywithApplePayButton() {
        return ResponseEntity.ok("pay with apple pay button endpoint!");
    }
    @PostMapping("/PaywithGooglePayButton")
    public ResponseEntity<String> PaywithGooglePayButton() {
        return ResponseEntity.ok("pay with google pay button endpoint!");
    }
    @PostMapping("/PaywithBitButton")
    public ResponseEntity<String> PaywithBitButton() {
        return ResponseEntity.ok("pay with bit button endpoint!");
    }
    
}
