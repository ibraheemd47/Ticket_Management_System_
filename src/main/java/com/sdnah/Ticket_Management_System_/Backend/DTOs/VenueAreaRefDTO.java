package com.sdnah.Ticket_Management_System_.Backend.DTOs;

/**
 * Lightweight reference to one of an event's inventory areas, used to link a
 * venue-map element to a pricing/seating area (II.4.2).
 */
public class VenueAreaRefDTO {
    public String id;
    public String name;
    public String type; // SEATED | STANDING

    public VenueAreaRefDTO() {}

    public VenueAreaRefDTO(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
