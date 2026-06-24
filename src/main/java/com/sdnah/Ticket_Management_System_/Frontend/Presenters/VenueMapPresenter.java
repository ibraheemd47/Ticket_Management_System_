package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueAreaRefDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueMapDTO;
import com.sdnah.Ticket_Management_System_.Frontend.VenueMapEditorView;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link VenueMapEditorView} (II.4.2). Resolves the acting
 * manager, loads/saves the event's graphical venue map (stored as JSON on the
 * event), and supplies the inventory areas a map element can link to.
 */
@Component
@UIScope
public class VenueMapPresenter {

    private final EventService eventService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private VenueMapEditorView view;
    private String token;
    private UUID eventId;
    private String managerId;

    public VenueMapPresenter(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    public void setView(VenueMapEditorView view) {
        this.view = view;
    }

    /**
     * Bind the session and verify the user may manage this event (owner /
     * manager of the event). Returns {@code false} when access is denied or the
     * state is incomplete, so the view can bounce the user back.
     */
    public boolean bind(String token, UUID eventId) {
        this.token = token;
        this.eventId = eventId;
        if (token == null || eventId == null) return false;
        try {
            this.managerId = userService.getMemberByToken(token).getMemberId();
        } catch (RuntimeException ex) {
            return false;
        }
        try {
            return eventService.getEventManagerIds(eventId).contains(managerId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public UUID getEventId() { return eventId; }

    public String getEventName() {
        try {
            String name = eventService.getEventName(eventId);
            return name != null ? name : "Event";
        } catch (RuntimeException ex) {
            return "Event";
        }
    }

    /** Load the stored map, or a fresh empty one if none/unparseable. */
    public VenueMapDTO loadMap() {
        try {
            String json = eventService.getEventMapJson(eventId);
            if (json == null || json.isBlank()) return new VenueMapDTO();
            return objectMapper.readValue(json, VenueMapDTO.class);
        } catch (Exception ex) {
            return new VenueMapDTO();
        }
    }

    /** Inventory areas this map can link elements to (II.4.2). */
    public List<VenueAreaRefDTO> getAreas() {
        try {
            return eventService.getEventAreaRefs(eventId);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /** {@code {standingCapacity, blocks, rowsPerBlock, seatsPerRow}} for pre-populating the map. */
    public int[] getSeatingDims() {
        try {
            return eventService.getEventSeatingDims(eventId);
        } catch (RuntimeException ex) {
            return new int[]{0, 0, 0, 0};
        }
    }

    /** Serialize and persist the map. Returns true on success. */
    public boolean saveMap(VenueMapDTO map) {
        try {
            String json = objectMapper.writeValueAsString(map);
            eventService.saveEventMap(eventId, json, managerId);
            return true;
        } catch (Exception ex) {
            view.showError("Couldn't save the venue map: " + ex.getMessage());
            return false;
        }
    }
}
