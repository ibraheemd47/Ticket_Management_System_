package com.sdnah.Ticket_Management_System_.Frontend;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("EventDetails")
public class EventDetailsView extends VerticalLayout {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy");
    private static final String[] BLOCK_COLORS = {
        "#1565c0", "#283593", "#0277bd", "#00838f", "#2e7d32", "#558b2f", "#6a1b9a"
    };

    // ── Seat-map data records (no JPA lazy-load risk in the view) ─────────────
    record BlockData(long id, String label, List<RowData> rows) {}
    record RowData(long id, String label, List<SeatData> seats) {}
    record SeatData(long id, String label, boolean available) {}
    record AreaInfo(UUID id, String name, boolean isSeated,
                    int standingMax, int standingAvail, List<BlockData> blocks) {}

    private final EventService eventService;
    private final TicketService ticketService;

    // Areas preloaded during construction (while JPA session may be open)
    private final Map<UUID, List<Area>> showAreasCache = new HashMap<>();

    private UUID cachedEventId;
    private UUID cachedUserId;

    public EventDetailsView(EventService eventService, TicketService ticketService) {
        this.eventService   = eventService;
        this.ticketService  = ticketService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(buildHeader());

        Object eventIdObj = UI.getCurrent().getSession().getAttribute("eventId");
        Object userIdObj  = UI.getCurrent().getSession().getAttribute("userId");
        if (userIdObj != null) {
            try { cachedUserId = UUID.fromString(userIdObj.toString()); } catch (Exception ignored) {}
        }

        Event ev;
        List<show> shows;

        if (eventIdObj == null) {
            ev    = mockEvent();
            shows = List.of(mockShow());
        } else {
            try {
                UUID eventId = UUID.fromString(eventIdObj.toString());
                cachedEventId = eventId;
                ev    = eventService.getEventDetails(eventId);
                shows = eventService.getShowsForEvent(eventId);
            } catch (RuntimeException ex) {
                add(emptyState("Could not load event: " + ex.getMessage()));
                return;
            }
        }

        // Preload areas for each show while we are still in the construction context
        for (show s : shows) {
            if (s.getShowid() == null) continue;
            try {
                List<Area> areas = s.getAreas();
                if (areas != null)
                    showAreasCache.put(s.getShowid(), new ArrayList<>(areas));
            } catch (Exception ignored) {
                // lazy loading not available outside transaction — cache stays empty
            }
        }

        Div content = new Div(buildEventInfoCard(ev), buildShowsCard(shows));
        content.getStyle()
                .set("max-width", "860px")
                .set("margin", "40px auto")
                .set("width", "100%")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "24px");

        add(content);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private Div buildHeader() {
        Div header = new Div();
        header.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "28px 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle().set("margin", "0").set("font-size", "24px").set("font-weight", "900");
        logo.getStyle().set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Div nav = new Div();
        nav.getStyle().set("display", "flex").set("gap", "32px").set("align-items", "center");
        nav.add(
                clickable("Home",          () -> UI.getCurrent().navigate("main")),
                clickable("← Company",    () -> UI.getCurrent().navigate("company")),
                clickable("👤 My Account", () -> UI.getCurrent().navigate("profile")));
        header.add(logo, nav);
        return header;
    }

    // ── Event info card ──────────────────────────────────────────────────────

    private Div buildEventInfoCard(Event ev) {
        Div card = card();

        Div titleRow = new Div();
        titleRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "flex-start")
                .set("margin-bottom", "20px");

        H2 name = new H2(ev.getName());
        name.getStyle().set("margin", "0").set("font-size", "26px").set("color", "#111");

        Span typeBadge = badge(
                capitalize(ev.getEventType() != null ? ev.getEventType().name() : "—"),
                "#e3f2fd", "#026cdf");

        titleRow.add(name, typeBadge);

