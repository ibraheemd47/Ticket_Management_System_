package com.sdnah.Ticket_Management_System_.DTOs;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;

public class EventDto {
    public UUID id;
    public String name;
    public String dateTime;
    public show_type eventType;
    public String venue;
    public String photoUrl;
    public EventDto() {}
    public EventDto(UUID id, String name, String dateTime, show_type eventType, String venue) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.eventType = eventType;
        this.venue = venue;
    }

}
