package com.sdnah.Ticket_Management_System_.Backend.endpoints;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/EventPage")
public class EventPage {
    @GetMapping("/GetEventDetailsByEventId")
    public ResponseEntity<String> GetEventDetailsByEventId(Long eventId) {
        return ResponseEntity.ok("get event details by event id endpoint!");
    }
    @GetMapping("/GetAllShowsByEventId")
    public ResponseEntity<String> GetAllShowsByEventId(Long eventId) {
        return ResponseEntity.ok("get all shows by event id endpoint!");
    }
    @GetMapping("/GetAllReviewsByEventId")
    public ResponseEntity<String> GetAllReviewsByEventId(Long eventId) {
        return ResponseEntity.ok("get all reviews by event id endpoint!");
    }
    //only if manager/owner/system admin is logged in
    @PostMapping("/editEventNameByEventId")
    public ResponseEntity<String> EditEventNameByEventId(Long eventId, String eventName) {
        return ResponseEntity.ok("edit event name by event id endpoint!");
    }
    @PostMapping("/editEventDescriptionByEventId")
    public ResponseEntity<String> EditEventDescriptionByEventId(Long eventId, String eventDescription) {
        return ResponseEntity.ok("edit event description by event id endpoint!");
    }
    @PostMapping("/editEventLocationByEventId")
    public ResponseEntity<String> EditEventLocationByEventId(Long eventId, String eventLocation) {
        return ResponseEntity.ok("edit event location by event id endpoint!");
    }
    @PostMapping("/editEventDateByEventId")
    public ResponseEntity<String> EditEventDateByEventId(Long eventId, String eventDate) {
        return ResponseEntity.ok("edit event date by event id endpoint!");
    }
    @PostMapping("/deleteEventByEventId")
    public ResponseEntity<String> DeleteEventByEventId(Long eventId) {
        return ResponseEntity.ok("delete event by event id endpoint!");
    }
    @PostMapping("/addShowToEventByEventId")
    public ResponseEntity<String> AddShowToEventByEventId(Long eventId, String showName, String singerName, String location, String showDescription, String showDate, Long ticketsQuantity, Double ticketPrice) {
        return ResponseEntity.ok("add show to event by event id endpoint!");
    }
    @PostMapping("/addDiscountPolicyToEventByEventId")
    public ResponseEntity<String> AddDiscountPolicyToEventByEventId(Long eventId, Double discountPercentage, String discountName) {
        return ResponseEntity.ok("add discount policy to event by event id endpoint!");
    }
    @PostMapping("/addSellingPolicyToEventByEventId")
    public ResponseEntity<String> AddSellingPolicyToEventByEventId(Long eventId, String sellingPolicyName, String sellingPolicyDescription) {
        return ResponseEntity.ok("add selling policy to event by event id endpoint!");
    }
    @PostMapping("/removeDiscountPolicyFromEventByEventId")
    public ResponseEntity<String> RemoveDiscountPolicyFromEventByEventId(Long eventId, Long discountPolicyId) {
        return ResponseEntity.ok("remove discount policy from event by event id endpoint!");
    }
    @PostMapping("/publishEventEditsByEventId")
    public ResponseEntity<String> PublishEventEditsByEventId(Long eventId) {
        return ResponseEntity.ok("publish event edits by event id endpoint!");
    }
}
