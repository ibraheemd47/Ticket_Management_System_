package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/OrderPageForMember")
public class OrderPageForMember {
    @GetMapping("/GetAllUpcomingOrdersByMemberId")
    public ResponseEntity<String> GetAllOrdersByMemberId(Long memberId) {
        return ResponseEntity.ok("get all orders by member id endpoint!");
    }
    @GetMapping("/GetAllPassedOrdersByMemberId")
    public ResponseEntity<String> GetAllPassedOrdersByMemberId(Long memberId) {
        return ResponseEntity.ok("get all passed orders by member id endpoint!");
    }
    @GetMapping("/GetOrderDetailsByOrderId")
    public ResponseEntity<String> GetOrderDetailsByOrderId(Long orderId) {
        return ResponseEntity.ok("get order details by order id endpoint!");
    }
    @PostMapping("/CancelOrderByOrderId")
    public ResponseEntity<String> CancelOrderByOrderId(Long orderId) {
        return ResponseEntity.ok("cancel order by order id endpoint!");
    }
}
