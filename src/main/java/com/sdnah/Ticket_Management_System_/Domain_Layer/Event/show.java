package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class show {

    private Logger logger = (Logger) LoggerFactory.getLogger(show.class);
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
    private String name;
    private String description;
    private String singer;

    public show(UUID EventId,String name , String description,String singer , Date showDate) {
      this.showid= UUID.randomUUID(); 
    this.EventId=EventId;
    this.showDate = showDate;
    this.name = name;
    this.description = description;
    this.singer = singer;
    logger.info("Show created with ID: {} for event {}", this.showid, this.EventId);

    }
    public show() {
        //TODO Auto-generated constructor stub
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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getSinger() {
        return singer;
    }
    public void setSinger(String singer) {
        this.singer = singer;
    }
}