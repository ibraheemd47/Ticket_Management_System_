package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import java.util.HashMap;
import java.util.Map;
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
    
    @ElementCollection
    @CollectionTable(name = "event_manager_ids", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "manager_id")
    private List<Long> ManagerIds;

    private Date StartDate;
    private Date EndDate;
    private String venue;
    private String description;

    @ElementCollection
    @CollectionTable(name = "event_reviews", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "rating")
    private Map<UUID, Integer> usersReviews = new HashMap<>();


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
    public List<show> getShowsForThisWeek() {
        logger.info("Retrieving shows for event {} for the current week", this.EventId);
        List<show> showsForThisWeek = new ArrayList<>();
        Date now = new Date();
        for (show show : shows) {
            if (isSameWeek(show.getShowDate(), now)) {
                showsForThisWeek.add(show);
            }
        }
        return showsForThisWeek;
    }
    
    private boolean isSameWeek(Date showDate, Date now) {
        // Simple implementation to check if two dates are in the same week
        // This can be improved with a more robust date handling library like Joda-Time or java.time
        long millisInWeek = 7 * 24 * 60 * 60 * 1000L;
        long showTime = showDate.getTime();
        long nowTime = now.getTime();
        return (showTime / millisInWeek) == (nowTime / millisInWeek);
    }
    public String getVenue() {
        return venue;
    }

    public String getDescription() {
        return description;
    }

    public void editName(String newName, Long managerId) {
        if (!ManagerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event name.");
        logger.info("Editing name of event {} by manager {}", this.EventId, managerId);
        this.name = newName;
    }

    public void editType(show_type newType, Long managerId) {
        if (!ManagerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event type.");
        logger.info("Editing type of event {} by manager {}", this.EventId, managerId);
        this.eventType = newType;
    }

    public void editDates(Date newStartDate, Date newEndDate, Long managerId) {
        if (!ManagerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event dates.");
        logger.info("Editing dates of event {} by manager {}", this.EventId, managerId);
        this.StartDate = newStartDate;
        this.EndDate = newEndDate;
    }

    public void editVenue(String newVenue, Long managerId) {
        if (!ManagerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event venue.");
        logger.info("Editing venue of event {} by manager {}", this.EventId, managerId);
        this.venue = newVenue;
    }

    public void editDescription(String newDescription, Long managerId) {
        if (!ManagerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event description.");
        logger.info("Editing description of event {} by manager {}", this.EventId, managerId);
        this.description = newDescription;
    }

    public void addReview(UUID userId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        logger.info("Adding review for event {} by user {}", this.EventId, userId);
        usersReviews.put(userId, rating);
    }
    public Map<UUID, Integer> getReviews() {
        return usersReviews;
    }


}