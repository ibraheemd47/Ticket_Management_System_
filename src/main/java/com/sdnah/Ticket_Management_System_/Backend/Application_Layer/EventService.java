package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueAreaRefDTO;
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
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;

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
    private final com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository companyRepository;

    public EventService(IEventRepository eventRepository,
            PurchaseRepository purchaseRepository,
            NotificationService notificationService,
            KeyedLock keyedLock,
            TransactionTemplate transactionTemplate,
            TicketRepository ticketRepository,
            com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository companyRepository) {
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
                this.companyRepository = companyRepository;
    }

    // ── Creation / Deletion ──────────────────────────────────────────────────

    public Event createEvent(EventDto dto, UUID companyId, String ownerId) {
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);
        return eventRepository.saveAndFlush(event);
    }

    /**
     * Permanently deletes an event and everything under it:
     * tickets → shows/areas → event.
     */
    public void deleteEvent(UUID eventId, String managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                // Verify caller is the owner (delegates to the domain check). This was
                // accidentally dropped during the company-id UUID refactor; the
                // tests in EventAcceptanceTest / EventIntegrationTest expect it.
                event.delete(managerId);

                // Snapshot the name before delete() detaches the entity, then
                // notify everyone who already bought a ticket for it (II.4.5
                // requirement — purchase history must reflect cancellation).
                String snapshotName = event.getName();
                notifyEventBuyers(eventId, snapshotName, true);

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

                // Remember the company id BEFORE deleting the event — the Event
                // entity becomes detached after delete().
                UUID owningCompanyId = event.getCompanyId();

                // Now delete the event — JPA cascades handle shows, areas, blocks, rows, seats
                eventRepository.delete(event);
                eventRepository.flush();

                // Clean up the owning Company's associatedEventIds collection
                // (the company_events join table) so the events list in the
                // managing-company view no longer shows the deleted event.
                if (owningCompanyId != null) {
                    companyRepository.findById(owningCompanyId).ifPresent(c -> {
                        c.removeEventIfPresent(eventId);
                        companyRepository.save(c);
                    });
                }

                logger.info("Event {} deleted by manager {}", eventId, managerId);
            });
        });
    }

    // ── Shows ────────────────────────────────────────────────────────────────

    public void addShowToEvent(UUID eventId, show newShow, String managerId) {
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

    public void removeShowFromEvent(UUID eventId, show showToRemove, String managerId) {
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
                                      String managerId) {
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

    // ── Seating layout edits ─────────────────────────────────────────────────

    /**
     * Current seating dimensions of a show as
     * {@code [standingCapacity, numBlocks, rowsPerBlock, seatsPerRow]}.
     * Read inside a transaction so the lazy seat collections initialise safely.
     */
    @Transactional(readOnly = true)
    public int[] getShowSeatingDims(UUID eventId, UUID showId) {
        show s = eventRepository.getShowDetails(eventId, showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        int standingCap = 0, numBlocks = 0, rowsPerBlock = 0, seatsPerRow = 0;
        if (s.getAreas() != null) {
            for (Area a : s.getAreas()) {
                if (a instanceof StandingArea sa) {
                    standingCap = sa.getMaxCapacity();
                } else if (a instanceof SeatedArea sea) {
                    Block[] blocks = sea.getBlocks();
                    numBlocks = blocks.length;
                    if (numBlocks > 0) {
                        List<Row> rows = blocks[0].getRows();
                        rowsPerBlock = rows != null ? rows.size() : 0;
                        if (rowsPerBlock > 0) {
                            List<Seat> seats = rows.get(0).getSeats();
                            seatsPerRow = seats != null ? seats.size() : 0;
                        }
                    }
                }
            }
        }
        return new int[] { standingCap, numBlocks, rowsPerBlock, seatsPerRow };
    }

    /**
     * Resizes the seating of an existing show <b>without regenerating tickets</b>.
     * Tickets stay lazily generated at booking time; this only adjusts the
     * standing capacity and/or the seated block/row/seat structure.
     *
     * <p>Editing is allowed even when seats are booked (force resize):
     * <ul>
     *   <li>Growing never touches an existing seat, so all bookings are kept.</li>
     *   <li>Shrinking keeps every seat that still fits the new grid (positionally),
     *       along with its booking. Seats that fall outside the new layout are
     *       removed and <b>their tickets are cancelled</b> — including purchased
     *       ones (the manager has accepted this trade-off).</li>
     * </ul>
     * Removing an area entirely is not supported here — delete the show for that.
     */
    public void updateShowSeating(UUID eventId, UUID showId,
                                  int standingCap, int numBlocks,
                                  int rowsPerBlock, int seatsPerRow,
                                  String managerId) {
        if (numBlocks > 0 && (rowsPerBlock <= 0 || seatsPerRow <= 0))
            throw new RuntimeException(
                    "Seated area needs blocks, rows per block and seats per row all > 0");

        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () ->
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));
                show target = event.getShows().stream()
                        .filter(s -> showId.equals(s.getShowid()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Show not found in event"));

                List<Area> areas = target.getAreas() != null ? target.getAreas() : new ArrayList<>();
                StandingArea standing = null;
                SeatedArea seated = null;
                for (Area a : areas) {
                    if (a instanceof StandingArea sa) standing = sa;
                    else if (a instanceof SeatedArea sea) seated = sea;
                }

                // ── Standing ──────────────────────────────────────────────────
                // Standing tickets aren't tied to a specific seat, so lowering the
                // capacity below the booked count is non-destructive (the area is
                // simply oversold and shows zero availability); allow it freely.
                if (standing != null) {
                    if (standingCap <= 0)
                        throw new RuntimeException(
                                "To remove the standing area, delete the show instead");
                    standing.setMaxCapacity(standingCap);
                } else if (standingCap > 0) {
                    areas.add(new StandingArea("Standing Area", standingCap));
                }

                // ── Seated ────────────────────────────────────────────────────
                if (seated != null) {
                    if (numBlocks <= 0)
                        throw new RuntimeException(
                                "To remove the seated area, delete the show instead");
                    if (!seatedDimsEqual(seated, numBlocks, rowsPerBlock, seatsPerRow))
                        resizeSeatedInPlace(seated, showId, target.getName(),
                                numBlocks, rowsPerBlock, seatsPerRow);
                } else if (numBlocks > 0) {
                    SeatedArea newSeated = new SeatedArea("Seated Area", numBlocks);
                    newSeated.setBlocks(buildBlocks(newSeated, numBlocks, rowsPerBlock, seatsPerRow));
                    areas.add(newSeated);
                }

                target.setAreas(areas);
                logger.info("Resized seating of show {} in event {} by manager {} "
                        + "(standing={}, blocks={}, rows={}, seats={})",
                        showId, eventId, managerId, standingCap, numBlocks, rowsPerBlock, seatsPerRow);
                eventRepository.save(event);
            }));
    }

    /**
     * Resizes a seated area in place to {@code targetBlocks × targetRows × targetSeats},
     * preserving the seats (and their tickets) that still fit the new grid and
     * dropping the rest. Tickets for dropped seats are deleted first so the seat
     * rows can be removed without violating the {@code seat_id} foreign key.
     * Must run inside the active transaction.
     */
    private void resizeSeatedInPlace(SeatedArea seated, UUID showId, String showName,
                                     int targetBlocks, int targetRows, int targetSeats) {
        List<Block> blocks = seated.blocksLive();

        // 1. Find every seat that falls outside the new grid (by position) and
        //    cancel its tickets — booked or not — before the seat row is removed.
        List<Long> droppedSeatIds = new ArrayList<>();
        for (int b = 0; b < blocks.size(); b++) {
            List<Row> rows = blocks.get(b).getRows();
            for (int r = 0; r < rows.size(); r++) {
                List<Seat> seats = rows.get(r).getSeats();
                for (int s = 0; s < seats.size(); s++) {
                    boolean keep = b < targetBlocks && r < targetRows && s < targetSeats;
                    if (!keep) droppedSeatIds.add(seats.get(s).getId());
                }
            }
        }
        deleteTicketsForSeats(showId, showName, droppedSeatIds);

        // 2. Drop whole blocks beyond the target count (orphanRemoval cascades
        //    to their rows/seats).
        while (blocks.size() > targetBlocks)
            blocks.remove(blocks.size() - 1);

        // 3. Reshape each surviving block to targetRows × targetSeats.
        for (Block block : blocks) {
            List<Row> rows = block.getRows();
            while (rows.size() > targetRows)
                rows.remove(rows.size() - 1);
            for (Row row : rows) {
                List<Seat> seats = row.getSeats();
                while (seats.size() > targetSeats)
                    seats.remove(seats.size() - 1);
                for (int s = seats.size(); s < targetSeats; s++)
                    seats.add(newSeat(s + 1, row));
            }
            for (int r = rows.size(); r < targetRows; r++)
                rows.add(newRow(r + 1, targetSeats, block));
        }

        // 4. Append brand-new blocks until the target count is reached.
        for (int b = blocks.size(); b < targetBlocks; b++)
            blocks.add(newBlock(b, targetRows, targetSeats, seated));

        seated.setNumberofBlocks(blocks.size());
    }

    /**
     * Deletes all tickets (any status) that reference the given seat ids, after
     * notifying the holders of any booked ones that their seat was removed.
     */
    private void deleteTicketsForSeats(UUID showId, String showName, List<Long> seatIds) {
        if (seatIds.isEmpty()) return;
        java.util.Set<Long> ids = new java.util.HashSet<>(seatIds);
        List<ticket> toDelete = ticketRepository.findByShowId(showId).stream()
                .filter(t -> t.getSeat() != null && ids.contains(t.getSeat().getId()))
                .toList();
        if (toDelete.isEmpty()) return;

        notifyHoldersOfRemovedSeats(showName, toDelete);
        ticketRepository.deleteAll(toDelete);
        ticketRepository.flush();   // clear seat_id FKs before the seats are removed
    }

    /**
     * Notifies each holder of a removed booked seat (one grouped message per
     * holder). Placeholder tickets with no owner are skipped. Delivery failures
     * are logged, never propagated, so they can't roll back the resize.
     */
    private void notifyHoldersOfRemovedSeats(String showName, List<ticket> removed) {
        Map<UUID, List<ticket>> byOwner = removed.stream()
                .filter(t -> t.getOwnerId() != null)
                .collect(Collectors.groupingBy(ticket::getOwnerId));

        for (Map.Entry<UUID, List<ticket>> entry : byOwner.entrySet()) {
            int count = entry.getValue().size();
            boolean anyPaid = entry.getValue().stream()
                    .anyMatch(t -> t.getStatus() == ticket.TicketStatus.PURCHASED
                                || t.getStatus() == ticket.TicketStatus.SCANNED);
            String message = count + " seat(s) you reserved for \"" + showName
                    + "\" were removed because the organizer changed the seating layout."
                    + (anyPaid ? " As you had already completed a purchase, please contact the"
                               + " organizer about a refund." : "");
            try {
                notificationService.createNotification(
                        entry.getKey().toString(), message, NotificationType.GENERIC);
            } catch (RuntimeException ex) {
                logger.warn("Failed to notify {} about removed seats for show \"{}\": {}",
                        entry.getKey(), showName, ex.getMessage());
            }
        }
    }

    /** True when the seated area already has exactly the requested dimensions. */
    private static boolean seatedDimsEqual(SeatedArea seated, int numBlocks,
                                           int rowsPerBlock, int seatsPerRow) {
        Block[] blocks = seated.getBlocks();
        if (blocks.length != numBlocks) return false;
        if (numBlocks == 0) return true;
        List<Row> rows = blocks[0].getRows();
        int curRows = rows != null ? rows.size() : 0;
        if (curRows != rowsPerBlock) return false;
        if (curRows == 0) return seatsPerRow == 0;
        List<Seat> seats = rows.get(0).getSeats();
        int curSeats = seats != null ? seats.size() : 0;
        return curSeats == seatsPerRow;
    }

    // New entities carry a null id so JPA treats them as transient (IDENTITY insert).
    private static Seat newSeat(int seatNumber, Row row) {
        Seat seat = new Seat(0, String.valueOf(seatNumber), row);
        seat.setId(null);
        return seat;
    }

    private static Row newRow(int rowNumber, int seatsPerRow, Block block) {
        Row row = new Row(0, String.valueOf(rowNumber), seatsPerRow, block);
        row.setId(null);
        List<Seat> seats = new ArrayList<>();
        for (int s = 1; s <= seatsPerRow; s++)
            seats.add(newSeat(s, row));
        row.setSeats(seats);
        return row;
    }

    private static Block newBlock(int blockIndex, int rowsPerBlock, int seatsPerRow, SeatedArea seated) {
        Block block = new Block(0, String.valueOf((char) ('A' + blockIndex)), rowsPerBlock, seated);
        block.setId(null);
        List<Row> rows = new ArrayList<>();
        for (int r = 1; r <= rowsPerBlock; r++)
            rows.add(newRow(r, seatsPerRow, block));
        block.setRows(rows);
        return block;
    }

    /** Builds a fresh block/row/seat structure (for adding a new seated area). */
    private static List<Block> buildBlocks(SeatedArea seated, int numBlocks,
                                           int rowsPerBlock, int seatsPerRow) {
        List<Block> blocks = new ArrayList<>();
        for (int b = 0; b < numBlocks; b++)
            blocks.add(newBlock(b, rowsPerBlock, seatsPerRow, seated));
        return blocks;
    }

    public List<show> getShowsForEvent(UUID eventId) {
        logger.info("Retrieving shows for event {}", eventId);
        return eventRepository.getShowsForEvent(eventId);
    }

    /**
     * Min/max ticket price across all of an event's shows (seated + standing),
     * or {@code null} when the event has no priced shows. Used by search to
     * filter by price range (II.2.3).
     *
     * @return {@code {min, max}} or {@code null}
     */
    public java.math.BigDecimal[] getEventPriceBounds(UUID eventId) {
        if (eventId == null) return null;
        // Fetch shows via a direct query (their seated/standing prices are basic
        // columns, loaded eagerly) rather than navigating the lazy shows
        // collection — so this works outside a transaction.
        List<show> shows = eventRepository.getShowsForEvent(eventId);
        if (shows == null || shows.isEmpty()) return null;
        java.math.BigDecimal min = null, max = null;
        for (show s : shows) {
            for (java.math.BigDecimal p : new java.math.BigDecimal[]{ s.getSeatedPrice(), s.getStandingPrice() }) {
                if (p == null) continue;
                if (min == null || p.compareTo(min) < 0) min = p;
                if (max == null || p.compareTo(max) > 0) max = p;
            }
        }
        return min == null ? null : new java.math.BigDecimal[]{ min, max };
    }

    // ── II.4.2: Venue layout / event map ──────────────────────────────────────

    /** Persist the graphical venue map JSON. Domain enforces manager authorization. */
    public void saveEventMap(UUID eventId, String mapJson, String managerId) {
        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            event.setVenueMapJson(mapJson, managerId);
            eventRepository.saveAndFlush(event);
        });
        logger.info("Venue map saved for event {} by manager {}", eventId, managerId);
    }

    /** Stored venue map JSON for an event, or {@code null} if none/no event. */
    public String getEventMapJson(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(Event::getVenueMapJson)
                .orElse(null);
    }

    /** Event display name, or {@code null} if the event doesn't exist. */
    public String getEventName(UUID eventId) {
        return eventRepository.findById(eventId).map(Event::getName).orElse(null);
    }

    /**
     * Seating dimensions for an event, taken from the first show that has any
     * seating: {@code {standingCapacity, blocks, rowsPerBlock, seatsPerRow}}.
     * Used to pre-populate the venue map from existing inventory (II.4.2).
     */
    public int[] getEventSeatingDims(UUID eventId) {
        return transactionTemplate.execute(status -> {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null || event.getShows() == null) return new int[]{0, 0, 0, 0};
            for (show s : event.getShows()) {
                int standingCap = 0, numBlocks = 0, rowsPerBlock = 0, seatsPerRow = 0;
                if (s.getAreas() != null) {
                    for (Area a : s.getAreas()) {
                        if (a instanceof StandingArea sa) {
                            standingCap = sa.getMaxCapacity();
                        } else if (a instanceof SeatedArea sea) {
                            Block[] blocks = sea.getBlocks();
                            numBlocks = blocks != null ? blocks.length : 0;
                            if (numBlocks > 0) {
                                List<Row> rws = blocks[0].getRows();
                                rowsPerBlock = rws != null ? rws.size() : 0;
                                if (rowsPerBlock > 0) {
                                    List<Seat> seatList = rws.get(0).getSeats();
                                    seatsPerRow = seatList != null ? seatList.size() : 0;
                                }
                            }
                        }
                    }
                }
                if (standingCap > 0 || numBlocks > 0)
                    return new int[]{standingCap, numBlocks, rowsPerBlock, seatsPerRow};
            }
            return new int[]{0, 0, 0, 0};
        });
    }

    /**
     * Distinct inventory areas (id, name, seated/standing) across the event's
     * shows — used to link venue-map elements to pricing/seating areas (II.4.2).
     */
    public List<VenueAreaRefDTO> getEventAreaRefs(UUID eventId) {
        return transactionTemplate.execute(status -> {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null || event.getShows() == null) return List.<VenueAreaRefDTO>of();
            java.util.LinkedHashMap<String, VenueAreaRefDTO> byId = new java.util.LinkedHashMap<>();
            for (show s : event.getShows()) {
                if (s.getAreas() == null) continue;
                for (Area a : s.getAreas()) {
                    if (a == null || a.getId() == null) continue;
                    String type = (a instanceof SeatedArea) ? "SEATED"
                                : (a instanceof StandingArea) ? "STANDING" : "AREA";
                    byId.putIfAbsent(a.getId().toString(),
                            new VenueAreaRefDTO(a.getId().toString(), a.getName(), type));
                }
            }
            return new java.util.ArrayList<>(byId.values());
        });
    }

    public show getShowDetails(UUID eventId, UUID showId) {
        logger.info("Retrieving show {} details for event {}", showId, eventId);
        return eventRepository.getShowDetails(eventId, showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
    }

    public boolean editShowInEvent(UUID eventId, UUID showId, String name, String description,
            String singer, Date showDate, String managerId) {
        String key = eventId + ":" + showId;

        return keyedLock.callLocked(LOCK_NS_EVENT, key, () -> transactionTemplate.execute(status -> {
            logger.info("Editing show {} in event {} by manager {}", showId, eventId, managerId);
            return eventRepository.editShowInEvent(
                    eventId, showId, name, description, singer, showDate, managerId);
        }));
    }

    // ── Areas ────────────────────────────────────────────────────────────────

    public boolean addAreaToShow(UUID eventId, UUID showId, String areaName, int capacity,
            double price, String managerId) {
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

    public void assignManager(UUID eventId, String newManagerId, String currentOwnerId) {
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

    public void removeManager(UUID eventId, String managerIdToRemove, String currentOwnerId) {
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

    public void transferOwnership(UUID eventId, String newOwnerId, String currentOwnerId) {
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

    public void editEventName(UUID eventId, String newName, String managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editName(newName, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventType(UUID eventId, show_type newType, String managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editType(newType, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    public void editEventDates(UUID eventId, Date newStartDate, Date newEndDate, String managerId) {
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

    public void editEventDescription(UUID eventId, String newDescription, String managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editDescription(newDescription, managerId);
                eventRepository.saveAndFlush(event);
            });
        });
    }

    /**
     * II.4.5 — Re-prices every still-AVAILABLE ticket of the given show
     * to {@code newPrice}, then notifies past buyers of the event that a
     * ticket price changed. Sold/scanned tickets are left untouched so we
     * never retroactively alter a completed purchase.
     */
    public void editShowPrice(UUID eventId,
                              UUID showId,
                              java.math.BigDecimal newPrice,
                              String managerId) {
        if (newPrice == null || newPrice.signum() < 0) {
            throw new IllegalArgumentException("price must be non-negative");
        }
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));
                // Same authorisation gate as other edit operations (e.g. editVenue).
                if (!event.getManagerIds().contains(managerId)) {
                    throw new IllegalArgumentException("Only managers can edit ticket prices.");
                }

                List<ticket> tickets = ticketRepository.findByShowId(showId);
                int repriced = 0;
                for (ticket t : tickets) {
                    if (t.getStatus() == ticket.TicketStatus.AVAILABLE) {
                        t.repriceIfAvailable(newPrice);
                        repriced++;
                    }
                }
                if (repriced > 0) {
                    ticketRepository.saveAll(tickets);
                    ticketRepository.flush();
                }

                String eventName = event.getName();
                purchaseRepository.findByEventId(eventId).forEach(p ->
                        notificationService.notifyEventPriceChanged(
                                p.getbuyerId(), eventName, null));

                logger.info("Repriced {} ticket(s) for show {} to {} by manager {}",
                        repriced, showId, newPrice, managerId);
            });
        });
    }

    public void editEventVenue(UUID eventId, String newVenue, String managerId) {
        keyedLock.runLocked(LOCK_NS_EVENT, eventId.toString(), () -> {
            transactionTemplate.executeWithoutResult(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new RuntimeException("Event not found"));

                event.editVenue(newVenue, managerId);
                eventRepository.saveAndFlush(event);

                // II.4.5 — every past buyer must learn about a place change.
                String eventName = event.getName();
                purchaseRepository.findByEventId(eventId).forEach(p ->
                        notificationService.notifyEventVenueChanged(
                                p.getbuyerId(), eventName, newVenue));
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

    public List<Event> getEventsByManager(String managerId) {
        logger.info("Retrieving events for manager {}", managerId);
        return eventRepository.findByManagerId(managerId);
    }

    public List<Event> getEventsByOwner(String ownerId) {
        logger.info("Retrieving events for owner {}", ownerId);
        return eventRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public List<String> getEventManagerIds(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return new ArrayList<>(event.getManagerIds());
    }

    // // ── Search ───────────────────────────────────────────────────────────────

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


    //search evet using keyword
    public List<EventDto> searchEventsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return eventRepository.findByNameContainingIgnoreCase(keyword.trim())
                .stream()
                .map(event -> new EventDto(
                        event.getEventId(),
                        event.getName(),
                        event.getStartDate() == null ? null : event.getStartDate().toString(),
                        event.getEventType(),
                        event.getVenue()
                ))
                .toList();
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

    // ── Search ───────────────────────────────────────────────────────────────

    // search by event name (matches name, case-insensitive substring)
    public List<EventDto> searchEventByName(String name) {
        logger.info("Searching events by name: {}", name);
        if (name == null || name.isBlank()) return List.of();
        return eventRepository.searchEventsByName(name.trim())
                .stream().map(this::toEventDto).toList();
    }

    // search by description only — DB has no index on description, use JPQL filter
    public List<EventDto> searchEventsByDescription(String description) {
        logger.info("Searching events by description: {}", description);
        if (description == null || description.isBlank()) return List.of();
        return eventRepository.searchEventsByDescription(description.trim())
                .stream().map(this::toEventDto).toList();
    }

    // search by category / event type
    public List<EventDto> searchEventsByCategory(show_type category) {
        logger.info("Searching events by category: {}", category);
        if (category == null) return List.of();
        return eventRepository.searchEventsByType(category)
                .stream().map(this::toEventDto).toList();
    }

    // search by start date (events starting on or after this date)
    public List<EventDto> searchEventsByStartDate(Date startDate) {
        logger.info("Searching events by start date: {}", startDate);
        if (startDate == null) return List.of();
        return eventRepository.getEventsByFilter(null, null, startDate, null)
                .stream().map(this::toEventDto).toList();
    }

    // search by end date (events ending on or before this date)
    public List<EventDto> searchEventsByEndDate(Date endDate) {
        logger.info("Searching events by end date: {}", endDate);
        if (endDate == null) return List.of();
        return eventRepository.getEventsByFilter(null, null, null, endDate)
                .stream().map(this::toEventDto).toList();
    }

    // search by date range
    public List<EventDto> searchEventsByDateRange(Date fromDate, Date toDate) {
        logger.info("Searching events by date range: {} to {}", fromDate, toDate);
        return eventRepository.getEventsByFilter(null, null, fromDate, toDate)
                .stream().map(this::toEventDto).toList();
    }

    // search by venue (substring match, case-insensitive)
    public List<EventDto> searchEventsByVenue(String venue) {
        logger.info("Searching events by venue: {}", venue);
        if (venue == null || venue.isBlank()) return List.of();
        return eventRepository.searchEventsByVenue(venue.trim())
                .stream().map(this::toEventDto).toList();
    }

    // search by minimum event rating — rating is computed in-memory from reviews map,
    // no DB column exists for it; load only active events then filter
    public List<EventDto> searchEventsByMinRating(double minRating) {
        logger.info("Searching events by min rating: {}", minRating);
        return eventRepository.findAllEvents().stream()
                .filter(e -> averageEventRating(e) >= minRating)
                .map(this::toEventDto)
                .toList();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private double averageEventRating(Event e) {
        Map<UUID, Integer> reviews = e.getReviews();
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private EventDto toEventDto(Event e) {
        return new EventDto(
                e.getEventId(),
                e.getName(),
                e.getStartDate() == null ? null : e.getStartDate().toString(),
                e.getEventType(),
                e.getVenue()
        );
    }
}