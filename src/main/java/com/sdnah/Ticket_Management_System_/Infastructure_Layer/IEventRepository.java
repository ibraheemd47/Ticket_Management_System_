package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {

    // ── Derived queries (Spring Data auto-implements these) ──────────────────
    List<Event> findByCompanyId(Long companyId);
    List<Event> findByOwnerId(Long ownerId);

    // ── Custom queries (require @Query implementation) ───────────────────────
    List<Event> findAllEvents();
    List<Event> findByManagerId(Long managerId);
    List<show> getShowsForEvent(UUID eventId);
    show getShowDetails(UUID eventId, UUID showId);
    boolean editShowInEvent(UUID eventId, UUID showId, String name, String description, String singer, Date showDate, Long managerId);
    boolean addAreaToShow(UUID eventId, UUID showId, String areaName, int capacity, double price, Long managerId);
    boolean removeAreaFromShow(UUID eventId, UUID showId, String areaName);
    String getEventDetails(UUID eventId);
    Map<UUID, Integer> getEventReviews(UUID eventId);
    boolean addReviewToEvent(UUID eventId, UUID userId, int rating);
    List<Event> getEventsByFilter(String name, show_type eventType, Date startDate, Date endDate);
    List<Event> searchEventsByName(String name);
    List<Event> searchEventsByType(show_type eventType);
    List<Event> searchEventsBySingerName(String singerName);
    boolean transferOwnership(UUID eventId, Long newOwnerId, Long currentOwnerId);
    boolean bookSeat(UUID eventId, UUID showId, String areaName, int seatNumber, Long userId);
}
