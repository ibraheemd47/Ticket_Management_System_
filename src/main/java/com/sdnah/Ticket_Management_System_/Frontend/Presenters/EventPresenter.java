package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ShowDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.vaadin.flow.spring.annotation.UIScope;

// IMPORTANT:
// Add your real ShowDTO import here.
// Use the same import you currently use in EventCreationView.
// import com.sdnah.Ticket_Management_System_.Backend.DTOs.ShowDTO;

@Component
@UIScope
public class EventPresenter {

    private final company_managment_serivce companyService;
    private final EventService eventService;
    private final TicketService ticketService;
    private final PolicyRepository policyRepo;
    private final ActiveOrderService orderService;

    private CreationView creationView;
    private DetailsView detailsView;

    private UUID cachedEventId;
    private UUID cachedUserId;
    private Event cachedEvent;
    private List<show> cachedShows = new ArrayList<>();

    public EventPresenter(
            company_managment_serivce companyService,
            EventService eventService,
            TicketService ticketService,
            PolicyRepository policyRepo,
            ActiveOrderService orderService) {

        this.companyService = companyService;
        this.eventService = eventService;
        this.ticketService = ticketService;
        this.policyRepo = policyRepo;
        this.orderService = orderService;
    }

    public void setCreationView(CreationView view) {
    this.creationView = view;
}

public void setDetailsView(DetailsView view) {
    this.detailsView = view;
}

    // ============================================================
    // EVENT CREATION LOGIC
    // ============================================================

