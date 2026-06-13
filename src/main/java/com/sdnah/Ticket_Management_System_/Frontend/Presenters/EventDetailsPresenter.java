package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery.LotteryStatus;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link com.sdnah.Ticket_Management_System_.Frontend.EventDetailsView}.
 * Owns every service interaction: event CRUD, show CRUD, policies, lottery, seat reservation,
 * and winner/loser notifications.
 */
@Component
@UIScope
public class EventDetailsPresenter {

    // ── Services ─────────────────────────────────────────────────────────────
    private final EventService       eventService;
    private final TicketService      ticketService;
    private final PolicyRepository   policyRepo;
    private final ActiveOrderService orderService;
    private final UserService        userService;
    private final LotteryService     lotteryService;
    private final NotificationService notificationService;

    // ── Cached state ──────────────────────────────────────────────────────────
    private UUID        cachedEventId;
    private Event       cachedEvent;
    private List<show>  cachedShows = new ArrayList<>();

    // ── View ──────────────────────────────────────────────────────────────────
    private View view;

    public EventDetailsPresenter(
            EventService eventService,
            TicketService ticketService,
            PolicyRepository policyRepo,
            ActiveOrderService orderService,
            UserService userService,
            LotteryService lotteryService,
            NotificationService notificationService) {
        this.eventService        = eventService;
        this.ticketService       = ticketService;
        this.policyRepo          = policyRepo;
        this.orderService        = orderService;
        this.userService         = userService;
        this.lotteryService      = lotteryService;
        this.notificationService = notificationService;
    }

    public void setView(View view) {
        this.view = view;
    }

    // =========================================================================
    // Load data
    // =========================================================================

