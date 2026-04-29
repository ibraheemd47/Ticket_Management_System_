package com.sdnah.Ticket_Management_System_.Infastructure_Layer;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
//import com.sdnah.Ticket_Management_System_.Domain_Layer.Seat;
@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {
    public Optional<Event> findById(UUID id);
    public void save(EventDto event);
    public void deleteById(UUID id);
    public List<Event> findAllEvents();
    public List<Event> findByCompanyId(Long companyId);
    public List<Event> findByManagerId(Long managerId);
    public List<Event> findByOwnerId(Long ownerId);
    public boolean createEvent(String name, show_type eventType, Long CompanyId, Long OwnerId);
    public void addShowToEvent(UUID EventId,String name , String description,String singer , Date showDate, Long managerId );
    public void assignManager(UUID EventId, Long newManagerId, Long currentOwnerId);
    public void removeManager(UUID EventId, Long managerIdToRemove, Long currentOwnerId);
    public List<show> getShowsForEvent(UUID EventId);
    public boolean deleteShowFromEvent(UUID EventId, UUID showId, Long managerId);
    public boolean editShowInEvent(UUID EventId, UUID showId, String name, String description, String singer, Date showDate, Long managerId);
    public boolean addAreaToShow(UUID EventId, UUID showId, String areaName, int capacity, double price, Long managerId);
    public boolean removeAreaFromShow(UUID EventId, UUID showId, String areaName);
    public String getEventDetails(UUID EventId);
    public Map<UUID, Integer> getEventReviews(UUID EventId);
    public boolean addReviewToEvent(UUID EventId, UUID userId, int rating);
    public boolean editEventName(UUID EventId, String newName, Long managerId);
    public boolean editEventType(UUID EventId, show_type newType, Long managerId);
    public boolean editEventDates(UUID EventId, Date newStartDate, Date newEndDate, Long managerId);
    public boolean editEventDescription(UUID EventId, String newDescription, Long managerId);
    public boolean editEventVenue(UUID EventId, String newVenue, Long managerId);
    public boolean editEventDateRange(UUID EventId, Date newStartDate, Date newEndDate, Long managerId);
    public List<Event> getEventsByFilter(String name, show_type eventType, Date startDate, Date endDate);
    public List<Event> searchEventsByName(String name);
    public List<Event> searchEventsByType(show_type eventType);
    public List<Event> searchEventsBySingerName(String singerName);
    public boolean transferOwnership(UUID EventId, Long newOwnerId, Long currentOwnerId);
    public show getShowDetails(UUID EventId, UUID showId);
    public boolean bookSeat(UUID EventId, UUID showId, String areaName, int seatNumber, Long userId);



}
