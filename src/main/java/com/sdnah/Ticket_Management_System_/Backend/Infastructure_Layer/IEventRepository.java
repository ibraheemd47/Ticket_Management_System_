package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {

    // ── Derived queries (Spring Data auto-implements) ────────────────────────
    List<Event> findByCompanyId(UUID companyId);

    List<Event> findByOwnerId(String ownerId);

    List<Event> findByNameContainingIgnoreCase(String keyword);

    // ── Custom JPQL queries ──────────────────────────────────────────────────
    // @Query("SELECT e FROM Event e")
    // List<Event> findAllEvents();
    @Query("SELECT e FROM Event e WHERE NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> findAllEvents();
    

    @Query("SELECT e FROM Event e WHERE :managerId MEMBER OF e.managerIds")
    List<Event> findByManagerId(@Param("managerId") String managerId);

    // Boolean EventExists(@Param("eventId") UUID eventUuid);

    @Query("SELECT s FROM show s WHERE s.event.eventId = :eventId")
    List<show> getShowsForEvent(@Param("eventId") UUID eventId);

    @Query("SELECT s FROM show s WHERE s.event.eventId = :eventId AND s.showid = :showId")
    Optional<show> getShowDetails(@Param("eventId") UUID eventId, @Param("showId") UUID showId);

    // Every search below filters out events whose owning Company has been
    // closed (by founder OR by system admin — both set isOpen=false). This
    // mirrors the filter on findAllEvents() above so buyers never discover
    // events from inactive companies through search.

    @Query("SELECT e FROM Event e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))" +
            " AND NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> searchEventsByName(@Param("name") String name);

    @Query("SELECT e FROM Event e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :desc, '%'))" +
            " AND NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> searchEventsByDescription(@Param("desc") String desc);

    @Query("SELECT e FROM Event e WHERE LOWER(e.venue) LIKE LOWER(CONCAT('%', :venue, '%'))" +
            " AND NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> searchEventsByVenue(@Param("venue") String venue);

    @Query("SELECT e FROM Event e WHERE e.eventType = :eventType" +
            " AND NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> searchEventsByType(@Param("eventType") show_type eventType);

    @Query("SELECT DISTINCT e FROM Event e JOIN e.shows s WHERE LOWER(s.singer) LIKE LOWER(CONCAT('%', :singerName, '%'))" +
            " AND NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> searchEventsBySingerName(@Param("singerName") String singerName);

    @Query("SELECT e FROM Event e WHERE " +
            "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:eventType IS NULL OR e.eventType = :eventType) AND " +
            "(:startDate IS NULL OR e.startDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.endDate <= :endDate) AND " +
            "NOT EXISTS (SELECT c FROM Company c WHERE c.companyId = e.companyId AND c.isOpen = false)")
    List<Event> getEventsByFilter(@Param("name") String name,
            @Param("eventType") show_type eventType,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM show s WHERE s.showid = :showId")
    void deleteShowById(@Param("showId") UUID showId);

    // ── Complex mutations — not yet implemented as JPQL ──────────────────────
    default boolean editShowInEvent(UUID eventId, UUID showId, String name, String description,
            String singer, Date showDate, String managerId) {
        throw new UnsupportedOperationException("Not yet implemented — use service layer");
    }

    default boolean addAreaToShow(UUID eventId, UUID showId, String areaName,
            int capacity, double price, String managerId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    default boolean removeAreaFromShow(UUID eventId, UUID showId, String areaName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    default boolean bookSeat(UUID eventId, UUID showId, String areaName,
            int seatNumber, Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Query("SELECT m FROM Event e JOIN e.managerIds m WHERE e.eventId = :eventId")
    List<UUID> getManagerIdsForEvent(@Param("eventId") UUID eventId);

    @Query("SELECT a FROM Area a WHERE a.id = :areaId")
    Optional<Area> findAreaById(@Param("areaId") UUID areaId);

    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findSeatById(@Param("seatId") Long seatId);
}
