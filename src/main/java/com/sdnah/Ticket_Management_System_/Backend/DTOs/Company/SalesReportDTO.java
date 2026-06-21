package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated company sales report (II.4.6). One header row carrying the
 * totals, plus one {@link EventLine} per company event.
 *
 * <p>Kept deliberately flat — the view renders this directly into a grid,
 * so anything richer would just have to be unwrapped again on the Vaadin
 * side.
 */
public class SalesReportDTO {

    public static class EventLine {
        private final UUID eventId;
        private final String eventName;
        private final int orders;
        private final BigDecimal revenue;

        public EventLine(UUID eventId, String eventName, int orders, BigDecimal revenue) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.orders = orders;
            this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }

        public UUID getEventId()       { return eventId; }
        public String getEventName()   { return eventName; }
        public int getOrders()         { return orders; }
        public BigDecimal getRevenue() { return revenue; }
    }

    private final UUID companyId;
    private final String companyName;
    private final int totalOrders;
    private final BigDecimal totalRevenue;
    private final List<EventLine> perEvent;

    public SalesReportDTO(UUID companyId, String companyName,
                          int totalOrders, BigDecimal totalRevenue,
                          List<EventLine> perEvent) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.perEvent = perEvent == null ? List.of() : perEvent;
    }

    public UUID getCompanyId()        { return companyId; }
    public String getCompanyName()    { return companyName; }
    public int getTotalOrders()       { return totalOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public List<EventLine> getPerEvent() { return perEvent; }
}
