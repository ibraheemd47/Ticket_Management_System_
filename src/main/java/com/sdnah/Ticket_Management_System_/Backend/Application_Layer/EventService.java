package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

import ch.qos.logback.classic.Logger;

import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;

@Service
public class EventService {

    private final IEventRepository eventRepository;
    private final KeyedLock keyedLock;
    private final TransactionTemplate transactionTemplate;
    private final TicketRepository ticketRepository;

    private final Logger logger = (Logger) LoggerFactory.getLogger(EventService.class);

    private static final String LOCK_NS_EVENT = "event";
    private static final String LOCK_NS_EVENT_MANAGER = "event:manager";
    private static final String LOCK_NS_EVENT_REVIEW = "event:review";
    private static final String LOCK_NS_EVENT_SEAT = "event:seat";

    private final NotificationService notificationService;
    private final PurchaseRepository purchaseRepository;

    public EventService(IEventRepository eventRepository,
            PurchaseRepository purchaseRepository,
            NotificationService notificationService,
            KeyedLock keyedLock,
            TransactionTemplate transactionTemplate,
            TicketRepository ticketRepository) {
                if (notificationService == null)
                throw new IllegalArgumentException("notificationService required");
                if (purchaseRepository == null)
                throw new IllegalArgumentException("purchaseRepository required");
                this.notificationService = notificationService;
                this.eventRepository = eventRepository;
                this.keyedLock = keyedLock;
                this.transactionTemplate = transactionTemplate;
                this.purchaseRepository = purchaseRepository;
                this.ticketRepository = ticketRepository;
    }

    // ── Creation / Deletion ──────────────────────────────────────────────────