        Div details = new Div();
        details.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "12px 32px");

        details.add(
                infoRow("Venue",      ev.getVenue() != null ? ev.getVenue() : "—"),
                infoRow("Start Date", formatDate(ev.getStartDate())),
                infoRow("Event ID",   ev.getEventId() != null
                        ? ev.getEventId().toString().substring(0, 8) + "…" : "—"),
                infoRow("End Date",   formatDate(ev.getEndDate()))
        );

        card.add(titleRow, details);

        if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
            Paragraph desc = new Paragraph(ev.getDescription());
            desc.getStyle().set("color", "#555").set("margin-top", "16px").set("line-height", "1.6");
            card.add(desc);
        }

        return card;
    }

    // ── Shows card ───────────────────────────────────────────────────────────

    private Div buildShowsCard(List<show> shows) {
        Div card = card();

        H2 title = new H2("Shows");
        title.getStyle().set("margin", "0 0 16px 0").set("font-size", "20px").set("color", "#111");

        if (shows == null || shows.isEmpty()) {
            Paragraph empty = new Paragraph("No shows have been added to this event yet.");
            empty.getStyle().set("color", "#888");
            card.add(title, empty);
            return card;
        }

        Grid<show> grid = new Grid<>(show.class, false);
        grid.addColumn(s -> nullSafe(s.getName())).setHeader("Show Name").setFlexGrow(2);
        grid.addColumn(s -> nullSafe(s.getSinger())).setHeader("Singer / Performer").setFlexGrow(2);
        grid.addColumn(s -> s.getShowDate() != null ? DATE_FMT.format(s.getShowDate()) : "—")
                .setHeader("Date").setFlexGrow(1);
        grid.addColumn(s -> {
            List<Area> areas = showAreasCache.getOrDefault(s.getShowid(), List.of());
            int total = areas.stream().mapToInt(EventDetailsView::areaTotal).sum();
            int avail = areas.stream().mapToInt(EventDetailsView::areaAvailable).sum();
            return areas.isEmpty() ? "—" : avail + " / " + total;
        }).setHeader("Available Seats").setFlexGrow(1);
        grid.addComponentColumn(s -> {
            Button detailsBtn = new Button("Details", e -> openShowDetailsDialog(s));
            detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button seatBtn = new Button("Select Seat", e -> openSeatDialog(s));
            seatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Div actions = new Div(detailsBtn, seatBtn);
            actions.getStyle().set("display", "flex").set("gap", "6px");
            return actions;
        }).setHeader("").setAutoWidth(true);

        grid.setItems(shows);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        card.add(title, grid);
        return card;
    }

    // ── Show details dialog ──────────────────────────────────────────────────

    private void openShowDetailsDialog(show s) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeaderTitle("Show Details");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.getStyle().set("gap", "6px");

        body.add(
                dialogRow("Name",        nullSafe(s.getName())),
                dialogRow("Singer",      nullSafe(s.getSinger())),
                dialogRow("Date",        s.getShowDate() != null ? DATE_FMT.format(s.getShowDate()) : "—"),
                dialogRow("Description", nullSafe(s.getDescription())),
                dialogRow("Show ID",     s.getShowid() != null ? s.getShowid().toString() : "—")
        );

        // ── Areas & seats ──
        H3 areasHeader = new H3("Areas & Available Seats");
        areasHeader.getStyle()
                .set("margin", "20px 0 8px 0")
                .set("font-size", "16px")
                .set("color", "#333");

        body.add(areasHeader);

        List<Area> areas = showAreasCache.getOrDefault(s.getShowid(), List.of());

        if (areas.isEmpty()) {
            Div note = new Div();
            note.getStyle()
                    .set("background", "#f5f5f5")
                    .set("border-radius", "8px")
                    .set("padding", "12px 16px")
                    .set("color", "#888")
                    .set("font-size", "14px");
            note.add(new Span("No areas have been added to this show yet."));
            body.add(note);
        } else {
            Grid<Area> areaGrid = new Grid<>(Area.class, false);
            areaGrid.addColumn(Area::getName).setHeader("Area").setFlexGrow(2);
            areaGrid.addColumn(a -> a instanceof StandingArea ? "Standing" : "Seated")
                    .setHeader("Type").setFlexGrow(1);
            areaGrid.addColumn(a -> {
                int total = areaTotal(a);
                return total >= 0 ? String.valueOf(total) : "—";
            }).setHeader("Total Seats").setFlexGrow(1);
            areaGrid.addComponentColumn(a -> {
                int avail = areaAvailable(a);
                int total = areaTotal(a);
                String label = avail >= 0 ? avail + " / " + total : "—";
                Span chip = new Span(label);
                chip.getStyle()
                        .set("padding", "3px 10px")
                        .set("border-radius", "999px")
                        .set("font-size", "13px")
                        .set("font-weight", "700");
                if (avail > 0) {
                    chip.getStyle().set("background", "#e8f5e9").set("color", "#2e7d32");
                } else if (avail == 0) {
                    chip.getStyle().set("background", "#ffebee").set("color", "#c62828");
                } else {
                    chip.getStyle().set("background", "#f5f5f5").set("color", "#888");
                }
                return chip;
            }).setHeader("Available").setFlexGrow(1);

            areaGrid.setItems(areas);
            areaGrid.setAllRowsVisible(true);
            areaGrid.setWidthFull();
            body.add(areaGrid);
        }

        dialog.add(body);

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);

        dialog.open();
    }

    // ── Area seat helpers ────────────────────────────────────────────────────

    private static int areaTotal(Area area) {
        if (area instanceof StandingArea sa) return sa.getMaxCapacity();
        if (area instanceof SeatedArea)    return -1; // blocks×rows×seats not available without deep fetch
        return -1;
    }

    private static int areaAvailable(Area area) {
        if (area instanceof StandingArea sa) {
            try {
                return sa.getMaxCapacity() - sa.getCurrentCapacity();
            } catch (Exception e) {
                return sa.getMaxCapacity(); // areaMap null (DB-loaded) — assume all available
            }
        }
        return -1;
    }

    // ── Mock data ────────────────────────────────────────────────────────────

    private static Event mockEvent() {
        Event ev = new Event("Summer Music Festival 2025", show_type.FESTIVAL, 1L, 1L);
        ev.editVenue("BGU Amphitheatre, Beer-Sheva", 1L);
        ev.editDescription(
                "A three-day outdoor festival featuring top local and international artists across multiple stages.",
                1L);
        try {
            ev.editDates(
                    new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-10"),
                    new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-12"),
                    1L);
        } catch (Exception ignored) {}
        return ev;
    }

    private static show mockShow() {
        try {
            Date showDate = new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-10");
            show s = new show(null, "Opening Night",
                    "An unforgettable opening with fireworks and live music.",
                    "The Midnight", showDate);

            StandingArea floor = new StandingArea("Floor GA", 500);
            StandingArea vip   = new StandingArea("VIP Standing", 80);
            SeatedArea seated  = new SeatedArea("Seated Balcony", 4);
            s.setAreas(List.of(floor, vip, seated));
            return s;
        } catch (Exception e) {
            return new show(null, "Opening Night", "Live music and fireworks.", "The Midnight", new Date());
        }
    }

    // ── UI helpers ───────────────────────────────────────────────────────────

    private static Div infoRow(String label, String value) {
        Div row = new Div();
        Span lbl = new Span(label + ": ");
        lbl.getStyle().set("font-weight", "600").set("color", "#555");
        Span val = new Span(value);
        val.getStyle().set("color", "#111");
        row.add(lbl, val);
        return row;
    }

    private static Div dialogRow(String label, String value) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("gap", "8px")
                .set("padding", "6px 0")
                .set("border-bottom", "1px solid #f0f0f0");
        Span lbl = new Span(label + ":");
        lbl.getStyle().set("font-weight", "600").set("color", "#555").set("min-width", "100px");
        Span val = new Span(value);
        val.getStyle().set("color", "#111");
        row.add(lbl, val);
        return row;
    }

    private static Span badge(String text, String bg, String color) {
        Span s = new Span(text);
        s.getStyle()
                .set("background", bg)
                .set("color", color)
                .set("padding", "4px 12px")
                .set("border-radius", "999px")
                .set("font-size", "13px")
                .set("font-weight", "700");
        return s;
    }

    private static Div card() {
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.07)")
                .set("padding", "32px 36px")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return card;
    }

    private static Span clickable(String text, Runnable onClick) {
        Span s = new Span(text);
        s.getStyle().set("cursor", "pointer").set("font-weight", "700");
        s.addClickListener(e -> onClick.run());
        return s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static String nullSafe(String s) {
        return s == null ? "—" : s;
    }

    private static String formatDate(Date d) {
        return d != null ? DATE_FMT.format(d) : "—";
    }

    private static Div emptyState(String message) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("max-width", "860px")
                .set("margin", "60px auto")
                .set("padding", "40px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.07)")
                .set("text-align", "center")
                .set("color", "#888")
                .set("font-size", "16px");
        wrapper.add(new Span(message));
        return wrapper;
    }

    // ── Seat selection dialog flow ────────────────────────────────────────────

    private void openSeatDialog(show s) {
        List<AreaInfo> areas;

        if (cachedEventId != null && s.getShowid() != null) {
            try {
                show full = eventService.loadShowFully(cachedEventId, s.getShowid());
                Map<Long, Boolean> avail = ticketService.getSeatAvailability(s.getShowid());
                areas = new ArrayList<>();
                if (full.getAreas() != null)
                    for (Area a : full.getAreas()) areas.add(toAreaInfo(a, avail));
            } catch (Exception ex) {
                Notification.show("Could not load seat data: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE);
                return;
            }
        } else {
            areas = mockAreaInfoList(); // demo mode
        }

        Dialog dialog = new Dialog();
        dialog.setWidth("720px");
        dialog.setHeaderTitle("Select Your Seat — " + nullSafe(s.getName()));

        Div stepContent = new Div();
        stepContent.setWidthFull();
        stepContent.getStyle().set("min-height", "260px");

        VerticalLayout body = new VerticalLayout(stepContent);
        body.setPadding(true);
        body.setSpacing(false);
        dialog.add(body);

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeBtn);

        showAreaStep(dialog, stepContent, areas, s);
        dialog.open();
    }

    private void showAreaStep(Dialog dialog, Div stepContent, List<AreaInfo> areas, show s) {
        // Skip area step when only one seated area
        if (areas.size() == 1 && areas.get(0).isSeated()) {
            showBlockStep(dialog, stepContent, areas.get(0), areas, s);
            return;
        }

        stepContent.removeAll();
        Span label = stepLabel("Choose an Area");
        stepContent.add(label);

        Div grid = new Div();
        grid.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "10px");

        for (AreaInfo ai : areas) {
            if (ai.isSeated()) {
                Button btn = new Button(ai.name() + " (Seated)",
                        e -> showBlockStep(dialog, stepContent, ai, areas, s));
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                btn.setWidthFull();
                grid.add(btn);
            } else {
                grid.add(buildStandingAreaCard(ai, s, dialog));
            }
        }
        stepContent.add(grid);
    }

    private void showBlockStep(Dialog dialog, Div stepContent,
                               AreaInfo area, List<AreaInfo> areas, show s) {
        stepContent.removeAll();

        if (areas.size() > 1) {
            stepContent.add(backBtn("← Areas",
                    () -> showAreaStep(dialog, stepContent, areas, s)));
        }
        stepContent.add(stepLabel(area.name() + " — Select a Block"));

        Div grid = new Div();
        grid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fill, minmax(150px, 1fr))")
            .set("gap", "12px");

        for (int i = 0; i < area.blocks().size(); i++) {
            BlockData block = area.blocks().get(i);
            String color = BLOCK_COLORS[i % BLOCK_COLORS.length];

            long total = block.rows().stream().mapToLong(r -> r.seats().size()).sum();
            long avail = block.rows().stream()
                .flatMap(r -> r.seats().stream()).filter(SeatData::available).count();

            Div card = new Div();
            card.getStyle()
                .set("background", color).set("color", "white")
                .set("border-radius", "12px").set("padding", "20px 12px")
                .set("text-align", "center").set("cursor", "pointer")
                .set("font-weight", "700").set("font-size", "16px").set("user-select", "none");

            Span nameSpan = new Span("Block " + block.label());
            Span availSpan = new Span(avail + "/" + total + " seats");
            availSpan.getStyle().set("font-size", "12px").set("opacity", "0.85")
                .set("display", "block").set("margin-top", "4px").set("font-weight", "400");
            card.add(nameSpan, availSpan);

            final BlockData b = block;
            card.addClickListener(e ->
                showRowStep(dialog, stepContent, area, b, areas, s));
            grid.add(card);
        }
        stepContent.add(grid);
    }

    private void showRowStep(Dialog dialog, Div stepContent,
                             AreaInfo area, BlockData block, List<AreaInfo> areas, show s) {
        stepContent.removeAll();
        stepContent.add(backBtn("← Blocks",
                () -> showBlockStep(dialog, stepContent, area, areas, s)));
        stepContent.add(stepLabel("Block " + block.label() + " — Select a Row"));

        Div rowFlex = new Div();
        rowFlex.getStyle()
            .set("display", "flex").set("flex-wrap", "wrap").set("gap", "8px").set("margin-top", "8px");

        for (RowData row : block.rows()) {
            long avail = row.seats().stream().filter(SeatData::available).count();
            Button btn = new Button("Row " + row.label() + "  (" + avail + " free)");
            if (avail > 0) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                final RowData r = row;
                btn.addClickListener(e ->
                    showSeatStep(dialog, stepContent, area, block, r, areas, s));
            } else {
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                btn.setEnabled(false);
            }
            rowFlex.add(btn);
        }
        stepContent.add(rowFlex);
    }

    private void showSeatStep(Dialog dialog, Div stepContent,
                              AreaInfo area, BlockData block, RowData row,
                              List<AreaInfo> areas, show s) {
        stepContent.removeAll();
        stepContent.add(backBtn("← Rows",
                () -> showRowStep(dialog, stepContent, area, block, areas, s)));
        stepContent.add(stepLabel(
                "Block " + block.label() + " › Row " + row.label() + " — Select a Seat"));

        // Legend
        Div legend = new Div();
        legend.getStyle()
            .set("display", "flex").set("gap", "16px")
            .set("margin", "6px 0 14px 0").set("font-size", "13px").set("align-items", "center");
        legend.add(legendDot("#4caf50", "Available"), legendDot("#ef5350", "Taken"),
                   legendDot("#026cdf", "Selected"));
        stepContent.add(legend);

        Set<Div> availCircles = new LinkedHashSet<>();
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
                availCircles.add(circle);
                final SeatData chosen = seat;
                circle.addClickListener(e -> {
                    availCircles.forEach(c ->
                        c.getStyle().set("background", "#4caf50").remove("transform"));
                    circle.getStyle().set("background", "#026cdf").set("transform", "scale(1.15)");
                    openSummaryDialog(area, block, row, chosen, s, dialog);
                });
            }
            seatFlex.add(circle);
        }
        stepContent.add(seatFlex);
    }

    private void openSummaryDialog(AreaInfo area, BlockData block, RowData row,
                                   SeatData seat, show s, Dialog parentDialog) {
        Dialog summary = new Dialog();
        summary.setWidth("420px");
        summary.setHeaderTitle("Confirm Reservation");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.getStyle().set("gap", "6px");
        body.add(
            dialogRow("Show",  nullSafe(s.getName())),
            dialogRow("Area",  area.name()),
            dialogRow("Block", "Block " + block.label()),
            dialogRow("Row",   "Row " + row.label()),
            dialogRow("Seat",  seat.label()),
            dialogRow("Price", "$50.00")
        );
        summary.add(body);

        Button reserveBtn = new Button("Reserve Ticket", e -> {
            UUID showId = s.getShowid();
            if (cachedEventId == null || showId == null || cachedUserId == null) {
                Notification.show("Please log in to reserve a ticket",
                        3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                ticket t = eventService.reserveSeat(cachedEventId, showId,
                        area.id(), seat.id(), cachedUserId);
                summary.close();
                parentDialog.close();
                Notification.show("Ticket reserved! ID: " + t.getTicketId().toString().substring(0, 8),
                        4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        reserveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button backBtn = new Button("← Back", e -> summary.close());
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        summary.getFooter().add(backBtn, reserveBtn);
        summary.open();
    }

    private Div buildStandingAreaCard(AreaInfo ai, show s, Dialog parentDialog) {
        Div card = new Div();
        card.getStyle()
            .set("background", "#f0f4ff").set("border-radius", "10px")
            .set("padding", "16px 20px").set("display", "flex")
            .set("justify-content", "space-between").set("align-items", "center");

        Div info = new Div();
        Span name = new Span(ai.name());
        name.getStyle().set("font-weight", "700").set("font-size", "15px");
        boolean hasSpots = ai.standingAvail() > 0;
        Span cap = new Span(ai.standingAvail() + " / " + ai.standingMax() + " spots — $30.00");
        cap.getStyle().set("font-size", "13px")
            .set("color", hasSpots ? "#2e7d32" : "#c62828")
            .set("display", "block");
        info.add(name, cap);

        Button btn = new Button("Reserve", e -> {
            UUID showId = s.getShowid();
            if (cachedEventId == null || showId == null || cachedUserId == null) {
                Notification.show("Please log in to reserve a ticket",
                        3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                ticket t = eventService.reserveStanding(cachedEventId, showId, ai.id(), cachedUserId);
                parentDialog.close();
                Notification.show("Standing ticket reserved! ID: " + t.getTicketId().toString().substring(0, 8),
                        4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.setEnabled(hasSpots);

        card.add(info, btn);
        return card;
    }

    // ── Area data conversion ──────────────────────────────────────────────────

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

    private static List<AreaInfo> mockAreaInfoList() {
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
            new AreaInfo(UUID.randomUUID(), "Main Stage (Seated)", true, 0, 0, blocks),
            new AreaInfo(UUID.randomUUID(), "Floor GA (Standing)", false, 300, 175, List.of())
        );
    }

    // ── Dialog UI helpers ─────────────────────────────────────────────────────

    private static Span stepLabel(String text) {
        Span s = new Span(text);
        s.getStyle()
            .set("font-weight", "700").set("font-size", "14px").set("color", "#444")
            .set("display", "block").set("margin-bottom", "12px");
        return s;
    }

    private static Div backBtn(String text, Runnable onClick) {
        Button btn = new Button(text, e -> onClick.run());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Div wrapper = new Div(btn);
        wrapper.getStyle().set("margin-bottom", "8px");
        return wrapper;
    }

    private static Div legendDot(String color, String label) {
        Div dot = new Div();
        dot.getStyle()
            .set("width", "13px").set("height", "13px")
            .set("border-radius", "50%").set("background", color).set("flex-shrink", "0");
        Div wrapper = new Div(dot, new Span(label));
        wrapper.getStyle().set("display", "flex").set("align-items", "center").set("gap", "5px");
        return wrapper;
    }
}
