package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.vaadin.flow.router.Route;

@Route("areaSelection")
public class AreaSelectionView {
    // Placeholder for area selection view
    // You can add components here to allow users to select different areas of the application
    private final EventService eventService;

    public AreaSelectionView(EventService eventService) {
        this.eventService = eventService;
    }

}
