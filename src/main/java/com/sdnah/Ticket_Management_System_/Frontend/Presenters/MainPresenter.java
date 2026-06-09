package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.UIScope;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
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

    public boolean isSystemAdmin(String token) {
        return userService.isSystemAdmin(token);
    }

    public void logout(String token) {
        if (token != null) {
            userService.logout(token);
        }
        view.reloadPage();
    }

    public void performSearch(String filterMode, String query, Date startDate, Date endDate) {
        String text = query != null ? query.trim() : "";
        List<EventDto> results = null;

        try {
            if (text.isEmpty() && startDate == null && endDate == null) {
                if (allEventDtos != null) {
                    view.showEvents(allEventDtos);
                }
                return;
            }

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

    public NotificationBell CreateNotificationBell() {
        NotificationBell notifcation_bell = new NotificationBell(notificationService, userService);
      return notifcation_bell;
    }

    public List<ComplaintDTO> getUserComplaints(String token) {
        try {
            return complaintService.getUserComplaints(token);
        } catch (Exception ex) {
            view.showNotification("Error fetching complaints: " + ex.getMessage(), true);
            return null;
        }
    }
}