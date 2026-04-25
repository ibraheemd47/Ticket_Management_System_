package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
//

@RestController
@RequestMapping("/api/ManagingCompanyPage")
public class ManagingCompanyPage {
    @GetMapping("/GetCompanyLogoURLByCompanyId")
    public ResponseEntity<String> GetCompanyLogoURLByCompanyId(Long companyId) {
        return ResponseEntity.ok("get company logo URL by company id endpoint!");
        //getCompanyLogoURLByCompanyId(companyId) from company management service and return the URL in the response
    }

    @GetMapping("/GetCompanyDetailsByCompanyId")
    public ResponseEntity<String> GetCompanyDetailsByCompanyId(Long companyId) {
        return ResponseEntity.ok("get company details by company id endpoint!");
        //getCompanyDetails by company id from company management service and return the details in the response
    }

    @GetMapping("/GetAllEventsByCompanyId")
    public ResponseEntity<String> GetAllEventsByCompanyId(Long companyId) {
        return ResponseEntity.ok("get all events by company id endpoint!");
        //getAllEventsByCompany id from company management service 
    }

    //shows event details including the remaining tickets for the event 
    @GetMapping("/GetAllUpcomingEventsByCompanyId")
    public ResponseEntity<String> GetAllUpcomingEventsByCompanyId(Long companyId) {
        return ResponseEntity.ok("get all upcoming events by company id endpoint!");
        //getAllEventsByCompany id from company management service and events service should handle date filtering and return only the upcoming events in the response

    }

    //transfer Ownership is viewd only for the owner of the company
    @PostMapping("/TransferOwnershipByCompanyId")
    public ResponseEntity<String> TransferOwnershipByCompanyId(Long companyId) {
        return ResponseEntity.ok("transfer ownership by company id endpoint!");
        //appointAdditionalOwner /removeOwnerAppointment/resignOwnership from company management service
    }

    //show only if owner
    @PostMapping("/DeleteCompanyByCompanyId")
    public ResponseEntity<String> DeleteCompanyByCompanyId(Long companyId) {
        return ResponseEntity.ok("delete company by company id endpoint!");
        //deleteCompany from company management service
    }

    @PostMapping("/EditEventDetailsByEventIdButton")
    public ResponseEntity<String> EditEventDetailsByEventId(Long eventId) {
        return ResponseEntity.ok("edit event details by event id endpoint!");
    }
}
