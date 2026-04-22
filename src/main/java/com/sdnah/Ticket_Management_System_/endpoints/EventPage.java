package com.sdnah.Ticket_Management_System_.endpoints;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;

@RestController
@RequestMapping("/api/EventPage")
public class EventPage {


    @GetMapping("/events")
    public ResponseEntity<List<EventDto>> getEvents() {
        // Mocking the data - in a real app, this comes from your Service/Repo
        List<EventDto> events = List.of(
            new EventDto(1L, "Summer Music Festival", "2026-07-15 19:00", "Central Park"),
            new EventDto(2L, "Tech Innovators Summit", "2026-08-10 09:00", "Convention Center"),
            new EventDto(3L, "Local Art Walk", "2026-09-05 10:00", "Downtown Gallery")
        );

        return ResponseEntity.ok(events);
    }



    
}
