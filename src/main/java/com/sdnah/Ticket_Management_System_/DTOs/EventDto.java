package com.sdnah.Ticket_Management_System_.DTOs;

public class EventDto {
    public Long id;
    public String name;
    public String dateTime;
    public String venue;
    public String photoUrl;
    public EventDto() {}
    public EventDto(Long id, String name, String dateTime, String venue) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.venue = venue;
    }

}
