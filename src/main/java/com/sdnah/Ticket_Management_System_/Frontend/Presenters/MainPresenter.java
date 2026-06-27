package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.UIScope;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CreateComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Frontend.MainView;
import com.sdnah.Ticket_Management_System_.Frontend.NotificationBell;

@Component
@UIScope
public class MainPresenter {

    private final EventService eventService;
    private final UserService userService;
    private final company_managment_serivce companyService;
    private final NotificationService notificationService;
     private final ComplaintService complaintService;

    private MainView view;
    private List<EventDto> allEventDtos; // State moves here!

    public MainPresenter(EventService eventService, UserService userService, company_managment_serivce companyService,NotificationService  notificationService, ComplaintService complaintService) {
        this.eventService = eventService;
        this.userService = userService;
        this.companyService = companyService;
        this.notificationService = notificationService;
        this.complaintService = complaintService;
    }

    public void setView(MainView view) {
        this.view = view;
    }

public void loadInitialData() {
        try {
            // 1. Map Events to EventDtos (Already doing this)
            List<Event> events = eventService.getAllEvents();
            allEventDtos = events.stream().map(ev -> {
                java.time.LocalDateTime start = ev.getStartDate() != null
                        ? ev.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
                java.time.LocalDateTime end = ev.getEndDate() != null
                        ? ev.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
                return new EventDto(ev.getEventId(), ev.getName(), start, end, ev.getEventType(), ev.getVenue(), ev.getPhotoUrl());
            }).collect(Collectors.toList());

            view.showEvents(allEventDtos);
            
            // 2. NEW: Map Companies to CompanyDtos
            List<CompanyDTO> Companies = companyService.getActiveCompanies();
            
      

            // Pass ONLY the DTOs to the View
            view.showCompanies(Companies);

        } catch (Exception e) {
            view.showNotification("Error loading initial data.", true);
            e.printStackTrace();
        }
    }

    /** Average 1–5 rating for an event, or 0.0 when it has no reviews yet. */
    public double getEventAverageRating(UUID eventId) {
        if (eventId == null) return 0.0;
        try {
            Map<UUID, Integer> reviews = eventService.getEventReviews(eventId);
            if (reviews == null || reviews.isEmpty()) return 0.0;
            return reviews.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public boolean isSystemAdmin(String token) {
        return userService.isSystemAdmin(token);
    }

    public void logout(String token) {
        if (token != null) {
            userService.logout(token);
        }
        view.reloadPage();
    }

    /** Back-compat entry point — search with no category / rating / price filters. */
    public void performSearch(String filterMode, String query, Date startDate, Date endDate) {
        performSearch(filterMode, query, startDate, endDate, null, null, null, null);
    }

    /**
     * Search events with the optional advanced filters from II.2.3:
     * {@code category} (event type), {@code minRating}, and a price range
     * ({@code minPrice}/{@code maxPrice}). Text/date drive the base result set
     * (as before); the advanced filters are then applied on top.
     */
    public void performSearch(String filterMode, String query, Date startDate, Date endDate,
                              show_type category, Double minRating, BigDecimal minPrice, BigDecimal maxPrice) {
        String text = query != null ? query.trim() : "";

        try {
            List<EventDto> results;
            boolean noBaseCriteria = text.isEmpty() && startDate == null && endDate == null;
            if (noBaseCriteria) {
                // No text/date — start from the full catalogue so the advanced
                // filters still have something to narrow.
                results = allEventDtos != null ? new ArrayList<>(allEventDtos) : new ArrayList<>();
            } else {
                List<EventDto> base = computeBaseResults(filterMode, text, startDate, endDate);
                results = base != null ? new ArrayList<>(base) : new ArrayList<>();
            }

            results = applyAdvancedFilters(results, category, minRating, minPrice, maxPrice);

            if (results != null && !results.isEmpty()) {
                view.showEvents(results);
            } else {
                view.clearEvents();
                view.showNotification("No events were found matching your search.", false);
            }

        } catch (Exception ex) {
            view.showNotification("Search Error: " + ex.getMessage(), true);
            System.err.println("Search Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Text/date driven base search, unchanged from the original routing. */
    private List<EventDto> computeBaseResults(String filterMode, String text, Date startDate, Date endDate) {
        List<EventDto> results = null;
        if (filterMode.equals("Event")) {
            if (!text.isEmpty()) {
                results = eventService.searchEventByName(text);
            } else if (startDate != null && endDate != null) {
                results = eventService.searchEventsByDateRange(startDate, endDate);
            } else if (startDate != null) {
                results = eventService.searchEventsByStartDate(startDate);
            } else if (endDate != null) {
                results = eventService.searchEventsByEndDate(endDate);
            }
        } else if (filterMode.equals("Company")) {
            if (!text.isEmpty()) {
                if (startDate != null && endDate != null) {
                    results = companyService.searchEventsInCompanyByDateRange(text, startDate, endDate);
                } else if (startDate != null) {
                    results = companyService.searchEventsInCompanyByStartDate(text, startDate);
                } else if (endDate != null) {
                    results = companyService.searchEventsInCompanyByEndDate(text, endDate);
                } else {
                    results = companyService.searchEventsInCompanyByKeyword(text, text);
                }
            }
        } else if (filterMode.equals("Venue")) {
            if (!text.isEmpty()) {
                results = eventService.searchEventsByVenue(text);
            }
        }
        return results;
    }

    /** Apply the II.2.3 advanced filters (category / rating / price range). */
    private List<EventDto> applyAdvancedFilters(List<EventDto> events, show_type category,
                                                Double minRating, BigDecimal minPrice, BigDecimal maxPrice) {
        if (events == null) return new ArrayList<>();
        Stream<EventDto> s = events.stream();
        if (category != null) {
            s = s.filter(e -> e.eventType == category);
        }
        if (minRating != null && minRating > 0) {
            s = s.filter(e -> getEventAverageRating(e.id) >= minRating);
        }
        if (minPrice != null || maxPrice != null) {
            s = s.filter(e -> eventPriceInRange(e.id, minPrice, maxPrice));
        }
        return s.collect(Collectors.toList());
    }

    /**
     * True when the event has a priced show whose range overlaps the requested
     * [{@code minPrice}, {@code maxPrice}] window (either bound may be null).
     * Events with no priced shows are excluded once a price filter is active.
     */
    private boolean eventPriceInRange(UUID eventId, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal[] bounds = eventService.getEventPriceBounds(eventId);
        if (bounds == null) return false;
        BigDecimal evMin = bounds[0], evMax = bounds[1];
        if (minPrice != null && evMax.compareTo(minPrice) < 0) return false; // entirely below window
        if (maxPrice != null && evMin.compareTo(maxPrice) > 0) return false; // entirely above window
        return true;
    }

   

    public List<ComplaintDTO> getUserComplaints(String token) {
        try {
            return complaintService.getUserComplaints(token);
        } catch (Exception ex) {
            view.showNotification("Error fetching complaints: " + ex.getMessage(), true);
            return null;
        }
    }

    public void createComplaint(String token, CreateComplaintDTO request) {
        try {
            complaintService.createComplaint(token, request);
            view.showNotification("Complaint submitted successfully", false);
        } catch (Exception ex) {
            view.showNotification("Error submitting complaint: " + ex.getMessage(), true);
        }
    }
}