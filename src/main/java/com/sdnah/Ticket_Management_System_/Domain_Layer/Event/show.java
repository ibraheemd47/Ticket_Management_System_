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

    private Date showDate;

    public show(UUID EventId, Date showDate) {
      this.showid= UUID.randomUUID(); 
    this.EventId=EventId;
    this.showDate = showDate;

    }
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
        return showDate;
    }
    public void setShowDate(Date showDate) {
        this.showDate = showDate;
    }

}