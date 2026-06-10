package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Frontend.OrderDetailsView;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.spring.annotation.UIScope;

@Component
@UIScope
public class OrderDetailsPresenter {

    private final ActiveOrderService orderService;
    private final TicketService ticketService;
    private final UserService userService;
    private final EventService eventService;

    private OrderDetailsView view;

    public OrderDetailsPresenter(ActiveOrderService orderService,
                                 TicketService ticketService,
                                 UserService userService,
                                 EventService eventService) {
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.userService = userService;
        this.eventService = eventService;
    }

    public void setView(OrderDetailsView view) {
        this.view = view;
    }

    public String checkAccess(BeforeEnterEvent event) {
        Object token = event.getUI().getSession().getAttribute("token");

        if (token == null) {
            event.rerouteTo("login");
            return null;
        }

        return token.toString();
    }

    public List<OrderDTO> getActiveOrders(String token) {
        return orderService.getPendingOrdersByBuyer(token);
    }

    public List<PurchaseDTO> getPurchaseHistory(String token) {
        return orderService.getPurchaseHistory(token);
    }

    public List<ticket> getUserTickets(String token) {
        UUID ownerId = resolveOwnerUuid(token);

        if (ownerId == null) {
            return List.of();
        }

        return ticketService.getTicketsByOwner(ownerId);
    }

    public OrderDTO reserveSampleOrder(String token) {
        List<Event> events = eventService.getAllEvents();

        if (events == null || events.isEmpty()) {
            throw new RuntimeException("No events in the DB");
        }

        UUID eventId = events.get(0).getEventId();

        SeatRequest seat = new SeatRequest(
                UUID.randomUUID().toString(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("30")
        );

        return orderService.reserveTickets(token, eventId, List.of(seat));
    }

    private UUID resolveOwnerUuid(String token) {
        try {
            String memberId = userService.getMemberByToken(token).getMemberId();
            return UUID.fromString(memberId);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}