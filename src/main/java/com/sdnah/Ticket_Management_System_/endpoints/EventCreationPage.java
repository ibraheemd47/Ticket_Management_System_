package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/EventCreationPage")
public class EventCreationPage {
    //Company:
    //authorizeOwnerOrManager(actingUserId, companyId, permission) - checks that the user is allowed to manage this company/event.  
    //validateEventBelongsToCompany(companyId, eventId) - makes sure the event being edited/updated is really owned by this company.
    //validateCompanyExists(companyId) - makes sure the company exists before doing company-related event work.
    //attachEventToCompany(companyId, eventId) - after EventService creates a new event company stores the new eventId in its event list.
    @PostMapping("/CreateEvent")
    public ResponseEntity<String> CreateEvent(String eventName, String eventDescription, String eventLocation, String eventDate, Long companyId) {
        return ResponseEntity.ok("create event endpoint!"); 
    }
    @PostMapping("/AddDiscountPolicyToEvent")
    public ResponseEntity<String> AddDiscountPolicyToEvent(Long eventId,String discountName, Double discountPercentage) {
        return ResponseEntity.ok("add discount policy to event endpoint!");
    }
    @PostMapping("/AddSellingPolicyToEvent")
    public ResponseEntity<String> AddSellingPolicyToEvent(Long eventId, String sellingPolicyName, String sellingPolicyDescription) {
        return ResponseEntity.ok("add selling policy to event endpoint!");
    }
    @PostMapping("/AddShowToEvent")
    public ResponseEntity<String> AddShowToEvent(Long eventId, String showName,String singerName,String Location ,String showDescription, String showDate, Long ticketsQuantity, Double ticketPrice) {
        return ResponseEntity.ok("add show to event endpoint!");
    }
    @GetMapping("/GetAllCreatedShowsByEventId")
    public ResponseEntity<String> GetAllCreatedShowsByEventId(Long eventId) {
        return ResponseEntity.ok("get all created shows by event ID endpoint!");
    }
    @PostMapping("/DeleteShowByShowId")
    public ResponseEntity<String> DeleteShowByShowId(Long showId) {
        return ResponseEntity.ok("delete show by show ID endpoint!");
    }
    @PostMapping("/editShowByShowId")
    public ResponseEntity<String> EditShowByShowId(Long showId, String showName, String singerName, String location, String showDescription, String showDate, Long ticketsQuantity, Double ticketPrice) {
        return ResponseEntity.ok("edit show by show ID endpoint!");
    }
    @PostMapping("/PublishEventButton")
    public ResponseEntity<String> PublishEventByEventId() {
        return ResponseEntity.ok("publish event endpoint!");
    }
    @PostMapping("/AddAreaToShow")
    public ResponseEntity<String> AddAreaToShow(Long showId, String areaName, Long areaCapacity) {
        return ResponseEntity.ok("add area to show endpoint!");
    }
}