    public Event createEvent(EventDto dto, UUID companyId, Long ownerId) {
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);
        return eventRepository.saveAndFlush(event);
    }

    /**
     * Permanently deletes an event and everything under it:
     * tickets → shows/areas → event.
     */
    public void deleteEvent(UUID eventId, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                // Delete all tickets for every show first (satisfies FK constraints)
                for (show s : event.getShows()) {
                    if (s.getShowid() != null) {
                        List<ticket> tickets = ticketRepository.findByShowId(s.getShowid());
                        if (!tickets.isEmpty()) {
                            ticketRepository.deleteAll(tickets);
                            ticketRepository.flush();
                            logger.info("Deleted {} ticket(s) for show {} during event deletion",
                                    tickets.size(), s.getShowid());
                        }
                    }
                }

                // Now delete the event — JPA cascades handle shows, areas, blocks, rows, seats
                eventRepository.delete(event);
                eventRepository.flush();
                logger.info("Event {} deleted by manager {}", eventId, managerId);
            });
        });
    }

    // ── Shows ────────────────────────────────────────────────────────────────

    public void addShowToEvent(UUID eventId, show newShow, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.addShow(newShow, managerId);
                eventRepository.saveAndFlush(event);

                logger.info("Show added to event {}", eventId);
            });
        });
    }

    public void removeShowFromEvent(UUID eventId, show showToRemove, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {

                // Delete all tickets for this show first to satisfy the FK constraint
                if (showToRemove.getShowid() != null) {
                    List<ticket> showTickets = ticketRepository.findByShowId(showToRemove.getShowid());
                    if (!showTickets.isEmpty()) {
                        ticketRepository.deleteAll(showTickets);
                        ticketRepository.flush();
                        logger.info("Deleted {} ticket(s) for show {} before removing it",
                                showTickets.size(), showToRemove.getShowid());
                    }
                }

                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                logger.info("Removing show {} from event {}", showToRemove.getShowid(), eventId);

                event.removeShow(showToRemove, managerId);

                if (showToRemove.getShowid() != null) {
                    eventRepository.deleteShowById(showToRemove.getShowid());
                }

                eventRepository.saveAndFlush(event);
            });
        });
    }

    /** Returns the number of tickets that exist for a given show (for UI confirmation dialogs). */
    public int countTicketsForShow(UUID showId) {
        return ticketRepository.findByShowId(showId).size();
    }

    /**
     * Updates only the basic fields of an existing show (name, description, singer, date,
     * seatedPrice, standingPrice) without touching its areas or seats.
     * This avoids referential-integrity violations caused by deleting seats that tickets
     * still reference.
     */
    public void updateShowBasicFields(UUID eventId, UUID showId,
                                      String name, String description,
                                      String singer, Date showDate,
                                      BigDecimal seatedPrice, BigDecimal standingPrice,
                                      Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                show target = event.getShows().stream()
                        .filter(s -> showId.equals(s.getShowid()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Show not found in event"));

                target.setName(name);
                target.setDescription(description);
                target.setSinger(singer);
                target.setShowDate(showDate);
                target.setSeatedPrice(seatedPrice);
                target.setStandingPrice(standingPrice);

                logger.info("Updated basic fields of show {} in event {} by manager {}",
                        showId, eventId, managerId);

                eventRepository.save(event);
            });
        });
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
        String key = eventId + ":" + showId;

        return keyedLock.callLocked(LOCK_NS_EVENT, key, () -> transactionTemplate.execute(status -> {
            logger.info("Editing show {} in event {} by manager {}", showId, eventId, managerId);
            return eventRepository.editShowInEvent(
                    eventId, showId, name, description, singer, showDate, managerId);
        }));
    }

    // ── Areas ────────────────────────────────────────────────────────────────

    public boolean addAreaToShow(UUID eventId, UUID showId, String areaName, int capacity,
            double price, Long managerId) {
        String key = eventId + ":" + showId + ":" + areaName;

        return keyedLock.callLocked(LOCK_NS_EVENT, key, () -> transactionTemplate.execute(status -> {
            logger.info("Adding area {} to show {} in event {} by manager {}",
                    areaName, showId, eventId, managerId);
            return eventRepository.addAreaToShow(eventId, showId, areaName, capacity, price, managerId);
        }));
    }

    public boolean removeAreaFromShow(UUID eventId, UUID showId, String areaName) {
        String key = eventId + ":" + showId + ":" + areaName;

        return keyedLock.callLocked(LOCK_NS_EVENT, key, () -> transactionTemplate.execute(status -> {
            logger.info("Removing area {} from show {} in event {}", areaName, showId, eventId);
            return eventRepository.removeAreaFromShow(eventId, showId, areaName);
        }));
    }

    // ── Managers / Ownership ─────────────────────────────────────────────────

    public void assignManager(UUID eventId, Long newManagerId, Long currentOwnerId) {
        String key = eventId + ":" + newManagerId;

        keyedLock.runLocked(LOCK_NS_EVENT_MANAGER, key, () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                logger.info("Assigning manager {} to event {}", newManagerId, eventId);

                event.addManager(newManagerId, currentOwnerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void removeManager(UUID eventId, Long managerIdToRemove, Long currentOwnerId) {
        String key = eventId + ":" + managerIdToRemove;

        keyedLock.runLocked(LOCK_NS_EVENT_MANAGER, key, () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                logger.info("Removing manager {} from event {}", managerIdToRemove, eventId);

                event.removeManager(managerIdToRemove, currentOwnerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void transferOwnership(UUID eventId, Long newOwnerId, Long currentOwnerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                logger.info("Transferring ownership of event {} from {} to {}",
                        eventId, currentOwnerId, newOwnerId);

                event.transferOwnership(newOwnerId, currentOwnerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    // ── Edit Event Fields ────────────────────────────────────────────────────

    public void editEventName(UUID eventId, String newName, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editName(newName, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventType(UUID eventId, show_type newType, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editType(newType, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventDates(UUID eventId, Date newStartDate, Date newEndDate, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editDates(newStartDate, newEndDate, managerId);

                //notify all buyers that event dates changed
                notifyEventBuyers(
                        event.getEventId(),
                        event.getName(),
                        false
                );
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventDescription(UUID eventId, String newDescription, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editDescription(newDescription, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventVenue(UUID eventId, String newVenue, Long managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editVenue(newVenue, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    // ── Queries ──────────────────────────────────────────────────────────────
    // @Transactional(readOnly = true)
    public Event getEventDetails(UUID eventId) {
        logger.info("Retrieving details for event {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }

    public List<Event> getAllEvents() {
        logger.info("Retrieving all events");
        return eventRepository.findAllEvents();
    }

    public List<Event> getEventsByCompany(UUID companyId) {
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

    @Transactional(readOnly = true)
    public List<Long> getEventManagerIds(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return new ArrayList<>(event.getManagerIds());
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
        logger.info("Filtering events — name: {}, type: {}, from: {}, to: {}",
                name, eventType, startDate, endDate);
        return eventRepository.getEventsByFilter(name, eventType, startDate, endDate);
    }

    // ── Reviews ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<UUID, Integer> getEventReviews(UUID eventId) {
        logger.info("Retrieving reviews for event {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return new HashMap<>(event.getReviews());
    }

    public void addReviewToEvent(UUID eventId, UUID userId, int rating) {
        String key = eventId + ":" + userId;

        keyedLock.runLocked(LOCK_NS_EVENT_REVIEW, key, () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                logger.info("Adding review to event {} by user {}", eventId, userId);

                event.addReview(userId, rating);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    // ── Tickets / Seats ──────────────────────────────────────────────────────

    public boolean bookSeat(UUID eventId, UUID showId, String areaName, int seatNumber, Long userId) {
        String key = eventId + ":" + showId + ":" + areaName + ":" + seatNumber;

        return keyedLock.callLocked(LOCK_NS_EVENT_SEAT, key, () -> transactionTemplate.execute(status -> {
            logger.info("Booking seat {} in area {} for show {} in event {} by user {}",
                    seatNumber, areaName, showId, eventId, userId);

            return eventRepository.bookSeat(eventId, showId, areaName, seatNumber, userId);
        }));
    }

    // ── Show loading (eagerly initializes all lazy collections) ─────────────

    @Transactional
    public show loadShowFully(UUID eventId, UUID showId) {
        show s = eventRepository.getShowDetails(eventId, showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        List<Area> areas = s.getAreas();
        if (areas != null) {
            for (Area area : areas) {
                if (area instanceof SeatedArea sa) {
                    for (Block block : sa.getBlocks()) {
                        List<Row> rows = block.getRows();
                        if (rows != null) {
                            for (Row row : rows) {
                                List<Seat> seats = row.getSeats();
                                if (seats != null) seats.size(); // force-init
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    // ── Ticket reservation ───────────────────────────────────────────────────

    @Transactional
    public ticket reserveSeat(UUID eventId, UUID showId, UUID areaId, Long seatId, UUID userId) {
        String key = eventId + ":" + showId + ":" + areaId + ":" + seatId;
        return keyedLock.callLocked(LOCK_NS_EVENT_SEAT, key, () -> {
            Optional<ticket> existing = ticketRepository
                    .findFirstByShowIdAndSeat_IdAndStatus(showId, seatId, ticket.TicketStatus.AVAILABLE);
            if (existing.isPresent()) {
                ticket t = existing.get();
                t.lockInCart(userId);
                return ticketRepository.save(t);
            }
            // Generate a new ticket on-the-fly for this seat
            Seat seat = eventRepository.findSeatById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));
            Area area = eventRepository.findAreaById(areaId)
                    .orElseThrow(() -> new RuntimeException("Area not found"));
            show s = eventRepository.getShowDetails(eventId, showId)
                    .orElseThrow(() -> new RuntimeException("Show not found"));
            BigDecimal seatedPrice = s.getSeatedPrice() != null ? s.getSeatedPrice() : new BigDecimal("50.00");
            ticket t = new ticket(UUID.randomUUID(), showId, seat, area,
                    s.getShowDate(), seatedPrice);
            t.lockInCart(userId);
            logger.info("Generated and reserved seated ticket for seat {} by user {}", seatId, userId);
            return ticketRepository.save(t);
        });
    }

    @Transactional
    public ticket reserveStanding(UUID eventId, UUID showId, UUID areaId, UUID userId) {
        String key = eventId + ":" + showId + ":" + areaId + ":standing";
        return keyedLock.callLocked(LOCK_NS_EVENT_SEAT, key, () -> {
            Optional<ticket> existing = ticketRepository
                    .findFirstByShowIdAndArea_IdAndSeatIsNullAndStatus(showId, areaId, ticket.TicketStatus.AVAILABLE);
            if (existing.isPresent()) {
                ticket t = existing.get();
                t.lockInCart(userId);
                return ticketRepository.save(t);
            }
            Area area = eventRepository.findAreaById(areaId)
                    .orElseThrow(() -> new RuntimeException("Standing area not found"));
            if (!(area instanceof StandingArea sa) || sa.isFull())
                throw new RuntimeException("No standing spots available");
            show s = eventRepository.getShowDetails(eventId, showId)
                    .orElseThrow(() -> new RuntimeException("Show not found"));
            BigDecimal standingPrice = s.getStandingPrice() != null ? s.getStandingPrice() : new BigDecimal("30.00");
            ticket t = new ticket(UUID.randomUUID(), showId, area, s.getShowDate(), standingPrice);
            t.lockInCart(userId);
            logger.info("Generated and reserved standing ticket for area {} by user {}", areaId, userId);
            return ticketRepository.save(t);
        });
    }

    //helper function
    private void notifyEventBuyers(UUID eventId,
                               String eventName,
                               boolean cancelled) {

        purchaseRepository.findByEventId(eventId)
                .forEach(purchase -> {

                    String buyerId = purchase.getbuyerId();

                    if (cancelled) {
                        notificationService.notifyEventCancelled(
                                buyerId,
                                eventName
                        );
                    } else {
                        notificationService.notifyEventRescheduled(
                                buyerId,
                                eventName
                        );
                    }
                });
    }
}