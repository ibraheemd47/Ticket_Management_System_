package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;

@RestController
@RequestMapping("/api/OrderPageForMember")
public class OrderPageForMember {
    private final ActiveOrderService activeOrderService;

    public OrderPageForMember(ActiveOrderService activeOrderService) {
        this.activeOrderService = activeOrderService;
    }

    /**
     * Returns all ACTIVE (in-progress, not yet completed) orders for the member.
     * These are orders the member started but hasn't paid for yet.
     */
    @GetMapping("/GetAllUpcomingOrdersByMemberId")
    public ResponseEntity<List<OrderDTO>> GetAllOrdersByMemberId(
            @RequestParam String memberId) {
        return ResponseEntity.ok(activeOrderService.getPendingOrdersByBuyer(memberId));
    }

    @GetMapping("/GetAllPassedOrdersByMemberId")
    public ResponseEntity<List<PurchaseDTO>> GetAllPassedOrdersByMemberId(
            @RequestParam String memberId) {
        return ResponseEntity.ok(activeOrderService.getPurchaseHistory(memberId));
    }

    @GetMapping("/GetOrderDetailsByOrderId")
    public ResponseEntity<OrderDTO> GetOrderDetailsByOrderId(
            @RequestParam UUID orderId,
            @RequestParam String buyerId) {
        try {
            return ResponseEntity.ok(activeOrderService.getOrderById(orderId, buyerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/CancelOrderByOrderId")
    public ResponseEntity<Void> CancelOrderByOrderId(
            @RequestParam UUID orderId,
            @RequestParam String buyerId) {
        try {
            activeOrderService.cancelOrder(orderId, buyerId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
