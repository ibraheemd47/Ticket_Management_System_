package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public  class Event {

    private Logger logger = (Logger) LoggerFactory.getLogger(Event.class);
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifically tells JPA to use a UUID
    private UUID EventId;

    private String name;

    private Long CompanyId; // Assuming this is a string, adjust type if needed

    private Long OwnerId; // Assuming this is a string, adjust type if needed
    
    private List<Long> ManagerIds; // Assuming this is a list of strings, adjust type if needed
    private Date StartDate; // Assuming you want to track the start date of the event
    private Date EndDate; // Assuming you want to track the end date of the event
    
    // And you use it exactly the same way
    @Enumerated(EnumType.STRING)
    private show_type eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<show> shows;

    public Event(String name, show_type eventType, Long CompanyId, Long OwnerId) {
        this.EventId = UUID.randomUUID();
        this.name = name;
        this.eventType = eventType;
        this.CompanyId = CompanyId;
        this.OwnerId = OwnerId;
        this.shows = new ArrayList<>(); // Initialize the list to avoid NullPointerException
        this.ManagerIds = new ArrayList<>(); // Initialize the list to avoid NullPointerException
        this.ManagerIds.add(OwnerId); // Add the owner as a manager by default

    }

    public UUID getEventId() {
        return EventId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getCompanyId() {
        return CompanyId;
    }
    public void setCompanyId(Long companyId) {
        CompanyId = companyId;
    }
    public Long getOwnerId() {
        return OwnerId;
    }
    public void setOwnerId(Long ownerId) {
        OwnerId = ownerId;
    }
    public List<Long> getManagerIds() {
        return ManagerIds;
    }
    public Date getStartDate() {
        return StartDate;
    }
    public void setStartDate(Date startDate) {
        StartDate = startDate;
    }
    public Date getEndDate() {
        return EndDate;
    }
    public void setEndDate(Date endDate) {
        EndDate = endDate;
    }
    public show_type getEventType() {
        return eventType;
    }
    public void setEventType(show_type eventType) {
        this.eventType = eventType;
    }
    public List<show> getShows() {
        return shows;
    }
    public void setShows(List<show> shows, Long ManagerId) {
        if (!ManagerIds.contains(ManagerId)) {
            throw new IllegalArgumentException("Only managers can update the shows list.");
        }
        logger.info("Updating shows list for event {} by manager {}", this.EventId, ManagerId);
        this.shows = shows;
    }
    public void addShow(show show,Long ManagerId) {
        if (!ManagerIds.contains(ManagerId)) {
            throw new IllegalArgumentException("Only managers can add shows to the event.");
        }
        logger.info("Adding show to event {} by manager {}", this.EventId, ManagerId);
        shows.add(show);
        show.setEvent(this);
    }
    public void removeShow(show show, Long ManagerId) {
        if (!ManagerIds.contains(ManagerId)) {
            throw new IllegalArgumentException("Only managers can remove shows from the event.");
        }
        logger.info("Removing show from event {} by manager {}", this.EventId, ManagerId);
        shows.remove(show);
        show.setEvent(null);
    }
    public void addManager(Long managerId, Long OwnerId) {
        if (!OwnerId.equals(this.OwnerId)) {
            throw new IllegalArgumentException("Only the owner can add managers to the event.");
        }
        if (ManagerIds.contains(managerId)) {
            throw new IllegalArgumentException("This manager is already assigned to the event.");
        }
        logger.info("Adding manager to event {} by owner {}", this.EventId, OwnerId);
        ManagerIds.add(managerId);
    }
    public void removeManager(Long managerId, Long OwnerId) {
        if (!OwnerId.equals(this.OwnerId)) {
            throw new IllegalArgumentException("Only the owner can remove managers from the event.");
        }
        if (!ManagerIds.contains(managerId)) {
            throw new IllegalArgumentException("This manager is not assigned to the event.");
        }
        logger.info("Removing manager from event {} by owner {}", this.EventId, OwnerId);
        ManagerIds.remove(managerId);
        
    }

    public void delete(Long ownerId2) {
        if (!ownerId2.equals(this.OwnerId)) {
            throw new IllegalArgumentException("Only the owner can delete the event.");
        }
        logger.info("Deleting event {} by owner {}", this.EventId, ownerId2);
        // Perform any necessary cleanup here, such as removing associated shows or tickets
        this.shows.clear(); // Clear the list of shows
        this.ManagerIds.clear(); // Clear the list of managers
    }
    


}