    public void loadData() {
        Object eventIdObj = view.getSessionAttribute("eventId");
        if (eventIdObj == null) {
            view.showError("No event selected.");
            return;
        }
        try {
            cachedEventId = UUID.fromString(eventIdObj.toString());
            cachedEvent   = eventService.getEventDetails(cachedEventId);
            cachedShows   = new ArrayList<>(eventService.getShowsForEvent(cachedEventId));
            view.renderEvent(cachedEvent, cachedShows);
        } catch (RuntimeException ex) {
            view.showError("Could not load event: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Auth helpers
    // =========================================================================

    /** Checks live token → member roles — does NOT rely on session attributes. */
    public boolean isManagerOrOwner() {
        if (cachedEvent == null) return false;
        Object tokenObj = view.getSessionAttribute("token");
        if (tokenObj == null) return false;
        try {
            Member member = userService.getMemberByToken(tokenObj.toString());
            UUID   cid    = cachedEvent.getCompanyId();
            return member.isOwnerInCompany(cid) || member.isManagerInCompany(cid);
        } catch (Exception e) {
            return false;
        }
    }

    public String getToken() {
        Object obj = view.getSessionAttribute("token");
        return obj != null ? obj.toString() : null;
    }

    public String getManagerId() {
        Object obj = view.getSessionAttribute("userId");
        return obj != null ? obj.toString() : null;
    }

    public UUID getCachedEventId()  { return cachedEventId; }
    public Event getCachedEvent()   { return cachedEvent;   }
    public List<show> getCachedShows() { return cachedShows; }

    // =========================================================================
    // Event CRUD
    // =========================================================================

    public void saveEventDetails(String name, String venue, show_type type,
                                  LocalDate start, LocalDate end, String description) {
        if (cachedEventId == null) { view.showError("No event loaded"); return; }
        if (name == null || name.isBlank()) { view.showError("Name is required"); return; }
        String mgr = getManagerId();
        if (mgr == null) { view.showError("Session expired — please log in again"); return; }
        try {
            eventService.editEventName(cachedEventId, name.trim(), mgr);
            eventService.editEventVenue(cachedEventId, venue != null ? venue.trim() : "", mgr);
            if (type != null) eventService.editEventType(cachedEventId, type, mgr);
            eventService.editEventDescription(cachedEventId,
                    description != null ? description.trim() : "", mgr);
            Date startDate = toDate(start);
            Date endDate   = toDate(end);
            eventService.editEventDates(cachedEventId, startDate, endDate, mgr);
            view.showSuccess("Event details saved");
            view.reloadPage();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    public void deleteEvent() {
        String mgr = getManagerId();
        if (mgr == null) { view.showError("Session expired"); return; }
        try {
            eventService.deleteEvent(cachedEventId, mgr);
            view.showSuccess("Event deleted");
            view.navigateToCompany();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    // =========================================================================
    // Show CRUD
    // =========================================================================

    public void addShow(String name, String singer, String description,
                        LocalDate showDateLocal, int standingCap,
                        int numBlocks, int rowsPerBlock, int seatsPerRow,
                        BigDecimal standingPrice, BigDecimal seatedPrice) {
        if (cachedEventId == null) { view.showError("No event loaded"); return; }
        if (name == null || name.isBlank()) { view.showError("Show name is required"); return; }
        if (standingCap == 0 && numBlocks == 0) {
            view.showError("Add at least one area (standing or seated)"); return;
        }
        if (numBlocks > 0 && (rowsPerBlock <= 0 || seatsPerRow <= 0)) {
            view.showError("Seated area requires blocks, rows per block, and seats per row all > 0"); return;
        }
        String mgr = getManagerId();
        if (mgr == null) { view.showError("Session expired"); return; }
        try {
            show s = new show(cachedEventId, name.trim(),
                    description != null ? description.trim() : "",
                    singer != null ? singer.trim() : "",
                    toDate(showDateLocal));
            s.setAreas(buildShowAreas(standingCap, numBlocks, rowsPerBlock, seatsPerRow));
            s.setStandingPrice(standingPrice);
            s.setSeatedPrice(seatedPrice);
            eventService.addShowToEvent(cachedEventId, s, mgr);
            cachedShows.add(s);
            view.showSuccess("Show added!");
            view.reloadPage();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    public void updateShow(show existing, String name, String singer, String description,
                           LocalDate showDateLocal, BigDecimal standingPrice, BigDecimal seatedPrice) {
        if (existing == null) { view.showError("No show selected"); return; }
        if (name == null || name.isBlank()) { view.showError("Show name is required"); return; }
        String mgr = getManagerId();
        if (mgr == null) { view.showError("Session expired"); return; }
        try {
            eventService.updateShowBasicFields(cachedEventId, existing.getShowid(),
                    name.trim(),
                    description != null ? description.trim() : "",
                    singer != null ? singer.trim() : "",
                    toDate(showDateLocal), seatedPrice, standingPrice, mgr);
            existing.setName(name.trim());
            existing.setDescription(description != null ? description.trim() : "");
            existing.setSinger(singer != null ? singer.trim() : "");
            existing.setShowDate(toDate(showDateLocal));
            existing.setSeatedPrice(seatedPrice);
            existing.setStandingPrice(standingPrice);
            view.showSuccess("Show updated!");
            view.reloadPage();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    public void deleteShow(show s) {
        String mgr = getManagerId();
        if (mgr == null) { view.showError("Session expired"); return; }
        try {
            eventService.removeShowFromEvent(cachedEventId, s, mgr);
            cachedShows.remove(s);
            view.showSuccess("Show deleted");
            view.reloadPage();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    public int countTicketsForShow(UUID showId) {
        try { return eventService.countTicketsForShow(showId); }
        catch (Exception e) { return 0; }
    }

    public int countTotalTickets() {
        return cachedShows.stream().mapToInt(s -> countTicketsForShow(s.getShowid())).sum();
    }

    // =========================================================================
    // Shows – seat helpers
    // =========================================================================

    public show loadShowFully(UUID showId) {
        try { return eventService.loadShowFully(cachedEventId, showId); }
        catch (Exception e) { return null; }
    }

    public Map<Long, Boolean> getSeatAvailability(UUID showId) {
        try { return ticketService.getSeatAvailability(showId); }
        catch (Exception e) { return Map.of(); }
    }

    public int getStandingAvailableCount(UUID showId, UUID areaId, int max) {
        try { return ticketService.getStandingAvailableCount(showId, areaId, max); }
        catch (Exception e) { return 0; }
    }

    public ticket reserveSeat(UUID showId, UUID areaId, Long seatId, UUID userId) {
        return eventService.reserveSeat(cachedEventId, showId, areaId, seatId, userId);
    }

    public ticket reserveStanding(UUID showId, UUID areaId, UUID userId) {
        return eventService.reserveStanding(cachedEventId, showId, areaId, userId);
    }

    // public OrderDTO reserveTickets(String token, List<SeatRequest> requests) {
    //     return orderService.reserveTickets(token, cachedEventId, requests);
    // }
    public OrderDTO reserveTickets(String token, List<SeatRequest> requests) {
        return reserveTickets(token, requests, null);
    }

    public OrderDTO reserveTickets(String token, List<SeatRequest> requests, String accessCode) {
        return orderService.reserveTickets(token, cachedEventId, requests, accessCode);
    }

    // =========================================================================
    // Policies
    // =========================================================================

    public List<Policy> getPolicies() {
        if (cachedEventId == null) return List.of();
        return policyRepo.findByEventId(cachedEventId);
    }

    public void deletePolicy(Policy policy) {
        try {
            policyRepo.deleteByPolicyId(policy.getPolicyId());
            view.showSuccess("Policy removed");
            view.refreshPolicies();
        } catch (RuntimeException ex) { view.showError("Could not remove: " + ex.getMessage()); }
    }

    public void savePolicy(Policy existing, String policyType, SellingType sellingType,
                            Integer minAge, Integer minTickets, Integer maxTickets,
                            Double percentage, String couponCode, Double couponPct,
                            LocalDate couponExpiry) {
        if (cachedEventId == null) { view.showError("No event loaded"); return; }
        Object companyIdObj = view.getSessionAttribute("managingCompanyId");
        if (companyIdObj == null) { view.showError("No company session"); return; }
        UUID companyId = UUID.fromString(companyIdObj.toString());
        try {
            if (existing != null) policyRepo.deleteByPolicyId(existing.getPolicyId());
            switch (policyType) {
                case "Selling" -> {
                    SellingType st = sellingType != null ? sellingType : SellingType.REGULAR;
                    policyRepo.savePolicy(new SellingPolicy(
                            st.name() + " selling policy", st, cachedEventId, companyId));
                }
                case "Purchase" -> {
                    if (minAge == null && minTickets == null && maxTickets == null) {
                        view.showError("At least one purchase restriction is required"); return;
                    }
                    PurchasePolicy pp = new PurchasePolicy(
                            "Purchase restrictions", cachedEventId, companyId);
                    if (minAge != null && minAge >= 0)      pp.addRule(new MinAgeRule(minAge));
                    if (minTickets != null && minTickets > 0) pp.addRule(new MinTicketsRule(minTickets));
                    if (maxTickets != null && maxTickets > 0) pp.addRule(new MaxTicketsRule(maxTickets));
                    policyRepo.savePolicy(pp);
                }
                case "Percentage Discount" -> {
                    if (percentage == null || percentage <= 0) {
                        view.showError("Percentage must be > 0"); return;
                    }
                    DiscountPolicy dp = new DiscountPolicy(
                            percentage + "% discount", cachedEventId, companyId);
                    dp.addRule(new PercentageDiscountRule(percentage, percentage + "% discount"));
                    policyRepo.savePolicy(dp);
                }
                case "Coupon Discount" -> {
                    if (couponCode == null || couponCode.isBlank()) {
                        view.showError("Coupon code is required"); return;
                    }
                    if (couponPct == null || couponPct <= 0) {
                        view.showError("Coupon percentage must be > 0"); return;
                    }
                    LocalDateTime expiry = couponExpiry != null
                            ? couponExpiry.atTime(23, 59, 59) : null;
                    DiscountPolicy dp = new DiscountPolicy(
                            "Coupon: " + couponCode.trim().toUpperCase(), cachedEventId, companyId);
                    dp.addRule(new CouponDiscountRule(couponPct,
                            couponCode.trim().toUpperCase(), expiry));
                    policyRepo.savePolicy(dp);
                }
                default -> { view.showError("Unknown policy type"); return; }
            }
            view.showSuccess(existing == null ? "Policy added" : "Policy updated");
            view.refreshPolicies();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    /**
     * Saves a composite discount policy with an explicit list of rules.
     * Used by the new "Discount" dialog that lets managers combine Percentage,
     * Coupon, Quantity and Date-range rules.
     *
     * @param existing   the policy to replace (null → new policy)
     * @param rules      the discount rules to apply
     * @param isAdditive true → AND (sum discounts), false → OR (best discount)
     */
    public void saveDiscountPolicy(Policy existing, List<DiscountRule> rules, boolean isAdditive) {
        if (cachedEventId == null) { view.showError("No event loaded"); return; }
        Object companyIdObj = view.getSessionAttribute("managingCompanyId");
        if (companyIdObj == null) { view.showError("No company session"); return; }
        if (rules == null || rules.isEmpty()) { view.showError("At least one discount rule is required"); return; }
        UUID companyId = UUID.fromString(companyIdObj.toString());
        try {
            // Remove existing discount policies for this event first
            policyRepo.findByEventId(cachedEventId).stream()
                    .filter(p -> p instanceof DiscountPolicy)
                    .forEach(p -> policyRepo.deleteByPolicyId(p.getPolicyId()));
            if (existing != null) {
                try { policyRepo.deleteByPolicyId(existing.getPolicyId()); } catch (Exception ignored) {}
            }
            DiscountPolicy dp = new DiscountPolicy("Discount policy", cachedEventId, companyId);
            dp.setRules(rules, isAdditive);
            policyRepo.savePolicy(dp);
            view.showSuccess("Discount policy saved");
            view.refreshPolicies();
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    // =========================================================================
    // Lottery
    // =========================================================================

    public List<LotteryDTO> getLotteriesByEvent() {
        if (cachedEventId == null) return List.of();
        return lotteryService.getLotteriesByEvent(cachedEventId);
    }

    public LotteryDTO createLottery(LocalDateTime deadline, LocalDateTime drawTime) {
        String token = getToken();
        if (token == null) { view.showError("Session expired"); return null; }
        try {
            LotteryDTO dto = lotteryService.createLottery(
                    token, cachedEventId, cachedEvent.getCompanyId(), deadline, drawTime);
            view.showSuccess("Lottery created!");
            return dto;
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); return null; }
    }

    public void registerToLottery(UUID lotteryId) {
        String token = getToken();
        if (token == null) { view.showError("Please log in to register"); return; }
        try {
            lotteryService.registerToLottery(token, lotteryId);
            view.showSuccess("You're registered! Good luck 🍀");
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); }
    }

    public List<LotteryEntryDTO> getEntriesByLottery(UUID lotteryId) {
        return lotteryService.getEntriesByLottery(lotteryId);
    }

    public List<LotteryEntryDTO> drawLotteryWithNotifications(UUID lotteryId, int winnerCount) {
        return drawLotteryWithNotifications(lotteryId, winnerCount, java.time.Duration.ofHours(24));
    }
    /**
     * Draws the lottery using max-show-capacity as winner count,
     * then sends win/loss notifications to ALL participants.
     * Returns the drawn winners list, or empty if draw failed.
     */
    public List<LotteryEntryDTO> drawLotteryWithNotifications(UUID lotteryId, int winnerCount,java.time.Duration window) {
        String token = getToken();
        if (token == null) { view.showError("Session expired"); return List.of(); }
        try {
            List<LotteryEntryDTO> allBeforeDraw = lotteryService.getEntriesByLottery(lotteryId);
            int participants = allBeforeDraw.size();
            if (participants == 0) { view.showError("No participants registered yet"); return List.of(); }

            int drawCount = Math.min(Math.max(winnerCount, 1), participants);

            List<LotteryEntryDTO> winners = lotteryService.drawLottery(token, lotteryId, drawCount, window);

            // Notify all participants
            java.util.Set<String> winnerIds = winners.stream()
                    .map(LotteryEntryDTO::getMemberId).collect(Collectors.toSet());

            List<LotteryEntryDTO> allEntries;
            try { allEntries = lotteryService.getEntriesByLottery(lotteryId); }
            catch (Exception e) { allEntries = allBeforeDraw; }

            String eventName = cachedEvent != null ? cachedEvent.getName() : "the event";
            int notifiedW = 0, notifiedL = 0;
            for (LotteryEntryDTO entry : allEntries) {
                try {
                    Member m = userService.getMemberById(entry.getMemberId());
                    if (winnerIds.contains(entry.getMemberId())) {
                        String code = winners.stream()
                                .filter(w -> w.getMemberId().equals(entry.getMemberId()))
                                .map(LotteryEntryDTO::getAccessCode).findFirst().orElse(null);
                        notificationService.notifyLotteryWin(m.getUsername(), eventName, code);
                        notifiedW++;
                    } else {
                        notificationService.notifyLotteryLoss(m.getUsername(), eventName);
                        notifiedL++;
                    }
                } catch (Exception ignored) {}
            }
            view.showSuccess("🎉 Draw complete! " + winners.size() + " winner(s) — "
                    + notifiedW + " win + " + notifiedL + " loss notifications sent.");
            return winners;
        } catch (RuntimeException ex) { view.showError(ex.getMessage()); return List.of(); }
    }

    /** Returns true if this event has a LOTTERY selling policy. */
    public boolean isLotteryEvent() {
        if (cachedEventId == null) return false;
        try {
            return policyRepo.findByEventId(cachedEventId).stream()
                    .anyMatch(p -> p instanceof SellingPolicy sp
                            && sp.getType() == SellingType.LOTTERY);
        } catch (Exception e) { return false; }
    }

    /** Returns true if any lottery for this event has been drawn. */
    public boolean hasDrawnLottery() {
        if (cachedEventId == null) return false;
        try {
            return lotteryService.getLotteriesByEvent(cachedEventId).stream()
                    .anyMatch(l -> l.getStatus() == LotteryStatus.DRAWN);
        } catch (Exception e) { return false; }
    }

    /** Resolves a lottery entry's memberId to a username, returns the raw id on failure. */
    public String resolveUsername(String memberId) {
        try { return userService.getMemberById(memberId).getUsername(); }
        catch (Exception e) { return memberId; }
    }

    // =========================================================================
    // Capacity helper
    // =========================================================================

    /**
     * Returns the total capacity of the largest show in the event.
     * Standing capacity + all seats in all seated areas.
     */
    public int computeMaxShowCapacity() {
        if (cachedEventId == null) return 0;
        try {
            int max = 0;
            for (show s : cachedShows) {
                int cap = 0;
                try {
                    show full = eventService.loadShowFully(cachedEventId, s.getShowid());
                    if (full.getAreas() != null) {
                        for (Area a : full.getAreas()) {
                            if (a instanceof StandingArea sa) {
                                cap += sa.getMaxCapacity();
                            } else if (a instanceof SeatedArea sea) {
                                for (Block b : sea.getBlocks())
                                    if (b.getRows() != null)
                                        for (Row r : b.getRows())
                                            cap += r.getSeats() != null ? r.getSeats().size() : 0;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                if (cap > max) max = cap;
            }
            return max;
        } catch (Exception e) { return 0; }
    }

    // =========================================================================
    // View contract
    // =========================================================================

    public interface View {
        Object getSessionAttribute(String key);
        void showSuccess(String message);
        void showError(String message);
        void showWarning(String message);
        void reloadPage();
        void navigateToCompany();
        void refreshPolicies();
        void renderEvent(Event event, List<show> shows);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static Date toDate(LocalDate d) {
        return d != null ? Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
    }

    private static List<Area> buildShowAreas(int standingCap, int numBlocks,
                                              int rowsPerBlock, int seatsPerRow) {
        List<Area> areas = new ArrayList<>();
        if (standingCap > 0) areas.add(new StandingArea("Standing Area", standingCap));
        if (numBlocks > 0 && rowsPerBlock > 0 && seatsPerRow > 0) {
            SeatedArea seated = new SeatedArea("Seated Area", numBlocks);
            List<Block> blocks = new ArrayList<>();
            long seq = 1;
            for (int b = 0; b < numBlocks; b++) {
                Block block = new Block(seq++, String.valueOf((char) ('A' + b)), rowsPerBlock, seated);
                List<Row> rows = new ArrayList<>();
                for (int r = 0; r < rowsPerBlock; r++) {
                    Row row = new Row(seq++, String.valueOf(r + 1), seatsPerRow, block);
                    List<com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat> seats = new ArrayList<>();
                    for (int s = 1; s <= seatsPerRow; s++)
                        seats.add(new com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat(seq++, String.valueOf(s), row));
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
}
