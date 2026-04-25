package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

@Service
public class EventService {

    @Autowired
    private IEventRepository eventRepository;


    public Event createEvent(EventDto dto, Long companyId, Long ownerId) {
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);
        return eventRepository.save(event);
    }

    public void addShowToEvent(UUID eventId, show newShow, Long managerId) {
        Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new RuntimeException("Event not found"));
        
        event.addShow(newShow, managerId); 
        
       
    }

    public void assignManager(UUID eventId, Long newManagerId, Long currentOwnerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
            
        event.addManager(newManagerId, currentOwnerId);
    }
}