package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/HomePage")
//

public class HomePage {
    @GetMapping("/SearchBar")
    public ResponseEntity<String> SearchBar() {
        return ResponseEntity.ok("search bar endpoint!");
    }
    @GetMapping("/GetAllUpComingEventsForHomePage")
    public ResponseEntity<String> GetAllUpComingEventsForHomePage() {
        return ResponseEntity.ok("showing all events endpoint!");
        //Json returns id event name event date and time and venue 
        //return array of events in json format
        //return ResponseEntity.ok(new EventDto(1L, "Concert A", "2024-12-01T20:00:00", "Stadium X"));

        //getAllUpComingEventsForHomePage() in company management service returns list of event ids for all upcoming events from active companies

    }
    //show only if guest not logged in
    @GetMapping("/LoginButton")
    public ResponseEntity<String> LoginButton() {
        return ResponseEntity.ok("login button endpoint!");
    }
    
    @GetMapping("/EventDetails")
    public ResponseEntity<String> EventDetails(Long eventId) {
        //hasEvent in event management service checks if event exists and is active
        return ResponseEntity.ok("event details endpoint!");
    }
    
    @GetMapping("/FilterEvents")
    public ResponseEntity<String> FilterEvents(double minPrice, double maxPrice,String dateRange, String location, String rating ,String companyName) {
        return ResponseEntity.ok("filter events endpoint!");
       //filterEventsByCompanyName in event management service returns list of event ids for events that match the company name

    }

    @GetMapping("/ShowOrders")
    public ResponseEntity<String> ShowOrders(Long userId ) {
        return ResponseEntity.ok("show orders endpoint!");
    }

    @GetMapping("/ShowCompaniesByRating")
    public ResponseEntity<String> ShowCompaniesByRating() {
        return ResponseEntity.ok("show companies by rating endpoint!");
        //showCompaniesByRating0 in company management service returns list of company ids sorted by rating
    }

    @GetMapping("/SearchByCompanyName")
    public ResponseEntity<String> SearchByCompanyName(String companyName) {
        return ResponseEntity.ok("search by company name endpoint!");
        //searchByCompanyName in company management service returns list of company ids that match the company name
    }
    @GetMapping("/SearchByEventName")
    public ResponseEntity<String> SearchByEventName(String eventName) {
        return ResponseEntity.ok("search by event name endpoint!");
    }
    @GetMapping("/SearchByCategory")
    public ResponseEntity<String> SearchByCategory(String category) {
        return ResponseEntity.ok("search by category endpoint!");
    }
    @GetMapping("/SearchBySingerName")
    public ResponseEntity<String> SearchBySingerName(String singerName) {
        return ResponseEntity.ok("search by singer name endpoint!");
    }
    //if logged in show username button if not show login button
    //if clicked shows username and dropdown with options to view profile, view settings, logout 
    //and if a manager in company show button to go to company profile
    @GetMapping("/UsernameButton")
    public ResponseEntity<String> UsernameButton() {
        return ResponseEntity.ok("username button endpoint!");
    }
}