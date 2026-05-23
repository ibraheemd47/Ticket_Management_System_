package com.sdnah.Ticket_Management_System_.Frontend;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.imageio.plugins.tiff.TIFFDirectory;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("seat-select")
public class SeatSelectionView extends VerticalLayout {

    // ── Plain data records — no JPA proxy risk in the view layer ─────────────
    record BlockData(long id, String label, List<RowData> rows) {}
    record RowData(long id, String label, List<SeatData> seats) {}
    record SeatData(long id, String label, boolean available) {}
    record AreaInfo(UUID id, String name, boolean isSeated,
                    int standingMax, int standingAvail, List<BlockData> blocks) {}

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy");
    private static final String[] BLOCK_COLORS = {
        "#1565c0", "#283593", "#0277bd", "#00838f", "#2e7d32", "#558b2f", "#6a1b9a"
    };

    // ── Services ──────────────────────────────────────────────────────────────
    private final EventService eventService;
    private final TicketService ticketService;

    // ── Session data ──────────────────────────────────────────────────────────
    private UUID showId;
    private UUID eventId;
    private UUID userId;
    private boolean isMock;

    // ── View data ─────────────────────────────────────────────────────────────
    private List<AreaInfo> areaInfoList = new ArrayList<>();

    // ── Selection state ───────────────────────────────────────────────────────
    private AreaInfo  currentArea;
    private BlockData currentBlock;
    private RowData   currentRow;
    private SeatData  currentSeat;

    // ── Mutable UI containers ─────────────────────────────────────────────────
    private Div stepDiv;
    private Div summaryCard;

    public SeatSelectionView(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "#f4f4f4").set("font-family", "Arial, sans-serif");

        add(buildHeader());

        // ── Read session ──────────────────────────────────────────────────────
        Object showIdObj  = UI.getCurrent().getSession().getAttribute("showId");
        Object eventIdObj = UI.getCurrent().getSession().getAttribute("eventId");
        Object userIdObj  = UI.getCurrent().getSession().getAttribute("userId");

        this.isMock = (showIdObj == null || eventIdObj == null);
        this.userId = userIdObj != null ? safeUuid(userIdObj.toString()) : null;

        show currentShow;

        if (!isMock) {
            try {
                this.showId  = UUID.fromString(showIdObj.toString());
                this.eventId = UUID.fromString(eventIdObj.toString());
                currentShow  = eventService.loadShowFully(eventId, showId);
                Map<Long, Boolean> avail = ticketService.getSeatAvailability(showId);
                if (currentShow.getAreas() != null) {
                    for (Area a : currentShow.getAreas())
                        areaInfoList.add(toAreaInfo(a, avail));
                }
            } catch (Exception ex) {
                add(emptyState("Could not load show: " + ex.getMessage()));
                return;
            }
        } else {
            currentShow = mockShow();
            areaInfoList = mockAreas();
        }

        // ── Content ───────────────────────────────────────────────────────────
        Div content = new Div();
        content.getStyle()
            .set("max-width", "920px").set("margin", "40px auto")
            .set("width", "100%").set("display", "flex")
            .set("flex-direction", "column").set("gap", "24px");

        content.add(buildShowCard(currentShow));

        Div mainCard = card();
        H2 heading = new H2("Select Your Seat");
        heading.getStyle().set("margin", "0 0 20px 0").set("font-size", "20px").set("color", "#111");
        mainCard.add(heading);

        stepDiv = new Div();
        stepDiv.setWidthFull();
        summaryCard = buildEmptySummary();

        if (areaInfoList.isEmpty()) {
            Paragraph empty = new Paragraph("No seating areas configured for this show.");
            empty.getStyle().set("color", "#888");
            mainCard.add(empty);
        } else if (areaInfoList.size() == 1) {
            mainCard.add(stepDiv);
            showArea(areaInfoList.get(0));
        } else {
            mainCard.add(buildAreaTabs(), stepDiv);
        }

