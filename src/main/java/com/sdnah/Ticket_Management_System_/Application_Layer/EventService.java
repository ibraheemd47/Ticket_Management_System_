package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

import ch.qos.logback.classic.Logger;

@Service
@Transactional
public class EventService {

    @Autowired
    private IEventRepository eventRepository;
    private final Logger logger = (Logger) LoggerFactory.getLogger(EventService.class);

    public EventService(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ── Creation / Deletion ──────────────────────────────────────────────────

    public Event createEvent(EventDto dto, Long companyId, Long ownerId) {
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);
        return eventRepository.save(event);
    }

    public void deleteEvent(UUID eventId, Long ownerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Deleting event {}", eventId);
        event.delete(ownerId);
        eventRepository.delete(event);
    }

    // ── Shows ────────────────────────────────────────────────────────────────

    public void addShowToEvent(UUID eventId, show newShow, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.addShow(newShow, managerId);
        eventRepository.saveAndFlush(event);
        logger.info("Show added to event {}", eventId);
    }

    public void removeShowFromEvent(UUID eventId, show showToRemove, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Removing show {} from event {}", showToRemove.getShowid(), eventId);
        event.removeShow(showToRemove, managerId);
        if (showToRemove.getShowid() != null) {
            eventRepository.deleteShowById(showToRemove.getShowid());
        }
    }

    public List<show> getShowsForEvent(UUID eventId) {
        logger.info("Retrieving shows for event {}", eventId);
        return eventRepository.getShowsForEvent(eventId);
    }

    public show getShowDetails(UUID eventId, UUID showId) {
        logger.info("Retrieving show {} details for event {}", showId, eventId);
        return eventRepository.getShowDetails(eventId, showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
    }

    public boolean editShowInEvent(UUID eventId, UUID showId, String name, String description,
            String singer, Date showDate, Long managerId) {
        logger.info("Editing show {} in event {} by manager {}", showId, eventId, managerId);
        return eventRepository.editShowInEvent(eventId, showId, name, description, singer, showDate, managerId);
    }

    // ── Areas ────────────────────────────────────────────────────────────────

    public boolean addAreaToShow(UUID eventId, UUID showId, String areaName, int capacity,
            double price, Long managerId) {
        logger.info("Adding area {} to show {} in event {} by manager {}", areaName, showId, eventId, managerId);
        return eventRepository.addAreaToShow(eventId, showId, areaName, capacity, price, managerId);
    }

    public boolean removeAreaFromShow(UUID eventId, UUID showId, String areaName) {
        logger.info("Removing area {} from show {} in event {}", areaName, showId, eventId);
        return eventRepository.removeAreaFromShow(eventId, showId, areaName);
    }

    // ── Managers / Ownership ─────────────────────────────────────────────────

    public void assignManager(UUID eventId, Long newManagerId, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Assigning manager {} to event {}", newManagerId, eventId);
        event.addManager(newManagerId, currentOwnerId);
        eventRepository.save(event);
    }

    public void removeManager(UUID eventId, Long managerIdToRemove, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Removing manager {} from event {}", managerIdToRemove, eventId);
        event.removeManager(managerIdToRemove, currentOwnerId);
        eventRepository.save(event);
    }

    public void transferOwnership(UUID eventId, Long newOwnerId, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Transferring ownership of event {} from {} to {}", eventId, currentOwnerId, newOwnerId);
        event.transferOwnership(newOwnerId, currentOwnerId);
        eventRepository.save(event);
    }

    // ── Edit Event Fields ────────────────────────────────────────────────────

    public void editEventName(UUID eventId, String newName, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.editName(newName, managerId);
        eventRepository.save(event);
    }

    public void editEventType(UUID eventId, show_type newType, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.editType(newType, managerId);
        eventRepository.save(event);
    }

    public void editEventDates(UUID eventId, Date newStartDate, Date newEndDate, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.editDates(newStartDate, newEndDate, managerId);
        eventRepository.save(event);
    }

    public void editEventDescription(UUID eventId, String newDescription, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.editDescription(newDescription, managerId);
        eventRepository.save(event);
    }

    public void editEventVenue(UUID eventId, String newVenue, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.editVenue(newVenue, managerId);
        eventRepository.save(event);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    // public Event getEventDetails(UUID eventId) {
    // logger.info("Retrieving details for event {}", eventId);
    // return eventRepository.findById(eventId)
    // .orElseThrow(() -> new RuntimeException("Event not found"));
    // }
    public Event getEventDetails(UUID id) {
        Event e = eventRepository.findById(id).orElseThrow();
        Hibernate.initialize(e.getManagerIds());
        return e;
    }

    public List<Event> getAllEvents() {
        logger.info("Retrieving all events");
        return eventRepository.findAllEvents();
    }

    public List<Event> getEventsByCompany(Long companyId) {
        logger.info("Retrieving events for company {}", companyId);
        return eventRepository.findByCompanyId(companyId);
    }

    public List<Event> getEventsByManager(Long managerId) {
        logger.info("Retrieving events for manager {}", managerId);
        return eventRepository.findByManagerId(managerId);
    }

    public List<Event> getEventsByOwner(Long ownerId) {
        logger.info("Retrieving events for owner {}", ownerId);
        return eventRepository.findByOwnerId(ownerId);
    }

    // ── Search ───────────────────────────────────────────────────────────────

    public List<Event> searchEventsByName(String name) {
        logger.info("Searching events by name: {}", name);
        return eventRepository.searchEventsByName(name);
    }

    public List<Event> searchEventsByType(show_type eventType) {
        logger.info("Searching events by type: {}", eventType);
        return eventRepository.searchEventsByType(eventType);
    }

    public List<Event> searchEventsBySingerName(String singerName) {
        logger.info("Searching events by singer: {}", singerName);
        return eventRepository.searchEventsBySingerName(singerName);
    }

    public List<Event> getEventsByFilter(String name, show_type eventType, Date startDate, Date endDate) {
        logger.info("Filtering events — name: {}, type: {}, from: {}, to: {}", name, eventType, startDate, endDate);
        return eventRepository.getEventsByFilter(name, eventType, startDate, endDate);
    }

    // ── Reviews ──────────────────────────────────────────────────────────────

    public Map<UUID, Integer> getEventReviews(UUID eventId) {
        logger.info("Retrieving reviews for event {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"))
                .getReviews();
    }

    public void addReviewToEvent(UUID eventId, UUID userId, int rating) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Adding review to event {} by user {}", eventId, userId);
        event.addReview(userId, rating);
        eventRepository.save(event);
    }

    // ── Tickets / Seats ──────────────────────────────────────────────────────

    public boolean bookSeat(UUID eventId, UUID showId, String areaName, int seatNumber, Long userId) {
        logger.info("Booking seat {} in area {} for show {} in event {} by user {}",
                seatNumber, areaName, showId, eventId, userId);
        return eventRepository.bookSeat(eventId, showId, areaName, seatNumber, userId);
    }

    public List<UUID> getEventsByManager(UUID eventId) {
        logger.info("Retrieving manager IDs for event {}", eventId);
        return eventRepository.getManagerIdsForEvent(eventId);
    }

}