    public void createEvent(
            String name,
            String venue,
            show_type type,
            LocalDate start,
            LocalDate end,
            List<ShowDTO> shows) {

        if (name == null || name.isBlank()) {
            creationView.showError("Event name is required");
            return;
        }

        if (venue == null || venue.isBlank()) {
            creationView.showError("Venue is required");
            return;
        }

        if (type == null) {
            creationView.showError("Please select an event type");
            return;
        }

        if (start == null) {
            creationView.showError("Start date is required");
            return;
        }

        if (end == null) {
            creationView.showError("End date is required");
            return;
        }

        if (end.isBefore(start)) {
            creationView.showError("End date cannot be before start");
            return;
        }

        Object tokenObj = creationView.getSessionAttribute("token");
        Object companyIdObj = creationView.getSessionAttribute("managingCompanyId");

        if (tokenObj == null) {
            creationView.showWarning("Not logged in — sign in first to save the event");
            return;
        }

        if (companyIdObj == null) {
            creationView.showWarning("No company selected — navigate from your company page");
            return;
        }

        String token = tokenObj.toString();
        UUID companyId = UUID.fromString(companyIdObj.toString());

        EventDto dto = new EventDto();
        dto.name = name.trim();
        dto.venue = venue.trim();
        dto.eventType = type;
        dto.startDate = start;
        dto.endDate = end;

        try {
            EventDto created = companyService.addEvent(token, companyId, dto);

            Object userIdObj = creationView.getSessionAttribute("userId");
            String memberId = userIdObj != null ? userIdObj.toString() : null;

            for (ShowDTO s : shows) {
                Date showDate = s.showDate != null
                        ? Date.from(s.showDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        : null;

                show newShow = new show(
                        created.id,
                        s.name,
                        s.description,
                        s.singer,
                        showDate
                );

                newShow.setAreas(buildAreasFromShowDTO(s));
                newShow.setSeatedPrice(s.seatedPrice);
                newShow.setStandingPrice(s.standingPrice);

                try {
                    eventService.addShowToEvent(created.id, newShow, memberId);
                } catch (Exception ignored) {
                }
            }

            creationView.setSessionAttribute("eventId", created.id.toString());
            creationView.showSuccess("Event \"" + created.name + "\" created with " + shows.size() + " show(s)!");
            creationView.navigateToCompany();

        } catch (RuntimeException ex) {
            creationView.showError(ex.getMessage());
        }
    }

    public void saveCreationPolicies(
            SellingType sellingType,
            Integer minAge,
            Integer minTickets,
            Integer maxTickets,
            Double percentage,
            String couponCode,
            Double couponPercentage,
            LocalDate couponExpiryDate) {

        Object eventIdObj = creationView.getSessionAttribute("eventId");
        Object companyIdObj = creationView.getSessionAttribute("managingCompanyId");

        if (eventIdObj == null) {
            creationView.showWarning("No event selected — create an event first");
            return;
        }

        if (companyIdObj == null) {
            creationView.showWarning("No company selected — navigate from your company page");
            return;
        }

        UUID eventId = UUID.fromString(eventIdObj.toString());
        UUID companyId = UUID.fromString(companyIdObj.toString());

        savePolicyInternal(
                null,
                eventId,
                companyId,
                "Selling",
                sellingType,
                minAge,
                minTickets,
                maxTickets,
                percentage,
                couponCode,
                couponPercentage,
                couponExpiryDate
        );

        creationView.showSuccess("Policies saved for event " + eventId);
    }

    // ============================================================
    // EVENT DETAILS PAGE LOGIC
    // ============================================================

    public void loadEventDetailsPageData() {
        Object eventIdObj = detailsView.getSessionAttribute("eventId");
        Object userIdObj = detailsView.getSessionAttribute("userId");

        if (userIdObj != null) {
            try {
                cachedUserId = UUID.fromString(userIdObj.toString());
            } catch (Exception ignored) {
                cachedUserId = UUID.randomUUID();
            }
        }

        if (eventIdObj == null) {
            detailsView.showError("No event selected.");
            return;
        }

        try {
            UUID eventId = UUID.fromString(eventIdObj.toString());
            cachedEventId = eventId;

            cachedEvent = eventService.getEventDetails(eventId);
            cachedShows = new ArrayList<>(eventService.getShowsForEvent(eventId));

            detailsView.renderEventDetails(cachedEvent, cachedShows);

        } catch (RuntimeException ex) {
            detailsView.showError("Could not load event: " + ex.getMessage());
        }
    }

    public boolean isManagerOrOwner() {
        Object companyIdObj = detailsView.getSessionAttribute("managingCompanyId");

        if (companyIdObj == null || cachedEvent == null) {
            return false;
        }

        try {
            UUID companyId = UUID.fromString(companyIdObj.toString());
            return companyId.equals(cachedEvent.getCompanyId());
        } catch (Exception e) {
            return false;
        }
    }

    public String getManagerId() {
        Object obj = detailsView.getSessionAttribute("userId");
        return obj != null ? obj.toString() : null;
    }

    public void saveEventDetails(
            String name,
            String venue,
            show_type type,
            LocalDate start,
            LocalDate end,
            String description) {

        if (cachedEventId == null) {
            detailsView.showError("No event loaded");
            return;
        }

        if (name == null || name.isBlank()) {
            detailsView.showError("Name is required");
            return;
        }

        String managerId = getManagerId();

        if (managerId == null) {
            detailsView.showError("Session expired — please log in again");
            return;
        }

        try {
            eventService.editEventName(cachedEventId, name.trim(), managerId);
            eventService.editEventVenue(cachedEventId, venue != null ? venue.trim() : "", managerId);

            if (type != null) {
                eventService.editEventType(cachedEventId, type, managerId);
            }

            eventService.editEventDescription(
                    cachedEventId,
                    description != null ? description.trim() : "",
                    managerId
            );

            Date startDate = start != null
                    ? Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            Date endDate = end != null
                    ? Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            eventService.editEventDates(cachedEventId, startDate, endDate, managerId);

            detailsView.showSuccess("Event details saved");
            detailsView.reloadPage();

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    public void addShow(
            String name,
            String singer,
            String description,
            LocalDate showDateLocal,
            Integer standingCapacity,
            Integer numBlocks,
            Integer rowsPerBlock,
            Integer seatsPerRow,
            Double standingPriceValue,
            Double seatedPriceValue) {

        if (cachedEventId == null) {
            detailsView.showError("No event loaded");
            return;
        }

        if (name == null || name.isBlank()) {
            detailsView.showError("Show name is required");
            return;
        }

        String managerId = getManagerId();

        if (managerId == null) {
            detailsView.showError("Session expired — please log in again");
            return;
        }

        int standingCap = standingCapacity != null ? standingCapacity : 0;
        int blocks = numBlocks != null ? numBlocks : 0;
        int rows = rowsPerBlock != null ? rowsPerBlock : 0;
        int seats = seatsPerRow != null ? seatsPerRow : 0;

        if (standingCap == 0 && blocks == 0) {
            detailsView.showError("Add at least one area: standing or seated");
            return;
        }

        boolean hasSeated = blocks > 0 || rows > 0 || seats > 0;

        if (hasSeated && (blocks <= 0 || rows <= 0 || seats <= 0)) {
            detailsView.showError("Seated area requires blocks, rows per block, and seats per row all > 0");
            return;
        }

        java.math.BigDecimal standingPrice = standingPriceValue != null
                ? java.math.BigDecimal.valueOf(standingPriceValue)
                : new java.math.BigDecimal("30.00");

        java.math.BigDecimal seatedPrice = seatedPriceValue != null
                ? java.math.BigDecimal.valueOf(seatedPriceValue)
                : new java.math.BigDecimal("50.00");

        Date showDate = showDateLocal != null
                ? Date.from(showDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;

        try {
            show newShow = new show(
                    cachedEventId,
                    name.trim(),
                    description != null ? description.trim() : "",
                    singer != null ? singer.trim() : "",
                    showDate
            );

            newShow.setAreas(buildShowAreas(standingCap, blocks, rows, seats));
            newShow.setStandingPrice(standingPrice);
            newShow.setSeatedPrice(seatedPrice);

            eventService.addShowToEvent(cachedEventId, newShow, managerId);
            cachedShows.add(newShow);

            detailsView.showSuccess("Show added!");
            detailsView.reloadPage();

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    public void updateShow(
            show existing,
            String name,
            String singer,
            String description,
            LocalDate showDateLocal,
            Double standingPriceValue,
            Double seatedPriceValue) {

        if (existing == null || existing.getShowid() == null) {
            detailsView.showError("No show selected");
            return;
        }

        if (name == null || name.isBlank()) {
            detailsView.showError("Show name is required");
            return;
        }

        String managerId = getManagerId();

        if (managerId == null) {
            detailsView.showError("Session expired — please log in again");
            return;
        }

        java.math.BigDecimal standingPrice = standingPriceValue != null
                ? java.math.BigDecimal.valueOf(standingPriceValue)
                : new java.math.BigDecimal("30.00");

        java.math.BigDecimal seatedPrice = seatedPriceValue != null
                ? java.math.BigDecimal.valueOf(seatedPriceValue)
                : new java.math.BigDecimal("50.00");

        Date showDate = showDateLocal != null
                ? Date.from(showDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;

        try {
            eventService.updateShowBasicFields(
                    cachedEventId,
                    existing.getShowid(),
                    name.trim(),
                    description != null ? description.trim() : "",
                    singer != null ? singer.trim() : "",
                    showDate,
                    seatedPrice,
                    standingPrice,
                    managerId
            );

            existing.setName(name.trim());
            existing.setDescription(description != null ? description.trim() : "");
            existing.setSinger(singer != null ? singer.trim() : "");
            existing.setShowDate(showDate);
            existing.setSeatedPrice(seatedPrice);
            existing.setStandingPrice(standingPrice);

            detailsView.showSuccess("Show updated!");
            detailsView.reloadPage();

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    public int countTicketsForShow(show s) {
        if (s == null || s.getShowid() == null) {
            return 0;
        }

        try {
            return eventService.countTicketsForShow(s.getShowid());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public int countTotalTicketsForEvent() {
        int totalTickets = 0;

        for (show s : cachedShows) {
            totalTickets += countTicketsForShow(s);
        }

        return totalTickets;
    }

    public void deleteShow(show s) {
        if (s == null) {
            detailsView.showError("No show selected");
            return;
        }

        String managerId = getManagerId();

        if (managerId == null) {
            detailsView.showError("Session expired");
            return;
        }

        try {
            eventService.removeShowFromEvent(cachedEventId, s, managerId);
            cachedShows.remove(s);

            detailsView.showSuccess("Show deleted");
            detailsView.reloadPage();

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    public void deleteEvent() {
        String managerId = getManagerId();

        if (managerId == null) {
            detailsView.showError("Session expired");
            return;
        }

        try {
            eventService.deleteEvent(cachedEventId, managerId);

            detailsView.showSuccess("Event deleted");
            detailsView.navigateToCompany();

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    public List<Policy> getPolicies() {
        if (cachedEventId == null) {
            return List.of();
        }

        return policyRepo.findByEventId(cachedEventId);
    }

    public void deletePolicy(Policy policy) {
        if (policy == null) {
            return;
        }

        try {
            policyRepo.deleteByPolicyId(policy.getPolicyId());
            detailsView.showSuccess("Policy removed");
            detailsView.refreshPolicies();

        } catch (RuntimeException ex) {
            detailsView.showError("Could not remove: " + ex.getMessage());
        }
    }

    public void saveEventDetailsPolicy(
            Policy existing,
            String policyType,
            SellingType sellingType,
            Integer minAge,
            Integer minTickets,
            Integer maxTickets,
            Double percentage,
            String couponCode,
            Double couponPercentage,
            LocalDate couponExpiryDate) {

        if (cachedEventId == null) {
            detailsView.showError("No event loaded");
            return;
        }

        Object companyIdObj = detailsView.getSessionAttribute("managingCompanyId");

        if (companyIdObj == null) {
            detailsView.showError("No company session — please log in again");
            return;
        }

        UUID companyId = UUID.fromString(companyIdObj.toString());

        savePolicyInternal(
                existing,
                cachedEventId,
                companyId,
                policyType,
                sellingType,
                minAge,
                minTickets,
                maxTickets,
                percentage,
                couponCode,
                couponPercentage,
                couponExpiryDate
        );

        detailsView.refreshPolicies();
    }

    public show loadShowFully(show s) {
        if (s == null || s.getShowid() == null || cachedEventId == null) {
            return s;
        }

        return eventService.loadShowFully(cachedEventId, s.getShowid());
    }

    public java.util.Map<Long, Boolean> getSeatAvailability(show s) {
        if (s == null || s.getShowid() == null) {
            return java.util.Map.of();
        }

        return ticketService.getSeatAvailability(s.getShowid());
    }

    public UUID getCachedEventId() {
        return cachedEventId;
    }

    public UUID getCachedUserId() {
        return cachedUserId;
    }

    public Event getCachedEvent() {
        return cachedEvent;
    }

    public List<show> getCachedShows() {
        return cachedShows;
    }

    // ============================================================
    // SHARED POLICY HELPER
    // ============================================================

    private void savePolicyInternal(
            Policy existing,
            UUID eventId,
            UUID companyId,
            String policyType,
            SellingType sellingType,
            Integer minAge,
            Integer minTickets,
            Integer maxTickets,
            Double percentage,
            String couponCode,
            Double couponPercentage,
            LocalDate couponExpiryDate) {

        if (policyType == null || policyType.isBlank()) {
            detailsView.showError("Please select a policy type");
            return;
        }

        try {
            if (existing != null) {
                policyRepo.deleteByPolicyId(existing.getPolicyId());
            }

            switch (policyType) {
                case "Selling" -> {
                    SellingType st = sellingType != null
                            ? sellingType
                            : SellingType.REGULAR;

                    policyRepo.savePolicy(new SellingPolicy(
                            Math.abs(UUID.randomUUID().hashCode()),
                            st.name() + " selling policy",
                            st,
                            eventId,
                            companyId
                    ));
                }

                case "Purchase" -> {
                    if (minAge == null && minTickets == null && maxTickets == null) {
                        detailsView.showError("At least one purchase restriction is required");
                        return;
                    }

                    PurchasePolicy pp = new PurchasePolicy(
                            Math.abs(UUID.randomUUID().hashCode()),
                            "Purchase restrictions",
                            eventId,
                            companyId
                    );

                    if (minAge != null && minAge >= 0) {
                        pp.addRule(new MinAgeRule(minAge));
                    }

                    if (minTickets != null && minTickets > 0) {
                        pp.addRule(new MinTicketsRule(minTickets));
                    }

                    if (maxTickets != null && maxTickets > 0) {
                        pp.addRule(new MaxTicketsRule(maxTickets));
                    }

                    policyRepo.savePolicy(pp);
                }

                case "Percentage Discount" -> {
                    if (percentage == null || percentage <= 0) {
                        detailsView.showError("Percentage must be greater than 0");
                        return;
                    }

                    DiscountPolicy dp = new DiscountPolicy(
                            Math.abs(UUID.randomUUID().hashCode()),
                            percentage + "% discount",
                            eventId,
                            companyId
                    );

                    dp.addRule(new PercentageDiscountRule(percentage, percentage + "% discount"));
                    policyRepo.savePolicy(dp);
                }

                case "Coupon Discount" -> {
                    if (couponCode == null || couponCode.isBlank()) {
                        detailsView.showError("Coupon code is required");
                        return;
                    }

                    if (couponPercentage == null || couponPercentage <= 0) {
                        detailsView.showError("Percentage must be greater than 0");
                        return;
                    }

                    java.time.LocalDateTime expiry = couponExpiryDate != null
                            ? couponExpiryDate.atTime(23, 59, 59)
                            : null;

                    DiscountPolicy dp = new DiscountPolicy(
                            Math.abs(UUID.randomUUID().hashCode()),
                            "Coupon: " + couponCode.trim().toUpperCase(),
                            eventId,
                            companyId
                    );

                    dp.addRule(new CouponDiscountRule(
                            couponPercentage,
                            couponCode.trim().toUpperCase(),
                            expiry
                    ));

                    policyRepo.savePolicy(dp);
                }

                default -> {
                    detailsView.showError("Unknown policy type");
                    return;
                }
            }

            detailsView.showSuccess(existing == null ? "Policy added" : "Policy updated");

        } catch (RuntimeException ex) {
            detailsView.showError(ex.getMessage());
        }
    }

    // ============================================================
    // SHARED AREA HELPERS
    // ============================================================

    private static List<Area> buildAreasFromShowDTO(ShowDTO s) {
        return buildShowAreas(
                s.standingCapacity,
                s.numBlocks,
                s.rowsPerBlock,
                s.seatsPerRow
        );
    }

    private static List<Area> buildShowAreas(
            int standingCap,
            int numBlocks,
            int rowsPerBlock,
            int seatsPerRow) {

        List<Area> areas = new ArrayList<>();

        if (standingCap > 0) {
            areas.add(new StandingArea("Standing Area", standingCap));
        }

        if (numBlocks > 0 && rowsPerBlock > 0 && seatsPerRow > 0) {
            SeatedArea seated = new SeatedArea("Seated Area", numBlocks);
            List<Block> blocks = new ArrayList<>();

            long idSeq = 1;

            for (int b = 0; b < numBlocks; b++) {
                String blockLabel = String.valueOf((char) ('A' + b));
                Block block = new Block(idSeq++, blockLabel, rowsPerBlock, seated);

                List<Row> rows = new ArrayList<>();

                for (int r = 0; r < rowsPerBlock; r++) {
                    Row row = new Row(idSeq++, String.valueOf(r + 1), seatsPerRow, block);
                    List<Seat> seats = new ArrayList<>();

                    for (int s = 1; s <= seatsPerRow; s++) {
                        seats.add(new Seat(idSeq++, String.valueOf(s), row));
                    }

                    row.setSeats(seats);
                    rows.add(row);
                }

                block.setRows(rows);
                blocks.add(block);
            }

            seated.setBlocks(blocks);
            areas.add(seated);
        }

        return areas;
    }

    // ============================================================
    // VIEW CONTRACT
    // ============================================================

    public interface CreationView {
    Object getSessionAttribute(String key);

    void setSessionAttribute(String key, Object value);

    void showSuccess(String message);

    void showError(String message);

    void showWarning(String message);

    void navigateToCompany();
}

public interface DetailsView {
    Object getSessionAttribute(String key);

    void showSuccess(String message);

    void showError(String message);

    void showWarning(String message);

    void renderEventDetails(Event event, List<show> shows);

    void reloadPage();

    void navigateToCompany();

    void refreshPolicies();
}
}