        content.add(mainCard, summaryCard);
        add(content);
    }

    // ── Area tabs ─────────────────────────────────────────────────────────────

    private Div buildAreaTabs() {
        Div tabs = new Div();
        tabs.getStyle()
            .set("display", "flex").set("gap", "8px")
            .set("margin-bottom", "20px").set("flex-wrap", "wrap");
        for (AreaInfo ai : areaInfoList) {
            Button btn = new Button(ai.name(), e -> showArea(ai));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn.getStyle().set("border", "2px solid #026cdf").set("border-radius", "8px");
            tabs.add(btn);
        }
        return tabs;
    }

    private void showArea(AreaInfo ai) {
        currentArea = ai;
        currentBlock = null;
        currentRow = null;
        currentSeat = null;
        clearSummary();
        stepDiv.removeAll();
        if (ai.isSeated()) {
            stepDiv.add(buildBlockGrid(ai));
        } else {
            stepDiv.add(buildStandingPanel(ai));
        }
    }

    // ── Step 1: Block grid ────────────────────────────────────────────────────

    private Div buildBlockGrid(AreaInfo ai) {
        Div wrapper = new Div();
        wrapper.add(stepLabel("Select a Block"));

        Div grid = new Div();
        grid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fill, minmax(150px, 1fr))")
            .set("gap", "12px");

        if (ai.blocks().isEmpty()) {
            grid.add(new Span("No blocks configured for this area."));
        } else {
            for (int i = 0; i < ai.blocks().size(); i++) {
                BlockData block = ai.blocks().get(i);
                String color = BLOCK_COLORS[i % BLOCK_COLORS.length];

                long total = block.rows().stream().mapToLong(r -> r.seats().size()).sum();
                long avail = block.rows().stream()
                    .flatMap(r -> r.seats().stream())
                    .filter(SeatData::available).count();

                Div blockCard = new Div();
                blockCard.getStyle()
                    .set("background", color).set("color", "white")
                    .set("border-radius", "12px").set("padding", "20px 12px")
                    .set("text-align", "center").set("cursor", "pointer")
                    .set("font-weight", "700").set("font-size", "16px").set("user-select", "none");

                Span nameSpan = new Span("Block " + block.label());
                Span availSpan = new Span(avail + "/" + total + " seats");
                availSpan.getStyle()
                    .set("font-size", "12px").set("opacity", "0.85")
                    .set("display", "block").set("margin-top", "4px").set("font-weight", "400");
                blockCard.add(nameSpan, availSpan);

                final BlockData b = block;
                blockCard.addClickListener(e -> {
                    currentBlock = b;
                    stepDiv.removeAll();
                    stepDiv.add(buildRowList(b));
                });
                grid.add(blockCard);
            }
        }
        wrapper.add(grid);
        return wrapper;
    }

    // ── Step 2: Row list ──────────────────────────────────────────────────────

    private Div buildRowList(BlockData block) {
        Div wrapper = new Div();
        wrapper.add(backBtn("← Blocks", () -> showArea(currentArea)));
        wrapper.add(stepLabel("Block " + block.label() + " — Select a Row"));

        Div rowFlex = new Div();
        rowFlex.getStyle()
            .set("display", "flex").set("flex-wrap", "wrap")
            .set("gap", "8px").set("margin-top", "12px");

        for (RowData row : block.rows()) {
            long avail = row.seats().stream().filter(SeatData::available).count();
            Button btn = new Button("Row " + row.label() + "  (" + avail + " free)");
            if (avail > 0) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                final RowData r = row;
                btn.addClickListener(e -> {
                    currentRow = r;
                    stepDiv.removeAll();
                    stepDiv.add(buildSeatGrid(r));
                });
            } else {
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                btn.setEnabled(false);
            }
            rowFlex.add(btn);
        }
        wrapper.add(rowFlex);
        return wrapper;
    }

    // ── Step 3: Seat grid ─────────────────────────────────────────────────────

    private Div buildSeatGrid(RowData row) {
        Div wrapper = new Div();
        wrapper.add(backBtn("← Rows", () -> {
            stepDiv.removeAll();
            stepDiv.add(buildRowList(currentBlock));
        }));
        wrapper.add(stepLabel(
            "Block " + currentBlock.label() + " › Row " + row.label() + " — Select a Seat"));

        Div legend = new Div();
        legend.getStyle()
            .set("display", "flex").set("gap", "16px")
            .set("margin", "8px 0 16px 0").set("font-size", "13px").set("align-items", "center");
        legend.add(legendDot("#4caf50", "Available"), legendDot("#ef5350", "Taken"), legendDot("#026cdf", "Selected"));
        wrapper.add(legend);

        Set<Div> availableCircles = new LinkedHashSet<>();
        Div seatFlex = new Div();
        seatFlex.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "8px");

        for (SeatData seat : row.seats()) {
            Div circle = new Div();
            circle.getStyle()
                .set("width", "44px").set("height", "44px")
                .set("border-radius", "50%").set("display", "flex")
                .set("align-items", "center").set("justify-content", "center")
                .set("font-size", "12px").set("font-weight", "700").set("color", "white")
                .set("background", seat.available() ? "#4caf50" : "#ef5350")
                .set("cursor", seat.available() ? "pointer" : "default")
                .set("user-select", "none");
            circle.add(new Span(seat.label()));

            if (seat.available()) {
                availableCircles.add(circle);
                final SeatData s = seat;
                circle.addClickListener(e -> {
                    availableCircles.forEach(c ->
                        c.getStyle().set("background", "#4caf50").remove("transform"));
                    circle.getStyle().set("background", "#026cdf").set("transform", "scale(1.15)");
                    currentSeat = s;
                    showSummary(s);
                });
            }
            seatFlex.add(circle);
        }
        wrapper.add(seatFlex);
        return wrapper;
    }

    // ── Standing panel ────────────────────────────────────────────────────────

    private Div buildStandingPanel(AreaInfo ai) {
        Div panel = new Div();
        panel.getStyle()
            .set("background", "#f0f4ff").set("border-radius", "12px")
            .set("padding", "32px").set("text-align", "center");

        H3 title = new H3("General Admission — " + ai.name());
        title.getStyle().set("margin", "0 0 12px 0");

        boolean hasSpots = ai.standingAvail() > 0;
        Span capacity = new Span(ai.standingAvail() + " / " + ai.standingMax() + " spots available");
        capacity.getStyle()
            .set("font-size", "20px").set("font-weight", "700").set("display", "block")
            .set("color", hasSpots ? "#2e7d32" : "#c62828").set("margin-bottom", "16px");

        Paragraph priceNote = new Paragraph("Price: $30.00 per ticket");
        priceNote.getStyle().set("color", "#555").set("margin-bottom", "20px");

        Button btn = new Button("Reserve Standing Ticket", e -> handleStandingReserve(ai));
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.getStyle().set("background", "#026cdf").set("font-weight", "700").set("font-size", "15px");
        btn.setEnabled(hasSpots);

        panel.add(title, capacity, priceNote, btn);
        return panel;
    }

    // ── Summary card ──────────────────────────────────────────────────────────

    private Div buildEmptySummary() {
        Div c = card();
        c.getStyle().set("text-align", "center").set("color", "#aaa");
        c.add(new Span("Select a seat to see ticket details"));
        return c;
    }

    private void showSummary(SeatData seat) {
        summaryCard.removeAll();
        summaryCard.getStyle().remove("text-align").remove("color");

        H2 title = new H2("Ticket Summary");
        title.getStyle().set("margin", "0 0 16px 0").set("font-size", "20px").set("color", "#111");

        Div info = new Div();
        info.getStyle()
            .set("display", "grid").set("grid-template-columns", "140px 1fr")
            .set("gap", "10px 16px").set("margin-bottom", "20px").set("font-size", "15px");
        info.add(
            bold("Area:"),   text(currentArea.name()),
            bold("Block:"),  text("Block " + currentBlock.label()),
            bold("Row:"),    text("Row " + currentRow.label()),
            bold("Seat:"),   text(seat.label()),
            bold("Price:"),  text("$50.00")
        );

        Button reserveBtn = new Button("Reserve Ticket", e -> handleSeatReserve(seat));
        reserveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reserveBtn.getStyle()
            .set("background", "#026cdf").set("font-weight", "700").set("width", "100%");

        summaryCard.add(title, info, reserveBtn);
    }

    private void clearSummary() {
        summaryCard.removeAll();
        summaryCard.getStyle().set("text-align", "center").set("color", "#aaa");
        summaryCard.add(new Span("Select a seat to see ticket details"));
    }

    // ── Reserve handlers ──────────────────────────────────────────────────────

    private void handleSeatReserve(SeatData seat) {
    // Check if "token" is present in the session instead of "userId"
    Object token = UI.getCurrent().getSession().getAttribute("token");
    
    if (isMock || token == null) {
        Notification.show("Please log in to reserve a ticket", 3000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_WARNING);
        return;
    }
    
    try {
        // 1. Call the service to lock the seat/create a temporary order
        ticket t = eventService.reserveSeat(eventId, showId, currentArea.id(), seat.id(), userId);
        
        // 2. Store the ticket ID in session so CheckoutView can find it later
        UI.getCurrent().getSession().setAttribute("pendingTicketId", t.getTicketId());
        
        // 3. Open the summary dialog
        openOrderSummaryDialog(t);

    } catch (RuntimeException ex) {
        Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}

private void openOrderSummaryDialog(ticket t) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Order Summary");
    dialog.setModal(true);

    VerticalLayout dialogLayout = new VerticalLayout();
    dialogLayout.add(new Paragraph("Seat reserved successfully!"));
    dialogLayout.add(new H3("Total: $50.00")); // Or fetch price from ticket object

    Button checkoutBtn = new Button("Proceed to Checkout", e -> {
        dialog.close();
        UI.getCurrent().navigate("checkout");
    });
    checkoutBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button continueBtn = new Button("Continue Shopping", e -> {
        dialog.close();
        UI.getCurrent().navigate("main");
    });

    dialogLayout.add(checkoutBtn, continueBtn);
    dialog.add(dialogLayout);
    dialog.open();
}

    private void handleStandingReserve(AreaInfo ai) {
        if (isMock || userId == null) {
            Notification.show("Please log in to reserve a ticket", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        try {
            ticket t = eventService.reserveStanding(eventId, showId, ai.id(), userId);
            Notification.show("Standing ticket reserved! ID: " + t.getTicketId().toString().substring(0, 8),
                    4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate("profile");
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ── Data conversion ───────────────────────────────────────────────────────

    private AreaInfo toAreaInfo(Area area, Map<Long, Boolean> seatAvail) {
        if (area instanceof SeatedArea sa) {
            List<BlockData> blocks = new ArrayList<>();
            for (Block block : sa.getBlocks()) {
                List<RowData> rows = new ArrayList<>();
                for (Row row : block.getRows()) {
                    List<SeatData> seats = new ArrayList<>();
                    for (Seat seat : row.getSeats()) {
                        boolean avail = seatAvail.getOrDefault(seat.getId(), true);
                        seats.add(new SeatData(seat.getId(), seat.getSeatNumber(), avail));
                    }
                    rows.add(new RowData(row.getId(), row.getRowNumber(), seats));
                }
                blocks.add(new BlockData(block.getId(), block.getBlockIdentifier(), rows));
            }
            return new AreaInfo(area.getId(), area.getName(), true, 0, 0, blocks);
        } else if (area instanceof StandingArea sa) {
            int max = sa.getMaxCapacity();
            int current = 0;
            try { current = sa.getCurrentCapacity(); } catch (Exception ignored) {}
            return new AreaInfo(area.getId(), area.getName(), false, max, max - current, List.of());
        }
        return new AreaInfo(area.getId(), area.getName(), false, 0, 0, List.of());
    }

    // ── Mock data ─────────────────────────────────────────────────────────────

    private static show mockShow() {
        return new show(null, "Opening Night",
            "An unforgettable opening with fireworks and live music.", "The Midnight", new Date());
    }

    private static List<AreaInfo> mockAreas() {
        Random rnd = new Random(42);
        List<BlockData> blocks = new ArrayList<>();
        long sid = 1;
        String[] labels = {"A", "B", "C"};
        for (int bi = 0; bi < 3; bi++) {
            List<RowData> rows = new ArrayList<>();
            for (int ri = 1; ri <= 4; ri++) {
                List<SeatData> seats = new ArrayList<>();
                for (int si = 1; si <= 8; si++)
                    seats.add(new SeatData(sid++, String.valueOf(si), rnd.nextDouble() > 0.3));
                rows.add(new RowData(ri, String.valueOf(ri), seats));
            }
            blocks.add(new BlockData(bi + 1, labels[bi], rows));
        }
        return List.of(
            new AreaInfo(UUID.randomUUID(), "Main Stage (Seated)", true,  0,   0,   blocks),
            new AreaInfo(UUID.randomUUID(), "Floor GA (Standing)", false, 300, 175, List.of())
        );
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private Div buildHeader() {
        
        Div header = new Div();
        header.getStyle()
            .set("background", "#026cdf").set("color", "white")
            .set("padding", "28px 52px").set("width", "100%")
            .set("box-sizing", "border-box").set("display", "flex")
            .set("justify-content", "space-between").set("align-items", "center");
        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle().set("margin", "0").set("font-size", "24px").set("font-weight", "900");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));
        logo.getStyle().set("cursor", "pointer");
        Div nav = new Div();
        nav.getStyle().set("display", "flex").set("gap", "32px").set("align-items", "center");
        nav.add(
            clickable("Home",          () -> UI.getCurrent().navigate("main")),
            clickable("← Event",       () -> UI.getCurrent().navigate("EventDetails"))
        );
            Object token = UI.getCurrent().getSession().getAttribute("token");
    if (token != null) {
        nav.add(clickable("👤 My Account", () -> UI.getCurrent().navigate("profile")));
    } else {
        nav.add(clickable("Login", () -> UI.getCurrent().navigate("login")));
    }
        header.add(logo, nav);
        return header;
    }

    private static Div buildShowCard(show s) {
        Div card = card();
        H2 name = new H2(s.getName() != null ? s.getName() : "Show");
        name.getStyle().set("margin", "0 0 12px 0").set("font-size", "22px").set("color", "#111");
        Div meta = new Div();
        meta.getStyle()
            .set("display", "flex").set("gap", "24px")
            .set("font-size", "14px").set("color", "#555");
        meta.add(
            metaItem("Singer", s.getSinger() != null ? s.getSinger() : "—"),
            metaItem("Date", s.getShowDate() != null ? DATE_FMT.format(s.getShowDate()) : "—"));
        card.add(name, meta);
        return card;
    }

    private static Div metaItem(String label, String value) {
        Div d = new Div();
        Span lbl = new Span(label + ": ");
        lbl.getStyle().set("font-weight", "600");
        d.add(lbl, new Span(value));
        return d;
    }

    private static Div backBtn(String text, Runnable onClick) {
        Button btn = new Button(text, e -> onClick.run());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Div wrapper = new Div(btn);
        wrapper.getStyle().set("margin-bottom", "4px");
        return wrapper;
    }

    private static Span stepLabel(String text) {
        Span s = new Span(text);
        s.getStyle()
            .set("font-weight", "700").set("font-size", "14px")
            .set("color", "#555").set("display", "block").set("margin-bottom", "12px");
        return s;
    }

    private static Div legendDot(String color, String label) {
        Div dot = new Div();
        dot.getStyle()
            .set("width", "14px").set("height", "14px")
            .set("border-radius", "50%").set("background", color)
            .set("flex-shrink", "0");
        Div wrapper = new Div(dot, new Span(label));
        wrapper.getStyle()
            .set("display", "flex").set("align-items", "center").set("gap", "6px");
        return wrapper;
    }

    private static Span bold(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-weight", "700").set("color", "#555");
        return s;
    }

    private static Span text(String t) { return new Span(t); }

    private static Span clickable(String text, Runnable onClick) {
        Span s = new Span(text);
        s.getStyle().set("cursor", "pointer").set("font-weight", "700");
        s.addClickListener(e -> onClick.run());
        return s;
    }

    private static Div card() {
        Div c = new Div();
        c.getStyle()
            .set("background", "white").set("border-radius", "16px")
            .set("box-shadow", "0 6px 20px rgba(0,0,0,0.07)")
            .set("padding", "32px 36px").set("width", "100%")
            .set("box-sizing", "border-box");
        return c;
    }

    private static Div emptyState(String msg) {
        Div d = new Div();
        d.getStyle()
            .set("max-width", "920px").set("margin", "60px auto")
            .set("padding", "40px").set("background", "white")
            .set("border-radius", "16px")
            .set("box-shadow", "0 6px 20px rgba(0,0,0,0.07)")
            .set("text-align", "center").set("color", "#888").set("font-size", "16px");
        d.add(new Span(msg));
        return d;
    }

    private static UUID safeUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
