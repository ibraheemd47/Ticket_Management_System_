package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/HomePage")

public class HomePage {
    @GetMapping("/SearchBar")
    public ResponseEntity<String> SearchBar() {
        return ResponseEntity.ok("search bar endpoint!");
    }
    @GetMapping("/GetAllEventsForHomePage")
    public ResponseEntity<String> GetAllEventsForHomePage() {
        return ResponseEntity.ok("showing all events endpoint!");
        //Json returns id event name event date and time and venue 
        //return array of events in json format
        //return ResponseEntity.ok(new EventDto(1L, "Concert A", "2024-12-01T20:00:00", "Stadium X"));
    }
    @GetMapping("/LoginButton")
    public ResponseEntity<String> LoginButton() {
        return ResponseEntity.ok("login button endpoint!");
    }
    @GetMapping("/RegisterButton")
    public ResponseEntity<String> RegisterButton() {
        return ResponseEntity.ok("register button endpoint!");
    }
    @GetMapping("/EventDetails")
    public ResponseEntity<String> EventDetails() {
        return ResponseEntity.ok("event details endpoint!");
    }
    
}
