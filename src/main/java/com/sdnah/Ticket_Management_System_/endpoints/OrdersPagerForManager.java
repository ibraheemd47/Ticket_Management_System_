package com.sdnah.Ticket_Management_System_.endpoints;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;

@RestController
@RequestMapping("/api/OrdersPagerForManager")
public class OrdersPagerForManager {
    private final ActiveOrderService activeOrderService;
 
    public OrdersPagerForManager(ActiveOrderService activeOrderService) {
        this.activeOrderService = activeOrderService;
    }
    // NOTE: companyId is NOT stored in the ORDER aggregate.
    //  * Since each eventId (UUID) uniquely identifies one event,
    //  * and each event belongs to exactly one company,
    //  * filtering by eventId is sufficient to get all orders for that company's event.

    @GetMapping("/GetAllOrdersByEventIdAndCompanyId")
    public ResponseEntity<List<PurchaseDTO>> GetAllOrdersByEventIdAndCompanyId(
            @RequestParam UUID eventId,
            @RequestParam Long companyId) {
        List<PurchaseDTO> purchases = activeOrderService.getPurchasesByEventId(eventId);
        return ResponseEntity.ok(purchases);
    }


    //NOT part of ORDER aggregate — belongs to EVENT/COMPANY aggregate.
    @GetMapping("/GetAllDetailsEventsByCompanyId")
    public ResponseEntity<String> GetAllDetailsEventsByCompanyId(Long companyId) {
        return ResponseEntity.ok("get all details events by company id endpoint!");
    }
}
