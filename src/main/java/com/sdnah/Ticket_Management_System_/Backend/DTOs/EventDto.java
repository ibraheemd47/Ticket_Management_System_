package com.sdnah.Ticket_Management_System_.Backend.DTOs;
import java.time.LocalDate;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;

public class EventDto {
    public UUID id;
    public String name;
    public String dateTime;
    public show_type eventType;
    public String venue;
    public String photoUrl;
    public LocalDate startDate;
    public LocalDate endDate;
    public EventDto() {}
    public EventDto(UUID id, String name, String dateTime, show_type eventType, String venue) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.eventType = eventType;
        this.venue = venue;
    }
    public EventDto(UUID id, String name, LocalDate startDate, LocalDate endDate, show_type eventType, String venue, String photoUrl) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventType = eventType;
        this.venue = venue;
        this.photoUrl = photoUrl;
    }

}
