package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/OrdersPagerForManager")
public class OrdersPagerForManager {
    @GetMapping("/GetAllOrdersByEventIdAndCompanyId")
    public ResponseEntity<String> GetAllOrdersByEventIdAndCompanyId(Long eventId, Long companyId) {
        return ResponseEntity.ok("get all orders by event id and company id endpoint!");
    }
    @GetMapping("/GetAllDetailsEventsByCompanyId")
    public ResponseEntity<String> GetAllDetailsEventsByCompanyId(Long companyId) {
        return ResponseEntity.ok("get all details events by company id endpoint!");
    }
}
