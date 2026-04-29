package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

import ch.qos.logback.classic.Logger;

@Service
public class EventService {

    @Autowired
    private IEventRepository eventRepository;
    private final Logger logger = (Logger) LoggerFactory.getLogger(EventService.class);
    
    public EventService(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }


    public Event createEvent(EventDto dto, Long companyId, Long ownerId) {
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);
        return eventRepository.save(event);
    }

    public void addShowToEvent(UUID eventId, show newShow, Long managerId) {
        Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new RuntimeException("Event not found"));
        
        event.addShow(newShow, managerId); 
        
        logger.info("Show added to event: {}", event.getEventId()); 
    }

    public void assignManager(UUID eventId, Long newManagerId, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        logger.info("Assigning manager {} to event {}", newManagerId, eventId);
        event.addManager(newManagerId, currentOwnerId);
    }
    public void removeManager(UUID eventId, Long managerIdToRemove, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        logger.info("Removing manager {} from event {}", managerIdToRemove, eventId);
            
        event.removeManager(managerIdToRemove, currentOwnerId);
    }
    public void deleteEvent(UUID eventId, Long ownerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        logger.info("Deleting event {}", eventId);
        event.delete(ownerId);
        eventRepository.delete(event);
    }
    public void removeShowFromEvent(UUID eventId, show showId, Long managerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        logger.info("Removing show {} from event {}", showId.getShowid(), eventId);
            
        event.removeShow(showId, managerId);
    }
}