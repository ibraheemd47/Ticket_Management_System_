package com.sdnah.Ticket_Management_System_.Backend.endpoints;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/AreaSelectionPage")
public class AreaSelectionPage {
    @GetMapping("/GetAreaMapbyEventId")
    public ResponseEntity<String> GetAreaMap() {
        return ResponseEntity.ok("get area map endpoint!");
    }
    
    
}
