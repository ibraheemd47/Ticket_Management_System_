package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import jakarta.persistence.Transient;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Event {

    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(Event.class);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    private String name;
    private Long companyId;
    private Long ownerId;

    @ElementCollection
    @CollectionTable(name = "event_manager_ids", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "manager_id")
    private List<Long> managerIds;

    private Date startDate;
    private Date endDate;
    private String venue;
    private String description;

    @ElementCollection
    @CollectionTable(name = "event_reviews", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "rating")
    private Map<UUID, Integer> usersReviews = new HashMap<>();

    @Enumerated(EnumType.STRING)
    private show_type eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<show> shows;

    protected Event() {}

    public Event(String name, show_type eventType, Long companyId, Long ownerId) {
        this.eventId = UUID.randomUUID();
        this.name = name;
        this.eventType = eventType;
        this.companyId = companyId;
        this.ownerId = ownerId;
        this.shows = new ArrayList<>();
        this.managerIds = new ArrayList<>();
        this.managerIds.add(ownerId);
    }

    public UUID getEventId() { return eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public List<Long> getManagerIds() { return managerIds; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public show_type getEventType() { return eventType; }
    public void setEventType(show_type eventType) { this.eventType = eventType; }

    public String getVenue() { return venue; }
    public String getDescription() { return description; }

    public List<show> getShows() { return shows; }
    public void setShows(List<show> shows, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can update the shows list.");
        logger.info("Updating shows list for event {} by manager {}", this.eventId, managerId);
        this.shows = shows;
    }

    public void addShow(show show, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can add shows to the event.");
        logger.info("Adding show to event {} by manager {}", this.eventId, managerId);
        shows.add(show);
        show.setEvent(this);
    }

    public void removeShow(show showToRemove, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can remove shows from the event.");
        logger.info("Removing show from event {} by manager {}", this.eventId, managerId);
        shows.removeIf(s -> Objects.equals(s.getShowid(), showToRemove.getShowid()));
    }

    public void addManager(Long managerId, Long requestingOwnerId) {
        if (!requestingOwnerId.equals(this.ownerId))
            throw new IllegalArgumentException("Only the owner can add managers to the event.");
        if (managerIds.contains(managerId))
            throw new IllegalArgumentException("This manager is already assigned to the event.");
        logger.info("Adding manager to event {} by owner {}", this.eventId, requestingOwnerId);
        managerIds.add(managerId);
    }

    public void removeManager(Long managerId, Long requestingOwnerId) {
        if (!requestingOwnerId.equals(this.ownerId))
            throw new IllegalArgumentException("Only the owner can remove managers from the event.");
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("This manager is not assigned to the event.");
        logger.info("Removing manager from event {} by owner {}", this.eventId, requestingOwnerId);
        managerIds.remove(managerId);
    }

    public void delete(Long requestingOwnerId) {
        if (!requestingOwnerId.equals(this.ownerId))
            throw new IllegalArgumentException("Only the owner can delete the event.");
        logger.info("Deleting event {} by owner {}", this.eventId, requestingOwnerId);
        this.shows.clear();
        this.managerIds.clear();
    }

    public void transferOwnership(Long newOwnerId, Long currentOwnerId) {
        if (!currentOwnerId.equals(this.ownerId))
            throw new IllegalArgumentException("Only the current owner can transfer ownership.");
        if (!managerIds.contains(newOwnerId))
            managerIds.add(newOwnerId);
        managerIds.remove(this.ownerId);
        this.ownerId = newOwnerId;
        logger.info("Ownership of event {} transferred from {} to {}", this.eventId, currentOwnerId, newOwnerId);
    }

    public void editName(String newName, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event name.");
        logger.info("Editing name of event {} by manager {}", this.eventId, managerId);
        this.name = newName;
    }

    public void editType(show_type newType, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event type.");
        logger.info("Editing type of event {} by manager {}", this.eventId, managerId);
        this.eventType = newType;
    }

    public void editDates(Date newStartDate, Date newEndDate, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event dates.");
        logger.info("Editing dates of event {} by manager {}", this.eventId, managerId);
        this.startDate = newStartDate;
        this.endDate = newEndDate;
    }

    public void editVenue(String newVenue, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event venue.");
        logger.info("Editing venue of event {} by manager {}", this.eventId, managerId);
        this.venue = newVenue;
    }

    public void editDescription(String newDescription, Long managerId) {
        if (!managerIds.contains(managerId))
            throw new IllegalArgumentException("Only managers can edit the event description.");
        logger.info("Editing description of event {} by manager {}", this.eventId, managerId);
        this.description = newDescription;
    }

    public void addReview(UUID userId, int rating) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        logger.info("Adding review for event {} by user {}", this.eventId, userId);
        usersReviews.put(userId, rating);
    }

    public Map<UUID, Integer> getReviews() { return usersReviews; }

    public List<show> getShowsForThisWeek() {
        logger.info("Retrieving shows for event {} for the current week", this.eventId);
        List<show> showsForThisWeek = new ArrayList<>();
        Date now = new Date();
        for (show show : shows) {
            if (isSameWeek(show.getShowDate(), now))
                showsForThisWeek.add(show);
        }
        return showsForThisWeek;
    }

    private boolean isSameWeek(Date showDate, Date now) {
        long millisInWeek = 7 * 24 * 60 * 60 * 1000L;
        return (showDate.getTime() / millisInWeek) == (now.getTime() / millisInWeek);
    }
}
