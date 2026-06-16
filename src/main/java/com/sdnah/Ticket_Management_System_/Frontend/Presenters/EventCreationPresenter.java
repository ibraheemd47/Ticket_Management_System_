package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ShowDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link com.sdnah.Ticket_Management_System_.Frontend.EventCreationView}.
 * Owns all business logic for creating an event, its shows, policies, and optional lottery.
 */
@Component
@UIScope
public class EventCreationPresenter {

    // ── Services ────────────────────────────────────────────────────────────
    private final company_managment_serivce companyService;
    private final EventService              eventService;
    private final PolicyRepository          policyRepo;
    private final LotteryService            lotteryService;

    // ── View contract ────────────────────────────────────────────────────────
    private View view;

    public EventCreationPresenter(
            company_managment_serivce companyService,
            EventService eventService,
            PolicyRepository policyRepo,
            LotteryService lotteryService) {
        this.companyService  = companyService;
        this.eventService    = eventService;
        this.policyRepo      = policyRepo;
        this.lotteryService  = lotteryService;
    }

    public void setView(View view) {
        this.view = view;
    }

    // =========================================================================
    // handleCreate — single entry point for the "Create Event" button
    // =========================================================================

    /**
     * Validates all inputs, then:
     *  1. Creates the event
     *  2. Adds every show
     *  3. Saves all policies (selling, purchase, discount)
     *  4. Creates a lottery if {@code isLottery == true}
     */
    public void handleCreate(
            String             name,
            String             venue,
            show_type          type,
            LocalDate          start,
            LocalDate          end,
            List<ShowDTO>      shows,
            // policies
            SellingType        sellingType,
            Integer            minAge,
            Integer            minTickets,
            Integer            maxTickets,
            Double             percentage,
            String             couponCode,
            Double             couponPct,
            LocalDate          couponExpiry,
            // lottery (only when sellingType == LOTTERY)
            LocalDateTime      lotteryDeadline,
            LocalDateTime      lotteryDrawTime) {

        // ── Basic validation ──────────────────────────────────────────────
        if (name == null || name.isBlank()) {
            view.showError("Event name is required");
            return;
        }
        if (venue == null || venue.isBlank()) {
            view.showError("Venue is required");
            return;
        }
        if (type == null) {
            view.showError("Please select an event type");
            return;
        }
        if (start == null) {
            view.showError("Start date is required");
            return;
        }
        if (end == null) {
            view.showError("End date is required");
            return;
        }
        if (end.isBefore(start)) {
            view.showError("End date cannot be before start date");
            return;
        }

        // ── Session ───────────────────────────────────────────────────────
        Object tokenObj     = view.getSessionAttribute("token");
        Object companyIdObj = view.getSessionAttribute("managingCompanyId");
        Object userIdObj    = view.getSessionAttribute("userId");

        if (tokenObj == null) {
            view.showWarning("Not logged in — sign in first");
            return;
        }
        if (companyIdObj == null) {
            view.showWarning("No company selected — navigate from your company page");
            return;
        }

        String token     = tokenObj.toString();
        UUID   companyId = UUID.fromString(companyIdObj.toString());
        String memberId  = userIdObj != null ? userIdObj.toString() : null;

        // ── Lottery validation ────────────────────────────────────────────
        boolean isLottery = sellingType == SellingType.LOTTERY;
        if (isLottery) {
            if (lotteryDeadline == null) {
                view.showError("Lottery registration deadline is required");
                return;
            }
            if (lotteryDrawTime == null) {
                view.showError("Lottery draw time is required");
                return;
            }
            if (!lotteryDrawTime.isAfter(lotteryDeadline)) {
                view.showError("Draw time must be after the registration deadline");
                return;
            }
        }

        try {
            // ── 1. Create event ───────────────────────────────────────────
            EventDto dto = new EventDto();
            dto.name      = name.trim();
            dto.venue     = venue.trim();
            dto.eventType = type;
            dto.startDate = start != null ? start.atStartOfDay() : null;
            dto.endDate   = end   != null ? end.atStartOfDay()   : null;

            EventDto created = companyService.addEvent(token, companyId, dto);

            // ── 2. Add shows ──────────────────────────────────────────────
            for (ShowDTO s : shows) {
                Date showDate = s.showDate != null
                        ? Date.from(s.showDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        : null;
                show newShow = new show(created.id, s.name, s.description, s.singer, showDate);
                newShow.setAreas(buildAreas(s));
                newShow.setSeatedPrice(s.seatedPrice);
                newShow.setStandingPrice(s.standingPrice);
                try {
                    eventService.addShowToEvent(created.id, newShow, memberId);
                } catch (Exception ignored) { /* single show failure shouldn't abort */ }
            }

            // ── 3. Selling policy ──────────────────────────────────────────
            SellingType st = sellingType != null ? sellingType : SellingType.REGULAR;
            policyRepo.savePolicy(new SellingPolicy(
                    st.name() + " selling policy",
                    st, created.id, companyId));

            // ── 4. Purchase policy (optional) ──────────────────────────────
            boolean hasPurchase = (minAge != null && minAge >= 0)
                    || (minTickets != null && minTickets > 0)
                    || (maxTickets != null && maxTickets > 0);
            if (hasPurchase) {
                PurchasePolicy pp = new PurchasePolicy(
                        "Purchase restrictions", created.id, companyId);
                if (minAge != null && minAge >= 0)      pp.addRule(new MinAgeRule(minAge));
                if (minTickets != null && minTickets > 0) pp.addRule(new MinTicketsRule(minTickets));
                if (maxTickets != null && maxTickets > 0) pp.addRule(new MaxTicketsRule(maxTickets));
                policyRepo.savePolicy(pp);
            }

            // ── 5. Percentage discount (optional) ─────────────────────────
            if (percentage != null && percentage > 0) {
                DiscountPolicy dp = new DiscountPolicy(
                        percentage + "% discount", created.id, companyId);
                dp.addRule(new PercentageDiscountRule(percentage, percentage + "% discount"));
                policyRepo.savePolicy(dp);
            }

            // ── 6. Coupon discount (optional) ─────────────────────────────
            if (couponCode != null && !couponCode.isBlank()
                    && couponPct != null && couponPct > 0) {
                LocalDateTime expiry = couponExpiry != null
                        ? couponExpiry.atTime(23, 59, 59) : null;
                DiscountPolicy dp = new DiscountPolicy(
                        "Coupon: " + couponCode.trim().toUpperCase(), created.id, companyId);
                dp.addRule(new CouponDiscountRule(couponPct,
                        couponCode.trim().toUpperCase(), expiry));
                policyRepo.savePolicy(dp);
            }

            // ── 7. Lottery (if LOTTERY selling type) ──────────────────────
            if (isLottery) {
                lotteryService.createLottery(token, created.id, companyId,
                        lotteryDeadline, lotteryDrawTime);
            }

            // ── Done ──────────────────────────────────────────────────────
            view.setSessionAttribute("eventId", created.id.toString());
            view.showSuccess("Event \"" + created.name + "\" created with "
                    + shows.size() + " show(s)!");
            view.navigateToCompany();

        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    // =========================================================================
    // View contract
    // =========================================================================

    public interface View {
        Object getSessionAttribute(String key);
        void   setSessionAttribute(String key, Object value);
        void   showSuccess(String message);
        void   showError(String message);
        void   showWarning(String message);
        void   navigateToCompany();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static List<Area> buildAreas(ShowDTO s) {
        List<Area> areas = new ArrayList<>();

        if (s.standingCapacity > 0)
            areas.add(new StandingArea("Standing Area", s.standingCapacity));

        if (s.numBlocks > 0 && s.rowsPerBlock > 0 && s.seatsPerRow > 0) {
            SeatedArea seated = new SeatedArea("Seated Area", s.numBlocks);
            List<Block> blocks = new ArrayList<>();
            long seq = 1;
            for (int b = 0; b < s.numBlocks; b++) {
                String label = String.valueOf((char) ('A' + b));
                Block block = new Block(seq++, label, s.rowsPerBlock, seated);
                List<Row> rows = new ArrayList<>();
                for (int r = 0; r < s.rowsPerBlock; r++) {
                    Row row = new Row(seq++, String.valueOf(r + 1), s.seatsPerRow, block);
                    List<Seat> seats = new ArrayList<>();
                    for (int seat = 1; seat <= s.seatsPerRow; seat++)
                        seats.add(new Seat(seq++, String.valueOf(seat), row));
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
