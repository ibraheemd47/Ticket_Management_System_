package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
public class show {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifically tells JPA to use a UUID
    private UUID showid;

    // Links back to the parent Event
    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    // A show has 1 to many Areas
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "show_id") 
    private List<Area> areas;

    private UUID EventId;

    public show(UUID EventId) {
      this.showid= UUID.randomUUID();  // TODO: Initialize the show with the given event ID
    this.EventId=EventId;
    

    }
    // Create Tickets for each seat in the show 
    // public void createTickets() {
    //     for (Area area : areas) {
    //         if (area instanceof SeatedArea) {
    //             SeatedArea seatedArea = (SeatedArea) area;
    //             for (Block block : seatedArea.getBlocks()) {
    //                 for (Row row : block.getRows()) {
    //                     for (Seat seat : row.getSeats()) {
    //                         // Create a ticket for this seat
    //                         ticket ticket = new ticket(Block,row, seat);
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }

    public UUID getShowid() {
        return showid;
    }
    public Event getEvent() {
        return event;
    }
    public void setEvent(Event event) {
        this.event = event;
    }
    public List<Area> getAreas() {
        return areas;
    }
    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }
    public UUID getEventId() {
        return EventId;
    }
    public void setEventId(UUID eventId) {
        EventId = eventId;
    }

    public Date getShowDate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getShowDate'");
    }
    
}