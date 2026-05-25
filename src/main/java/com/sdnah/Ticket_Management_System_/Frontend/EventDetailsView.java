package com.sdnah.Ticket_Management_System_.Frontend;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.CompositePurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import java.util.Optional;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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

    private final ActiveOrderService orderService;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy");
    private static final String[] BLOCK_COLORS = {
            "#1565c0", "#283593", "#0277bd", "#00838f", "#2e7d32", "#558b2f", "#6a1b9a"
    };

    // ── Seat-map data records (no JPA lazy-load risk in the view) ─────────────
    record BlockData(long id, String label, List<RowData> rows) {
    }

    record RowData(long id, String label, List<SeatData> seats) {
    }

    record SeatData(long id, String label, boolean available) {
    }

    record AreaInfo(UUID id, String name, boolean isSeated,
            int standingMax, int standingAvail, List<BlockData> blocks) {
    }

    record CartEntry(String description, java.math.BigDecimal unitPrice, int quantity,
            boolean isSeated, UUID areaId, Long seatId) {
        java.math.BigDecimal total() {
            return unitPrice.multiply(java.math.BigDecimal.valueOf(quantity));
        }
    }

    private final EventService eventService;
    private final TicketService ticketService;
    private final PolicyRepository policyRepo;

    // Areas preloaded during construction (while JPA session may be open)
    private final Map<UUID, List<Area>> showAreasCache = new HashMap<>();

    private UUID cachedEventId;
    private UUID cachedUserId;
    private Event cachedEvent;
    private List<show> cachedShows = new ArrayList<>();

    public EventDetailsView(EventService eventService, TicketService ticketService, PolicyRepository policyRepo,
            ActiveOrderService orderService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
        this.policyRepo = policyRepo;
        this.orderService = orderService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(buildHeader());

        Object eventIdObj = UI.getCurrent().getSession().getAttribute("eventId");
        Object userIdObj = UI.getCurrent().getSession().getAttribute("userId");
        if (userIdObj != null) {
            try {
                System.out.println("Cached user ID: " + userIdObj.toString());
                cachedUserId = UUID.fromString(userIdObj.toString());
                System.out.println("Parsed user ID: " + cachedUserId.toString());
            } catch (Exception ignored) {
                cachedUserId = UUID.randomUUID();
            }
        }

        Event ev;
        List<show> shows;

        if (eventIdObj == null) {
            ev = mockEvent();
            shows = List.of(mockShow());
        } else {
            try {
                UUID eventId = UUID.fromString(eventIdObj.toString());
                cachedEventId = eventId;
                ev = eventService.getEventDetails(eventId);
                shows = eventService.getShowsForEvent(eventId);
            } catch (RuntimeException ex) {
                add(emptyState("Could not load event: " + ex.getMessage()));
                return;
            }
        }

        // Preload areas for each show while we are still in the construction context
        for (show s : shows) {
            if (s.getShowid() == null)
                continue;
            try {
                List<Area> areas = s.getAreas();
                if (areas != null)
                    showAreasCache.put(s.getShowid(), new ArrayList<>(areas));
            } catch (Exception ignored) {
                // lazy loading not available outside transaction — cache stays empty
            }
        }

        cachedEvent = ev;
        cachedShows = new ArrayList<>(shows);

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
                clickable("Home", () -> UI.getCurrent().navigate("main")),
                clickable("← Company", () -> UI.getCurrent().navigate("company")),
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

        Div rightSide = new Div();
        rightSide.getStyle().set("display", "flex").set("gap", "12px").set("align-items", "center");
        rightSide.add(typeBadge);

        if (isManagerOrOwner()) {
            Button editBtn = new Button("Edit Event", e -> openEditDialog());
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editBtn.getStyle()
                    .set("background", "#026cdf")
                    .set("color", "white")
                    .set("font-weight", "700");

            Button deleteEventBtn = new Button("Delete Event", e -> openDeleteEventConfirm());
            deleteEventBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteEventBtn.getStyle()
                    .set("font-weight", "700");

            rightSide.add(editBtn, deleteEventBtn);
        }

        titleRow.add(name, rightSide);

        Div details = new Div();
        details.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "12px 32px");

        details.add(
                infoRow("Venue", ev.getVenue() != null ? ev.getVenue() : "—"),
                infoRow("Start Date", formatDate(ev.getStartDate())),
                infoRow("Event ID", ev.getEventId() != null
                        ? ev.getEventId().toString().substring(0, 8) + "…"
                        : "—"),
                infoRow("End Date", formatDate(ev.getEndDate())));

        card.add(titleRow, details);

        if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
            Paragraph desc = new Paragraph(ev.getDescription());
            desc.getStyle().set("color", "#555").set("margin-top", "16px").set("line-height", "1.6");
            card.add(desc);
        }

        return card;
    }

    // ── Manager check ────────────────────────────────────────────────────────

    private boolean isManagerOrOwner() {
        Object companyIdObj = UI.getCurrent().getSession().getAttribute("managingCompanyId");
        if (companyIdObj == null || cachedEvent == null)
            return false;
        try {
            Long companyId = Long.valueOf(companyIdObj.toString());
            // Use companyId (plain column, no lazy-loading) instead of managerIds
            // (ElementCollection)
            return companyId.equals(cachedEvent.getCompanyId());
        } catch (Exception e) {
            return false;
        }
    }

    private Long getManagerId() {
        Object obj = UI.getCurrent().getSession().getAttribute("managingCompanyId");
        return obj != null ? Long.valueOf(obj.toString()) : null;
    }

    // ── Edit dialog ──────────────────────────────────────────────────────────

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("700px");
        dialog.setHeaderTitle("Edit Event");

        Tab detailsTab = new Tab("Details");
        Tab showsTab = new Tab("Shows");
        Tab policiesTab = new Tab("Policies");
        Tabs tabs = new Tabs(detailsTab, showsTab, policiesTab);
        tabs.setWidthFull();

        Div detailsSection = buildEditDetailsSection(dialog);
        Div showsSection = buildEditShowsSection(dialog);
        Div policiesSection = buildEditPoliciesSection();

        showsSection.setVisible(false);
        policiesSection.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            Tab sel = e.getSelectedTab();
            detailsSection.setVisible(sel == detailsTab);
            showsSection.setVisible(sel == showsTab);
            policiesSection.setVisible(sel == policiesTab);
        });

        VerticalLayout body = new VerticalLayout(tabs, detailsSection, showsSection, policiesSection);
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidthFull();

        dialog.add(body);
        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    // ── Edit details section ─────────────────────────────────────────────────

    private Div buildEditDetailsSection(Dialog parent) {
        Div section = new Div();
        section.getStyle()
                .set("padding", "16px 0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");
        section.setWidthFull();

        TextField nameField = new TextField("Event Name");
        nameField.setValue(cachedEvent.getName() != null ? cachedEvent.getName() : "");
        nameField.setWidthFull();

        TextField venueField = new TextField("Venue");
        venueField.setValue(cachedEvent.getVenue() != null ? cachedEvent.getVenue() : "");
        venueField.setWidthFull();

        ComboBox<show_type> typeBox = new ComboBox<>("Event Type");
        typeBox.setItems(show_type.values());
        typeBox.setItemLabelGenerator(t -> capitalize(t.name()));
        typeBox.setValue(cachedEvent.getEventType());
        typeBox.setWidthFull();

        DatePicker startPicker = new DatePicker("Start Date");
        if (cachedEvent.getStartDate() != null)
            startPicker.setValue(cachedEvent.getStartDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        startPicker.setWidthFull();

        DatePicker endPicker = new DatePicker("End Date");
        if (cachedEvent.getEndDate() != null)
            endPicker.setValue(cachedEvent.getEndDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        endPicker.setWidthFull();

        TextArea descField = new TextArea("Description");
        descField.setValue(cachedEvent.getDescription() != null ? cachedEvent.getDescription() : "");
        descField.setWidthFull();
        descField.setMinHeight("80px");

        Button saveBtn = new Button("Save Details", e -> {
            String name = nameField.getValue();
            if (name == null || name.isBlank()) {
                error("Name is required");
                return;
            }
            Long mgr = getManagerId();
            if (mgr == null) {
                error("Session expired — please log in again");
                return;
            }
            UUID evId = cachedEvent.getEventId();
            try {
                eventService.editEventName(evId, name.trim(), mgr);
                eventService.editEventVenue(evId, venueField.getValue().trim(), mgr);
                if (typeBox.getValue() != null)
                    eventService.editEventType(evId, typeBox.getValue(), mgr);
                eventService.editEventDescription(evId, descField.getValue().trim(), mgr);

                LocalDate s = startPicker.getValue();
                LocalDate en = endPicker.getValue();
                Date startDate = s != null ? Date.from(s.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
                Date endDate = en != null ? Date.from(en.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
                eventService.editEventDates(evId, startDate, endDate, mgr);

                Notification.show("Event details saved",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().getPage().reload();
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "#026cdf").set("color", "white").set("font-weight", "700");

        section.add(nameField, venueField, typeBox, startPicker, endPicker, descField, saveBtn);
        return section;
    }

    // ── Edit shows section ────────────────────────────────────────────────────

    private Div buildEditShowsSection(Dialog parent) {
        Div section = new Div();
        section.getStyle()
                .set("padding", "16px 0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");
        section.setWidthFull();

        Div listContainer = new Div();
        listContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px");
        listContainer.setWidthFull();

        Runnable[] refresh = { null };
        refresh[0] = () -> {
            listContainer.removeAll();
            for (show s : cachedShows) {
                Div row = new Div();
                row.getStyle()
                        .set("background", "#f8faff")
                        .set("border", "1px solid #d0e4ff")
                        .set("border-radius", "10px")
                        .set("padding", "12px 16px")
                        .set("display", "flex")
                        .set("justify-content", "space-between")
                        .set("align-items", "center");

                Div info = new Div();
                Span nameSpan = new Span(nullSafe(s.getName()));
                nameSpan.getStyle().set("font-weight", "700").set("font-size", "14px");
                Span singerSpan = new Span(s.getSinger() != null ? "  •  " + s.getSinger() : "");
                singerSpan.getStyle().set("color", "#666").set("font-size", "13px");
                Span dateSpan = new Span(s.getShowDate() != null ? "  •  " + DATE_FMT.format(s.getShowDate()) : "");
                dateSpan.getStyle().set("color", "#888").set("font-size", "13px");
                info.add(nameSpan, singerSpan, dateSpan);

                Button editBtn = new Button("Edit", ev -> openEditShowDialog(s, refresh, parent));
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

                Button deleteBtn = new Button("Delete", ev -> openDeleteShowConfirm(s));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

                Div actions = new Div(editBtn, deleteBtn);
                actions.getStyle().set("display", "flex").set("gap", "4px");
                row.add(info, actions);
                listContainer.add(row);
            }
        };
        refresh[0].run();

        Button addBtn = new Button("+ Add Show", e -> openEditShowDialog(null, refresh, parent));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("align-self", "flex-start");

        section.add(listContainer, addBtn);
        return section;
    }

    private void openEditShowDialog(show existing, Runnable[] refresh, Dialog parent) {
        boolean isEdit = existing != null;
        Dialog dialog = new Dialog();
        dialog.setWidth("620px");
        dialog.setHeaderTitle(isEdit ? "Edit Show" : "Add Show");

        // ── Basic info ────────────────────────────────────────────────────────
        TextField nameField = new TextField("Show Name");
        nameField.setWidthFull();
        TextField singerField = new TextField("Singer / Performer");
        singerField.setWidthFull();
        TextArea descField = new TextArea("Description");
        descField.setWidthFull();
        descField.setMinHeight("72px");
        DatePicker datePicker = new DatePicker("Show Date");
        datePicker.setWidthFull();

        // ── Standing area ─────────────────────────────────────────────────────
        IntegerField standingCapField = new IntegerField("Capacity");
        standingCapField.setMin(0);
        standingCapField.setPlaceholder("0 = no standing area");
        standingCapField.setWidthFull();

        NumberField standingPriceField = new NumberField("Ticket Price ($)");
        standingPriceField.setMin(0);
        standingPriceField.setPlaceholder("e.g. 30");
        standingPriceField.setWidthFull();

        // ── Seated area ───────────────────────────────────────────────────────
        IntegerField blocksField = new IntegerField("Number of Blocks");
        blocksField.setMin(0);
        blocksField.setPlaceholder("e.g. 5");
        blocksField.setWidthFull();

        IntegerField rowsField = new IntegerField("Rows per Block");
        rowsField.setMin(0);
        rowsField.setPlaceholder("e.g. 10");
        rowsField.setWidthFull();

        IntegerField seatsField = new IntegerField("Seats per Row");
        seatsField.setMin(0);
        seatsField.setPlaceholder("e.g. 20");
        seatsField.setWidthFull();

        NumberField seatedPriceField = new NumberField("Ticket Price ($)");
        seatedPriceField.setMin(0);
        seatedPriceField.setPlaceholder("e.g. 50");
        seatedPriceField.setWidthFull();

        // ── Pre-fill when editing ─────────────────────────────────────────────
        if (isEdit) {
            nameField.setValue(nullSafe(existing.getName()));
            singerField.setValue(nullSafe(existing.getSinger()));
            descField.setValue(nullSafe(existing.getDescription()));
            if (existing.getShowDate() != null)
                datePicker.setValue(existing.getShowDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate());
            if (existing.getStandingPrice() != null)
                standingPriceField.setValue(existing.getStandingPrice().doubleValue());
            if (existing.getSeatedPrice() != null)
                seatedPriceField.setValue(existing.getSeatedPrice().doubleValue());
        }

        // ── Save ──────────────────────────────────────────────────────────────
        Button saveBtn = new Button(isEdit ? "Save" : "Add Show", e -> {
            String sName = nameField.getValue();
            if (sName == null || sName.isBlank()) {
                error("Show name is required");
                return;
            }
            Long mgr = getManagerId();
            if (mgr == null) {
                error("Session expired — please log in again");
                return;
            }

            java.math.BigDecimal seatedPrice = seatedPriceField.getValue() != null
                    ? java.math.BigDecimal.valueOf(seatedPriceField.getValue())
                    : new java.math.BigDecimal("50.00");
            java.math.BigDecimal standingPrice = standingPriceField.getValue() != null
                    ? java.math.BigDecimal.valueOf(standingPriceField.getValue())
                    : new java.math.BigDecimal("30.00");

            Date showDate = datePicker.getValue() != null
                    ? Date.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            try {
                if (isEdit) {
                    // ── Edit: update fields in place — never delete seats ─────
                    eventService.updateShowBasicFields(
                            cachedEventId, existing.getShowid(),
                            sName.trim(), descField.getValue().trim(),
                            singerField.getValue().trim(), showDate,
                            seatedPrice, standingPrice, mgr);

                    // Reflect changes locally so the list in the Edit dialog updates
                    existing.setName(sName.trim());
                    existing.setDescription(descField.getValue().trim());
                    existing.setSinger(singerField.getValue().trim());
                    existing.setShowDate(showDate);
                    existing.setSeatedPrice(seatedPrice);
                    existing.setStandingPrice(standingPrice);

                } else {
                    // ── Add: validate areas and create the new show ───────────
                    int standingCap = standingCapField.getValue() != null ? standingCapField.getValue() : 0;
                    int numBlocks = blocksField.getValue() != null ? blocksField.getValue() : 0;
                    int rowsPerBlock = rowsField.getValue() != null ? rowsField.getValue() : 0;
                    int seatsPerRow = seatsField.getValue() != null ? seatsField.getValue() : 0;

                    if (standingCap == 0 && numBlocks == 0) {
                        error("Add at least one area (standing or seated)");
                        return;
                    }
                    boolean hasSeated = numBlocks > 0 || rowsPerBlock > 0 || seatsPerRow > 0;
                    if (hasSeated && (numBlocks <= 0 || rowsPerBlock <= 0 || seatsPerRow <= 0)) {
                        error("Seated area requires Blocks, Rows per Block, and Seats per Row all > 0");
                        return;
                    }

                    show newShow = new show(cachedEventId, sName.trim(),
                            descField.getValue().trim(), singerField.getValue().trim(), showDate);
                    newShow.setAreas(buildShowAreas(standingCap, numBlocks, rowsPerBlock, seatsPerRow));
                    newShow.setSeatedPrice(seatedPrice);
                    newShow.setStandingPrice(standingPrice);

                    eventService.addShowToEvent(cachedEventId, newShow, mgr);
                    cachedShows.add(newShow);
                }

                refresh[0].run();
                dialog.close();

                Notification.show(isEdit ? "Show updated!" : "Show added!",
                        2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                UI.getCurrent().getPage().reload();

            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "#026cdf").set("color", "white").set("font-weight", "700");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Area fields are only relevant when adding a new show.
        // Editing cannot change areas because existing tickets reference their seats.
        VerticalLayout body;
        if (isEdit) {
            Span areaNote = new Span("ℹ Areas cannot be changed after a show is created.");
            areaNote.getStyle()
                    .set("font-size", "13px").set("color", "#888")
                    .set("font-style", "italic").set("margin-top", "4px");
            body = new VerticalLayout(
                    nameField, singerField, descField, datePicker,
                    showSectionLabel("Ticket Prices"),
                    standingPriceField, seatedPriceField,
                    areaNote);
        } else {
            body = new VerticalLayout(
                    nameField, singerField, descField, datePicker,
                    showSectionLabel("Standing Area"),
                    standingCapField, standingPriceField,
                    showSectionLabel("Seated Area"),
                    blocksField, rowsField, seatsField, seatedPriceField);
        }
        body.setPadding(true);
        body.setSpacing(true);
        body.setWidthFull();

        dialog.add(body);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    /** Confirmation dialog before deleting a show — warns about ticket count. */
    private void openDeleteShowConfirm(show s) {
        Long mgr = getManagerId();
        if (mgr == null) {
            error("Session expired");
            return;
        }

        int ticketCount = 0;
        try {
            if (s.getShowid() != null)
                ticketCount = eventService.countTicketsForShow(s.getShowid());
        } catch (Exception ignored) {
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete Show");

        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("gap", "12px");

        Paragraph msg = new Paragraph(
                "Are you sure you want to permanently delete \"" + nullSafe(s.getName()) + "\"?");
        msg.getStyle().set("margin", "0").set("font-weight", "600");

        body.add(msg);

        if (ticketCount > 0) {
            Paragraph warning = new Paragraph(
                    "⚠ This show has " + ticketCount + " ticket(s) that will also be deleted.");
            warning.getStyle()
                    .set("margin", "0")
                    .set("color", "#c62828")
                    .set("font-size", "14px");
            body.add(warning);
        }

        confirm.add(body);

        final int finalTicketCount = ticketCount;
        Button confirmBtn = new Button("Yes, Delete", e -> {
            try {
                eventService.removeShowFromEvent(cachedEventId, s, mgr);
                cachedShows.remove(s);
                confirm.close();
                Notification.show(
                        "Show deleted" + (finalTicketCount > 0
                                ? " along with " + finalTicketCount + " ticket(s)."
                                : "."),
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().getPage().reload();
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.getStyle().set("font-weight", "700");

        Button cancelBtn = new Button("Cancel", e -> confirm.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirm.getFooter().add(cancelBtn, confirmBtn);
        confirm.open();
    }

    /** Confirmation dialog before deleting the entire event. */
    private void openDeleteEventConfirm() {
        Long mgr = getManagerId();
        if (mgr == null) {
            error("Session expired");
            return;
        }

        // Count total tickets across all shows
        int totalTickets = 0;
        for (show s : cachedShows) {
            try {
                if (s.getShowid() != null)
                    totalTickets += eventService.countTicketsForShow(s.getShowid());
            } catch (Exception ignored) {
            }
        }
        int totalShows = cachedShows.size();

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete Event");

        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("gap", "12px");

        Paragraph msg = new Paragraph(
                "Are you sure you want to permanently delete \"" + nullSafe(cachedEvent.getName()) + "\"?");
        msg.getStyle().set("margin", "0").set("font-weight", "600");
        body.add(msg);

        if (totalShows > 0 || totalTickets > 0) {
            String detail = "⚠ This will also delete " + totalShows + " show(s)";
            if (totalTickets > 0)
                detail += " and " + totalTickets + " ticket(s)";
            detail += ". This action cannot be undone.";
            Paragraph warning = new Paragraph(detail);
            warning.getStyle()
                    .set("margin", "0")
                    .set("color", "#c62828")
                    .set("font-size", "14px");
            body.add(warning);
        }

        confirm.add(body);

        final int finalTickets = totalTickets;
        Button confirmBtn = new Button("Yes, Delete Event", e -> {
            try {
                eventService.deleteEvent(cachedEventId, mgr);
                confirm.close();
                Notification.show(
                        "Event deleted" + (finalTickets > 0
                                ? " along with " + finalTickets + " ticket(s)."
                                : "."),
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("company");
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.getStyle().set("font-weight", "700");

        Button cancelBtn = new Button("Cancel", e -> confirm.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirm.getFooter().add(cancelBtn, confirmBtn);
        confirm.open();
    }

    /** Builds Area objects from raw capacity / layout numbers. */
    private static List<Area> buildShowAreas(int standingCap,
            int numBlocks, int rowsPerBlock, int seatsPerRow) {
        List<Area> areas = new ArrayList<>();

        if (standingCap > 0)
            areas.add(new StandingArea("Standing Area", standingCap));

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
                    for (int s = 1; s <= seatsPerRow; s++)
                        seats.add(new Seat(idSeq++, String.valueOf(s), row));
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

    private static Span showSectionLabel(String text) {
        Span s = new Span(text);
        s.getStyle()
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("color", "#026cdf")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em")
                .set("margin-top", "8px");
        return s;
    }

    // ── Edit policies section ─────────────────────────────────────────────────

    private Div buildEditPoliciesSection() {
        Div section = new Div();
        section.setWidthFull();
        section.getStyle()
                .set("padding", "16px 0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        if (cachedEventId == null) {
            Span msg = new Span("No event loaded.");
            msg.getStyle().set("color", "#888").set("font-size", "14px");
            section.add(msg);
            return section;
        }

        Div listContainer = new Div();
        listContainer.setWidthFull();
        listContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px");

        Runnable[] refresh = { null };
        refresh[0] = () -> {
            listContainer.removeAll();
            List<Policy> policies;
            try {
                policies = policyRepo.findByEventId(cachedEventId);
            } catch (Exception ex) {
                Span errSpan = new Span("Could not load policies: " + ex.getMessage());
                errSpan.getStyle().set("color", "#c62828").set("font-size", "13px");
                listContainer.add(errSpan);
                return;
            }
            if (policies.isEmpty()) {
                Span empty = new Span("No policies have been defined for this event yet.");
                empty.getStyle().set("color", "#888").set("font-size", "14px").set("padding", "8px 0");
                listContainer.add(empty);
            } else {
                for (Policy p : policies)
                    listContainer.add(buildPolicyRow(p, refresh));
            }
        };
        refresh[0].run();

        Button addBtn = new Button("+ Add Policy", e -> openPolicyDialog(null, refresh));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("align-self", "flex-start").set("margin-top", "4px");

        section.add(listContainer, addBtn);
        return section;
    }

    private Div buildPolicyRow(Policy p, Runnable[] refresh) {
        Div row = new Div();
        row.setWidthFull();
        row.getStyle()
                .set("background", "#f8faff")
                .set("border", "1px solid #d0e4ff")
                .set("border-radius", "10px")
                .set("padding", "12px 16px")
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        // Left: type badge + text
        String[] colors = policyTypeColors(p);
        Span typeBadge = badge(policyTypeName(p), colors[0], colors[1]);

        Span descSpan = new Span(p.getDescription() != null ? p.getDescription() : "");
        descSpan.getStyle().set("font-weight", "600").set("font-size", "14px").set("color", "#111");

        Span summarySpan = new Span(policySummary(p));
        summarySpan.getStyle()
                .set("font-size", "12px").set("color", "#666")
                .set("display", "block").set("margin-top", "2px");

        Div textInfo = new Div(descSpan, summarySpan);

        Div info = new Div();
        info.getStyle().set("display", "flex").set("align-items", "center").set("gap", "12px");
        info.add(typeBadge, textInfo);

        // Right: Edit + Remove
        Button editBtn = new Button("Edit", e -> openPolicyDialog(p, refresh));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button removeBtn = new Button("Remove", ev -> {
            try {
                policyRepo.deleteByPolicyId(p.getPolicyId());
                refresh[0].run();
            } catch (Exception ex) {
                error("Could not remove: " + ex.getMessage());
            }
        });
        removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        Div actions = new Div(editBtn, removeBtn);
        actions.getStyle().set("display", "flex").set("gap", "4px").set("flex-shrink", "0");

        row.add(info, actions);
        return row;
    }

    private void openPolicyDialog(Policy existing, Runnable[] refresh) {
        boolean isEdit = existing != null;
        Dialog dialog = new Dialog();
        dialog.setWidth("520px");
        dialog.setHeaderTitle(isEdit ? "Edit Policy" : "Add Policy");

        // ── Type selector (locked when editing) ──
        ComboBox<String> typeBox = new ComboBox<>("Policy Type");
        typeBox.setItems("Selling", "Purchase", "Percentage Discount", "Coupon Discount");
        typeBox.setWidthFull();
        if (isEdit)
            typeBox.setEnabled(false);

        // ── Selling fields ──
        ComboBox<SellingType> sellingTypeBox = new ComboBox<>("Selling Type");
        sellingTypeBox.setItems(SellingType.values());
        sellingTypeBox.setItemLabelGenerator(t -> capitalize(t.name()));
        sellingTypeBox.setValue(SellingType.REGULAR);
        sellingTypeBox.setWidthFull();

        // ── Purchase fields ──
        IntegerField minAgeField = new IntegerField("Minimum Age");
        minAgeField.setMin(0);
        minAgeField.setPlaceholder("Leave empty = no restriction");
        minAgeField.setWidthFull();

        IntegerField minTicketsField = new IntegerField("Minimum Tickets per Purchase");
        minTicketsField.setMin(1);
        minTicketsField.setPlaceholder("Leave empty = no minimum");
        minTicketsField.setWidthFull();

        IntegerField maxTicketsField = new IntegerField("Maximum Tickets per Purchase");
        maxTicketsField.setMin(1);
        maxTicketsField.setPlaceholder("Leave empty = no limit");
        maxTicketsField.setWidthFull();

        // ── Percentage Discount fields ──
        NumberField percentageField = new NumberField("Percentage Off (0–100)");
        percentageField.setMin(0);
        percentageField.setMax(100);
        percentageField.setPlaceholder("e.g. 10");
        percentageField.setWidthFull();

        // ── Coupon Discount fields ──
        TextField couponCodeField = new TextField("Coupon Code");
        couponCodeField.setPlaceholder("e.g. SUMMER25");
        couponCodeField.setWidthFull();

        NumberField couponPctField = new NumberField("Percentage Off (0–100)");
        couponPctField.setMin(0);
        couponPctField.setMax(100);
        couponPctField.setPlaceholder("e.g. 25");
        couponPctField.setWidthFull();

        DatePicker couponExpiryPicker = new DatePicker("Expiry Date (optional)");
        couponExpiryPicker.setPlaceholder("Leave empty = never expires");
        couponExpiryPicker.setWidthFull();

        // ── Pre-fill when editing ──
        if (isEdit) {
            if (existing instanceof SellingPolicy sp) {
                typeBox.setValue("Selling");
                if (sp.getType() != null)
                    sellingTypeBox.setValue(sp.getType());
            } else if (existing instanceof PurchasePolicy pp) {
                typeBox.setValue("Purchase");
                extractMinAge(pp).ifPresent(minAgeField::setValue);
                extractMinTickets(pp).ifPresent(minTicketsField::setValue);
                extractMaxTickets(pp).ifPresent(maxTicketsField::setValue);
            } else if (existing instanceof DiscountPolicy dp) {
                if (dp.getRootRule() instanceof CouponDiscountRule cr) {
                    typeBox.setValue("Coupon Discount");
                    couponCodeField.setValue(cr.getCouponCode());
                    couponPctField.setValue(cr.getPercentage());
                    if (cr.getExpiry() != null)
                        couponExpiryPicker.setValue(cr.getExpiry().toLocalDate());
                } else {
                    typeBox.setValue("Percentage Discount");
                    if (dp.getRootRule() instanceof PercentageDiscountRule pdr)
                        percentageField.setValue(pdr.getPercentage());
                }
            }
        }

        // ── Dynamic form area (swaps fields based on type) ──
        Div formArea = new Div();
        formArea.setWidthFull();
        formArea.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px")
                .set("margin-top", "4px");

        Runnable updateForm = () -> {
            formArea.removeAll();
            String t = typeBox.getValue();
            if ("Selling".equals(t)) {
                formArea.add(sellingTypeBox);
            } else if ("Purchase".equals(t)) {
                formArea.add(
                        editSectionLabel("At least one rule required"),
                        minAgeField, minTicketsField, maxTicketsField);
            } else if ("Percentage Discount".equals(t)) {
                formArea.add(percentageField);
            } else if ("Coupon Discount".equals(t)) {
                formArea.add(
                        editSectionLabel("Coupon details"),
                        couponCodeField, couponPctField, couponExpiryPicker);
            }
        };
        typeBox.addValueChangeListener(e -> updateForm.run());
        updateForm.run();

        // ── Save handler ──
        Button saveBtn = new Button(isEdit ? "Save Changes" : "Add Policy", e -> {
            String t = typeBox.getValue();
            if (t == null || t.isBlank()) {
                error("Please select a policy type");
                return;
            }
            Object companyIdObj = UI.getCurrent().getSession().getAttribute("managingCompanyId");
            if (companyIdObj == null) {
                error("No company session — please log in again");
                return;
            }
            UUID companyId = UUID.fromString(companyIdObj.toString());
            try {
                if (isEdit)
                    policyRepo.deleteByPolicyId(existing.getPolicyId());

                switch (t) {
                    case "Selling" -> {
                        SellingType st = sellingTypeBox.getValue() != null
                                ? sellingTypeBox.getValue()
                                : SellingType.REGULAR;
                        policyRepo.savePolicy(new SellingPolicy(
                                Math.abs(UUID.randomUUID().hashCode()),
                                st.name() + " selling policy", st, cachedEventId, companyId));
                    }
                    case "Purchase" -> {
                        Integer minAge = minAgeField.getValue();
                        Integer minTix = minTicketsField.getValue();
                        Integer maxTix = maxTicketsField.getValue();
                        if (minAge == null && minTix == null && maxTix == null) {
                            error("At least one purchase restriction is required");
                            return;
                        }
                        PurchasePolicy pp = new PurchasePolicy(
                                Math.abs(UUID.randomUUID().hashCode()),
                                "Purchase restrictions", cachedEventId, companyId);
                        if (minAge != null && minAge >= 0)
                            pp.addRule(new MinAgeRule(minAge));
                        if (minTix != null && minTix > 0)
                            pp.addRule(new MinTicketsRule(minTix));
                        if (maxTix != null && maxTix > 0)
                            pp.addRule(new MaxTicketsRule(maxTix));
                        policyRepo.savePolicy(pp);
                    }
                    case "Percentage Discount" -> {
                        Double pct = percentageField.getValue();
                        if (pct == null || pct <= 0) {
                            error("Percentage must be greater than 0");
                            return;
                        }
                        DiscountPolicy dp = new DiscountPolicy(
                                Math.abs(UUID.randomUUID().hashCode()),
                                pct + "% discount", cachedEventId, companyId);
                        dp.addRule(new PercentageDiscountRule(pct, pct + "% discount"));
                        policyRepo.savePolicy(dp);
                    }
                    case "Coupon Discount" -> {
                        String code = couponCodeField.getValue();
                        if (code == null || code.isBlank()) {
                            error("Coupon code is required");
                            return;
                        }
                        Double pct = couponPctField.getValue();
                        if (pct == null || pct <= 0) {
                            error("Percentage must be greater than 0");
                            return;
                        }
                        java.time.LocalDateTime expiry = couponExpiryPicker.getValue() != null
                                ? couponExpiryPicker.getValue().atTime(23, 59, 59)
                                : null;
                        DiscountPolicy dp = new DiscountPolicy(
                                Math.abs(UUID.randomUUID().hashCode()),
                                "Coupon: " + code.trim().toUpperCase(), cachedEventId, companyId);
                        dp.addRule(new CouponDiscountRule(pct, code.trim().toUpperCase(), expiry));
                        policyRepo.savePolicy(dp);
                    }
                }

                refresh[0].run();
                dialog.close();
                Notification.show(isEdit ? "Policy updated" : "Policy added",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                error(ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "#026cdf").set("color", "white").set("font-weight", "700");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout body = new VerticalLayout(typeBox, formArea);
        body.setPadding(true);
        body.setSpacing(false);
        body.getStyle().set("gap", "8px");
        body.setWidthFull();

        dialog.add(body);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    // ── Policy display helpers ────────────────────────────────────────────────

    private static String policyTypeName(Policy p) {
        if (p instanceof SellingPolicy)
            return "SELLING";
        if (p instanceof PurchasePolicy)
            return "PURCHASE";
        if (p instanceof DiscountPolicy dp) {
            return dp.getRootRule() instanceof CouponDiscountRule ? "COUPON" : "DISCOUNT";
        }
        return "POLICY";
    }

    private static String[] policyTypeColors(Policy p) {
        if (p instanceof SellingPolicy)
            return new String[] { "#e3f2fd", "#026cdf" };
        if (p instanceof PurchasePolicy)
            return new String[] { "#e8f5e9", "#2e7d32" };
        if (p instanceof DiscountPolicy dp) {
            return dp.getRootRule() instanceof CouponDiscountRule
                    ? new String[] { "#f3e5f5", "#6a1b9a" } // purple for coupon
                    : new String[] { "#fff3e0", "#e65100" }; // orange for percentage
        }
        return new String[] { "#f5f5f5", "#555" };
    }

    private static String policySummary(Policy p) {
        if (p instanceof SellingPolicy sp)
            return "Selling type: " + (sp.getType() != null ? capitalize(sp.getType().name()) : "—");
        if (p instanceof PurchasePolicy pp)
            return summarizePurchaseRule(pp.getRootRule());
        if (p instanceof DiscountPolicy dp) {
            if (dp.getRootRule() instanceof CouponDiscountRule r) {
                String base = r.getPercentage() + "% off — code: \"" + r.getCouponCode() + "\"";
                return r.getExpiry() != null
                        ? base + "  (expires " + r.getExpiry().toLocalDate() + ")"
                        : base;
            }
            if (dp.getRootRule() instanceof PercentageDiscountRule r)
                return r.getPercentage() + "% off";
            return dp.getRootRule() != null ? dp.getRootRule().describe() : "No rules";
        }
        return p.getDescription() != null ? p.getDescription() : "";
    }

    private static String summarizePurchaseRule(PurchaseRule rule) {
        if (rule == null)
            return "No restrictions";
        if (rule instanceof MinAgeRule r)
            return "Min age: " + r.getMinimumAge();
        if (rule instanceof MinTicketsRule r)
            return "Min tickets: " + r.getMinTickets();
        if (rule instanceof MaxTicketsRule r)
            return "Max tickets: " + r.getMaxTickets();
        if (rule instanceof CompositePurchaseRule c)
            return c.getRules().stream()
                    .map(EventDetailsView::summarizePurchaseRule)
                    .collect(java.util.stream.Collectors.joining("  •  "));
        return rule.describe();
    }

    private static Optional<Integer> extractMinAge(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MinAgeRule.class).map(MinAgeRule::getMinimumAge);
    }

    private static Optional<Integer> extractMinTickets(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MinTicketsRule.class).map(MinTicketsRule::getMinTickets);
    }

    private static Optional<Integer> extractMaxTickets(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MaxTicketsRule.class).map(MaxTicketsRule::getMaxTickets);
    }

    @SuppressWarnings("unchecked")
    private static <T extends PurchaseRule> Optional<T> extractPurchaseRule(PurchaseRule root, Class<T> type) {
        if (root == null)
            return Optional.empty();
        if (type.isInstance(root))
            return Optional.of((T) root);
        if (root instanceof CompositePurchaseRule c) {
            for (PurchaseRule r : c.getRules()) {
                Optional<T> found = extractPurchaseRule(r, type);
                if (found.isPresent())
                    return found;
            }
        }
        return Optional.empty();
    }

    private static Span editSectionLabel(String text) {
        Span s = new Span(text);
        s.getStyle()
                .set("font-weight", "700").set("font-size", "12px").set("color", "#026cdf")
                .set("text-transform", "uppercase").set("letter-spacing", "0.05em")
                .set("margin-top", "4px");
        return s;
    }

    private static void error(String msg) {
        Notification.show(msg, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // ── Shows card ───────────────────────────────────────────────────────────

    private Div buildShowsCard(List<show> shows) {
        Div card = card();

        Div titleRow = new Div();
        titleRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-bottom", "16px");

        H2 title = new H2("Shows");
        title.getStyle().set("margin", "0").set("font-size", "20px").set("color", "#111");
        titleRow.add(title);

        if (isManagerOrOwner()) {
            Button addShowBtn = new Button("+ Add Show", e -> {
                Runnable[] refresh = { () -> UI.getCurrent().getPage().reload() };
                openEditShowDialog(null, refresh, null);
            });
            addShowBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addShowBtn.getStyle()
                    .set("background", "#026cdf")
                    .set("color", "white")
                    .set("font-weight", "700");
            titleRow.add(addShowBtn);
        }

        card.add(titleRow);

        if (shows == null || shows.isEmpty()) {
            Paragraph empty = new Paragraph("No shows have been added to this event yet.");
            empty.getStyle().set("color", "#888");
            card.add(empty);
            return card;
        }

        Grid<show> grid = new Grid<>(show.class, false);
        grid.addColumn(s -> nullSafe(s.getName())).setHeader("Show Name").setFlexGrow(2);
        grid.addColumn(s -> nullSafe(s.getSinger())).setHeader("Singer / Performer").setFlexGrow(2);
        grid.addColumn(s -> s.getShowDate() != null ? DATE_FMT.format(s.getShowDate()) : "—")
                .setHeader("Date").setFlexGrow(1);
        grid.addColumn(s -> {
            if (cachedEventId == null || s.getShowid() == null) {
                return "—";
            }
            try {
                show fullShow = eventService.loadShowFully(cachedEventId, s.getShowid());
                Map<Long, Boolean> seatAvailability = ticketService.getSeatAvailability(s.getShowid());

                int total = 0;
                int available = 0;

                if (fullShow.getAreas() != null) {
                    for (Area area : fullShow.getAreas()) {
                        SeatCount count = countAreaSeats(s.getShowid(), area, seatAvailability);
                        total += count.total();
                        available += count.available();
                    }
                }
                return total == 0 ? "—" : available + " / " + total;

            } catch (Exception ex) {
                return "—";
            }
            // List<Area> areas = showAreasCache.getOrDefault(s.getShowid(), List.of());
            // int total = areas.stream().mapToInt(EventDetailsView::areaTotal).sum();
            // int avail = areas.stream().mapToInt(EventDetailsView::areaAvailable).sum();
            // return areas.isEmpty() ? "—" : avail + " / " + total;
        }).setHeader("Available Seats").setFlexGrow(1);
        grid.addComponentColumn(s -> {
            Div actions = new Div();
            actions.getStyle().set("display", "flex").set("gap", "6px").set("align-items", "center");

            Button detailsBtn = new Button("Details", e -> openShowDetailsDialog(s));
            detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            actions.add(detailsBtn);

            Button seatBtn = new Button("Select Seat", e -> openSeatDialog(s));
            seatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(seatBtn);

            if (isManagerOrOwner()) {
                Runnable[] refresh = { () -> UI.getCurrent().getPage().reload() };

                Button editBtn = new Button("Edit", e -> openEditShowDialog(s, refresh, null));
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.getStyle().set("color", "#026cdf");

                Button deleteBtn = new Button("Delete", e -> openDeleteShowConfirm(s));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

                actions.add(editBtn, deleteBtn);
            }

            return actions;
        }).setHeader("").setAutoWidth(true);

        grid.setItems(shows);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        card.add(grid);
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
                dialogRow("Name", nullSafe(s.getName())),
                dialogRow("Singer", nullSafe(s.getSinger())),
                dialogRow("Date", s.getShowDate() != null ? DATE_FMT.format(s.getShowDate()) : "—"),
                dialogRow("Description", nullSafe(s.getDescription())),
                dialogRow("Show ID", s.getShowid() != null ? s.getShowid().toString() : "—"));

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
        if (area instanceof StandingArea sa)
            return sa.getMaxCapacity();
        if (area instanceof SeatedArea)
            return -1; // blocks×rows×seats not available without deep fetch
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
        Event ev = new Event("Summer Music Festival 2025", show_type.FESTIVAL,
                UUID.fromString("00000000-0000-0000-0000-000000000001"), 1L);
        ev.editVenue("BGU Amphitheatre, Beer-Sheva", 1L);
        ev.editDescription(
                "A three-day outdoor festival featuring top local and international artists across multiple stages.",
                1L);
        try {
            ev.editDates(
                    new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-10"),
                    new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-12"),
                    1L);
        } catch (Exception ignored) {
        }
        return ev;
    }

    private static show mockShow() {
        try {
            Date showDate = new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-10");
            show s = new show(null, "Opening Night",
                    "An unforgettable opening with fireworks and live music.",
                    "The Midnight", showDate);

            StandingArea floor = new StandingArea("Floor GA", 500);
            StandingArea vip = new StandingArea("VIP Standing", 80);
            SeatedArea seated = new SeatedArea("Seated Balcony", 4);
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
        if (s == null || s.isEmpty())
            return s;
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

    // ── Seat / ticket selection dialog (multi-ticket + cart) ─────────────────

    private void openSeatDialog(show s) {
        List<AreaInfo> areas;
        if (cachedEventId != null && s.getShowid() != null) {
            try {
                show full = eventService.loadShowFully(cachedEventId, s.getShowid());
                Map<Long, Boolean> avail = ticketService.getSeatAvailability(s.getShowid());
                areas = new ArrayList<>();
                if (full.getAreas() != null)
                    for (Area a : full.getAreas())
                        areas.add(toAreaInfo(a, s.getShowid(), avail));
            } catch (Exception ex) {
                Notification.show("Could not load seat data: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE);
                return;
            }
        } else {
            areas = mockAreaInfoList();
        }

        // ── Shared cart state ──
        List<CartEntry> cart = new ArrayList<>();
        Set<Long> selectedSeatIds = new LinkedHashSet<>();
        Runnable[] cartRefresh = { null };

        Dialog dialog = new Dialog();
        dialog.setWidth("980px");
        dialog.setMaxHeight("92vh");
        dialog.setHeaderTitle("Select Tickets — " + nullSafe(s.getName()));

        // Left: wizard; Right: cart panel
        Div stepContent = new Div();
        stepContent.getStyle()
                .set("flex", "1").set("min-height", "320px").set("overflow-y", "auto");

        Div cartPanel = buildCartPanel(cart, selectedSeatIds, s, dialog, cartRefresh);

        Div bodyRow = new Div(stepContent, cartPanel);
        bodyRow.getStyle()
                .set("display", "flex").set("gap", "20px")
                .set("align-items", "flex-start")
                .set("padding", "8px 16px 16px 16px")
                .set("width", "100%").set("box-sizing", "border-box");

        dialog.add(bodyRow);
        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeBtn);

        showAreaStep(dialog, stepContent, areas, s, cart, selectedSeatIds, cartRefresh);
        dialog.open();
    }

    // ── Cart panel ────────────────────────────────────────────────────────────

    private Div buildCartPanel(List<CartEntry> cart, Set<Long> selectedSeatIds,
            show s, Dialog parentDialog, Runnable[] cartRefreshRef) {
        Div panel = new Div();
        panel.getStyle()
                .set("width", "290px").set("flex-shrink", "0")
                .set("background", "#f8faff").set("border", "1px solid #d0e4ff")
                .set("border-radius", "12px").set("padding", "16px")
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "8px").set("align-self", "stretch");

        Span title = new Span("🛒  Cart");
        title.getStyle().set("font-weight", "700").set("font-size", "15px").set("color", "#111");

        Div itemList = new Div();
        itemList.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "6px").set("flex", "1").set("min-height", "80px");

        Div divider = new Div();
        divider.getStyle().set("border-top", "1px solid #d0e4ff").set("margin", "4px 0");

        Span totalSpan = new Span("Total: $0.00");
        totalSpan.getStyle().set("font-weight", "700").set("font-size", "14px").set("color", "#111");

        Button checkoutBtn = new Button("Proceed to Checkout →", e -> {
            if (cart.isEmpty()) {
                error("Add at least one ticket first");
                return;
            }
            if (cachedUserId == null) {
                Notification.show("Please log in to checkout",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            List<String> ticketIds = new ArrayList<>();
            List<SeatRequest> seatRequests = new ArrayList<>();

            List<java.util.Map<String, String>> checkoutItems = new ArrayList<>();
            int checker = 0;
            try {
                Object tokenObj = UI.getCurrent().getSession().getAttribute("token");
                if (tokenObj == null || tokenObj.toString().isBlank()) {
                    Notification.show("Please log in to checkout",
                            3000,
                            Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                String token = tokenObj.toString();
                checker++;
                for (CartEntry entry : cart) {
                    java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                    item.put("description", entry.description());
                    item.put("unitPrice", entry.unitPrice().toPlainString());
                    item.put("quantity", String.valueOf(entry.quantity()));
                    checkoutItems.add(item);
                    if (entry.isSeated()) {
                        ticket t = eventService.reserveSeat(
                                cachedEventId, s.getShowid(), entry.areaId(), entry.seatId(), cachedUserId);
                        ticketIds.add(t.getTicketId().toString());
                        seatRequests.add(new SeatRequest(
                                t.getTicketId().toString(),
                                entry.seatId(),
                                entry.areaId(),
                                entry.unitPrice()));
                    } else {
                        for (int i = 0; i < entry.quantity(); i++) {
                            ticket t = eventService.reserveStanding(
                                    cachedEventId, s.getShowid(), entry.areaId(), cachedUserId);
                            ticketIds.add(t.getTicketId().toString());
                            seatRequests.add(new SeatRequest(
                                    t.getTicketId().toString(),
                                    null,
                                    entry.areaId(),
                                    entry.unitPrice()));
                        }
                    }
                }
                checker++;
                OrderDTO order = orderService.reserveTickets(token, cachedEventId, seatRequests);
                checker++;
                var session = UI.getCurrent().getSession();
                session.setAttribute("checkoutOrderId", order.getOrderId().toString());

                checker++;
                session.setAttribute("checkoutTicketIds", ticketIds);
                checker++;
                session.setAttribute("checkoutItems", checkoutItems);
                checker++;
                session.setAttribute("checkoutUserId", cachedUserId.toString());
                checker++;
                session.setAttribute("checkoutShowName", nullSafe(s.getName()));
                checker++;
                session.setAttribute("checkoutEventId", cachedEventId != null ? cachedEventId.toString() : "");
                checker++;
                parentDialog.close();
                UI.getCurrent().navigate("checkout");
            } catch (RuntimeException ex) {
                error("Reservation failed: " + ex.getMessage() + " (step " + checker + ")");
            }
        });
        checkoutBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkoutBtn.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("margin-top", "4px");
        checkoutBtn.setEnabled(false);
        checkoutBtn.setWidthFull();

        cartRefreshRef[0] = () -> {
            itemList.removeAll();
            if (cart.isEmpty()) {
                Span empty = new Span("No tickets selected yet.");
                empty.getStyle().set("color", "#aaa").set("font-size", "13px").set("padding", "8px 0");
                itemList.add(empty);
                totalSpan.setText("Total: $0.00");
                checkoutBtn.setEnabled(false);
            } else {
                java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                for (CartEntry entry : cart) {
                    itemList.add(buildCartRow(entry, cart, selectedSeatIds, cartRefreshRef));
                    total = total.add(entry.total());
                }
                totalSpan.setText("Total: $" + total.setScale(2, java.math.RoundingMode.HALF_UP));
                checkoutBtn.setEnabled(true);
            }
        };
        cartRefreshRef[0].run();

        panel.add(title, itemList, divider, totalSpan, checkoutBtn);
        return panel;
    }

    private Div buildCartRow(CartEntry entry, List<CartEntry> cart,
            Set<Long> selectedSeatIds, Runnable[] cartRefresh) {
        Div row = new Div();
        row.getStyle()
                .set("background", "white").set("border-radius", "8px")
                .set("padding", "8px 10px").set("font-size", "13px")
                .set("display", "flex").set("justify-content", "space-between")
                .set("align-items", "flex-start").set("gap", "6px");

        Div info = new Div();
        Span desc = new Span(entry.description());
        desc.getStyle().set("font-weight", "600").set("display", "block").set("color", "#111");
        String priceText = "$" + entry.unitPrice().toPlainString()
                + (entry.quantity() > 1
                        ? " × " + entry.quantity() + " = $"
                                + entry.total().setScale(2, java.math.RoundingMode.HALF_UP)
                        : "");
        Span price = new Span(priceText);
        price.getStyle().set("font-size", "12px").set("color", "#666");
        info.add(desc, price);

        Button removeBtn = new Button("✕", e -> {
            if (entry.isSeated() && entry.seatId() != null)
                selectedSeatIds.remove(entry.seatId());
            cart.remove(entry);
            cartRefresh[0].run();
        });
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeBtn.getStyle().set("min-width", "0").set("padding", "0 6px");

        row.add(info, removeBtn);
        return row;
    }

    // ── Wizard steps ──────────────────────────────────────────────────────────

    private void showAreaStep(Dialog dialog, Div stepContent, List<AreaInfo> areas, show s,
            List<CartEntry> cart, Set<Long> selectedSeatIds, Runnable[] cartRefresh) {
        stepContent.removeAll();
        if (areas.isEmpty()) {
            Div msg = new Div();
            msg.getStyle().set("padding", "24px 0").set("color", "#888")
                    .set("font-size", "14px").set("text-align", "center");
            msg.add(new Span("No areas have been configured for this show yet."));
            stepContent.add(msg);
            return;
        }
        if (areas.size() == 1 && areas.get(0).isSeated()) {
            showBlockStep(dialog, stepContent, areas.get(0), areas, s, cart, selectedSeatIds, cartRefresh);
            return;
        }
        stepContent.add(stepLabel("Choose an Area"));
        Div grid = new Div();
        grid.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "10px");
        for (AreaInfo ai : areas) {
            if (ai.isSeated()) {
                Button btn = new Button(ai.name() + " (Seated)",
                        e -> showBlockStep(dialog, stepContent, ai, areas, s, cart, selectedSeatIds, cartRefresh));
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                btn.setWidthFull();
                grid.add(btn);
            } else {
                java.math.BigDecimal stPrice = s.getStandingPrice() != null
                        ? s.getStandingPrice()
                        : new java.math.BigDecimal("30.00");
                grid.add(buildStandingAreaCard(ai, cart, cartRefresh, stPrice));
            }
        }
        stepContent.add(grid);
    }

    private void showBlockStep(Dialog dialog, Div stepContent,
            AreaInfo area, List<AreaInfo> areas, show s,
            List<CartEntry> cart, Set<Long> selectedSeatIds, Runnable[] cartRefresh) {
        stepContent.removeAll();
        if (areas.size() > 1)
            stepContent.add(backBtn("← Areas",
                    () -> showAreaStep(dialog, stepContent, areas, s, cart, selectedSeatIds, cartRefresh)));
        stepContent.add(stepLabel(area.name() + " — Select a Block"));

        Div grid = new Div();
        grid.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(140px, 1fr))").set("gap", "12px");

        for (int i = 0; i < area.blocks().size(); i++) {
            BlockData block = area.blocks().get(i);
            String color = BLOCK_COLORS[i % BLOCK_COLORS.length];
            long total = block.rows().stream().mapToLong(r -> r.seats().size()).sum();
            long avail = block.rows().stream().flatMap(r -> r.seats().stream())
                    .filter(SeatData::available).count();
            long selected = block.rows().stream().flatMap(r -> r.seats().stream())
                    .filter(sd -> selectedSeatIds.contains(sd.id())).count();

            Div card = new Div();
            card.getStyle().set("background", color).set("color", "white")
                    .set("border-radius", "12px").set("padding", "18px 10px")
                    .set("text-align", "center").set("cursor", "pointer")
                    .set("font-weight", "700").set("font-size", "15px").set("user-select", "none");
            Span nameSpan = new Span("Block " + block.label());
            Span availSpan = new Span(avail + "/" + total + " avail");
            availSpan.getStyle().set("font-size", "11px").set("opacity", "0.85")
                    .set("display", "block").set("margin-top", "4px").set("font-weight", "400");
            card.add(nameSpan, availSpan);
            if (selected > 0) {
                Span selSpan = new Span("✓ " + selected + " in cart");
                selSpan.getStyle().set("font-size", "10px").set("display", "block")
                        .set("margin-top", "2px").set("color", "#ffe082");
                card.add(selSpan);
            }
            final BlockData b = block;
            card.addClickListener(
                    e -> showRowStep(dialog, stepContent, area, b, areas, s, cart, selectedSeatIds, cartRefresh));
            grid.add(card);
        }
        stepContent.add(grid);
    }

    private void showRowStep(Dialog dialog, Div stepContent,
            AreaInfo area, BlockData block, List<AreaInfo> areas, show s,
            List<CartEntry> cart, Set<Long> selectedSeatIds, Runnable[] cartRefresh) {
        stepContent.removeAll();
        stepContent.add(backBtn("← Blocks",
                () -> showBlockStep(dialog, stepContent, area, areas, s, cart, selectedSeatIds, cartRefresh)));
        stepContent.add(stepLabel("Block " + block.label() + " — Select a Row"));

        Div rowFlex = new Div();
        rowFlex.getStyle().set("display", "flex").set("flex-wrap", "wrap")
                .set("gap", "8px").set("margin-top", "8px");
        for (RowData row : block.rows()) {
            long avail = row.seats().stream().filter(SeatData::available).count();
            long selected = row.seats().stream().filter(sd -> selectedSeatIds.contains(sd.id())).count();
            String lbl = "Row " + row.label() + "  (" + avail + " free"
                    + (selected > 0 ? ", ✓ " + selected : "") + ")";
            Button btn = new Button(lbl);
            if (avail > 0 || selected > 0) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                final RowData r = row;
                btn.addClickListener(e -> showSeatStep(dialog, stepContent, area, block, r, areas, s, cart,
                        selectedSeatIds, cartRefresh));
            } else {
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                btn.setEnabled(false);
            }
            rowFlex.add(btn);
        }
        stepContent.add(rowFlex);
    }

    private void showSeatStep(Dialog dialog, Div stepContent,
            AreaInfo area, BlockData block, RowData row, List<AreaInfo> areas, show s,
            List<CartEntry> cart, Set<Long> selectedSeatIds, Runnable[] cartRefresh) {
        stepContent.removeAll();
        stepContent.add(backBtn("← Rows",
                () -> showRowStep(dialog, stepContent, area, block, areas, s, cart, selectedSeatIds, cartRefresh)));
        stepContent.add(stepLabel("Block " + block.label() + " › Row " + row.label()
                + " — Click to select / deselect"));

        Div legend = new Div();
        legend.getStyle().set("display", "flex").set("gap", "16px")
                .set("margin", "6px 0 14px 0").set("font-size", "13px").set("align-items", "center");
        legend.add(legendDot("#4caf50", "Available"), legendDot("#ef5350", "Taken"),
                legendDot("#026cdf", "Selected"));
        stepContent.add(legend);

        Div seatFlex = new Div();
        seatFlex.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "8px");

        for (SeatData seat : row.seats()) {
            boolean isSelected = selectedSeatIds.contains(seat.id());
            String bg = !seat.available() ? "#ef5350" : isSelected ? "#026cdf" : "#4caf50";

            Div circle = new Div();
            circle.getStyle()
                    .set("width", "44px").set("height", "44px").set("border-radius", "50%")
                    .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                    .set("font-size", "12px").set("font-weight", "700").set("color", "white")
                    .set("background", bg)
                    .set("cursor", seat.available() ? "pointer" : "default")
                    .set("user-select", "none").set("transition", "background 0.15s");
            circle.add(new Span(seat.label()));

            if (seat.available()) {
                circle.addClickListener(e -> {
                    e.getSource().getElement().executeJs("event.stopPropagation();");

                    if (selectedSeatIds.contains(seat.id())) {
                        selectedSeatIds.remove(seat.id());

                        cart.removeIf(en -> en.isSeated()
                                && en.seatId() != null
                                && en.seatId().equals(seat.id()));

                        circle.getStyle().set("background", "#4caf50");
                    } else {
                        selectedSeatIds.add(seat.id());

                        String desc = "Seated — Block " + block.label()
                                + ", Row " + row.label()
                                + ", Seat " + seat.label();

                        java.math.BigDecimal seatPrice = s.getSeatedPrice() != null
                                ? s.getSeatedPrice()
                                : new java.math.BigDecimal("50.00");
                        cart.add(new CartEntry(
                                desc,
                                seatPrice,
                                1,
                                true,
                                area.id(),
                                seat.id()));

                        circle.getStyle().set("background", "#026cdf");
                    }

                    if (cartRefresh[0] != null) {
                        cartRefresh[0].run();
                    }
                });
            }
            seatFlex.add(circle);
        }
        stepContent.add(seatFlex);
    }

    private Div buildStandingAreaCard(AreaInfo ai, List<CartEntry> cart, Runnable[] cartRefresh,
            java.math.BigDecimal standingPrice) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#f0f4ff").set("border-radius", "10px")
                .set("padding", "14px 18px").set("display", "flex")
                .set("justify-content", "space-between").set("align-items", "center").set("gap", "12px");

        Div info = new Div();
        Span name = new Span(ai.name());
        name.getStyle().set("font-weight", "700").set("font-size", "14px");
        boolean hasSpots = ai.standingAvail() > 0;
        int maxQty = Math.max(1, Math.min(ai.standingAvail(), 20));
        String priceLabel = "$" + standingPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        Span cap = new Span(ai.standingAvail() + " / " + ai.standingMax() + " spots — " + priceLabel + " each");
        cap.getStyle().set("font-size", "12px").set("color", hasSpots ? "#2e7d32" : "#c62828")
                .set("display", "block");
        info.add(name, cap);

        Div controls = new Div();
        controls.getStyle().set("display", "flex").set("align-items", "center").set("gap", "8px");

        IntegerField qtyField = new IntegerField();
        qtyField.setMin(1);
        qtyField.setMax(maxQty);
        qtyField.setValue(1);
        qtyField.setStepButtonsVisible(true);
        qtyField.setWidth("96px");
        qtyField.setEnabled(hasSpots);

        Button addBtn = new Button("Add to Cart", e -> {
            int qty = qtyField.getValue() != null ? qtyField.getValue() : 1;
            boolean merged = false;
            for (int i = 0; i < cart.size(); i++) {
                CartEntry ex = cart.get(i);
                if (!ex.isSeated() && ai.id().equals(ex.areaId())) {
                    cart.set(i, new CartEntry(ex.description(), ex.unitPrice(),
                            ex.quantity() + qty, false, ex.areaId(), null));
                    merged = true;
                    break;
                }
            }
            if (!merged)
                cart.add(new CartEntry(ai.name() + " (Standing)",
                        standingPrice, qty, false, ai.id(), null));
            cartRefresh[0].run();
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setEnabled(hasSpots);
        controls.add(qtyField, addBtn);

        card.add(info, controls);
        return card;
    }

    // ── Area data conversion ──────────────────────────────────────────────────

    // private AreaInfo toAreaInfo(Area area, Map<Long, Boolean> seatAvail) {
    // if (area instanceof SeatedArea sa) {
    // List<BlockData> blocks = new ArrayList<>();
    // for (Block block : sa.getBlocks()) {
    // List<RowData> rows = new ArrayList<>();
    // for (Row row : block.getRows()) {
    // List<SeatData> seats = new ArrayList<>();
    // for (Seat seat : row.getSeats()) {
    // boolean avail = seatAvail.getOrDefault(seat.getId(), true);
    // seats.add(new SeatData(seat.getId(), seat.getSeatNumber(), avail));
    // }
    // rows.add(new RowData(row.getId(), row.getRowNumber(), seats));
    // }
    // blocks.add(new BlockData(block.getId(), block.getBlockIdentifier(), rows));
    // }
    // return new AreaInfo(area.getId(), area.getName(), true, 0, 0, blocks);
    // } else if (area instanceof StandingArea sa) {
    // int max = sa.getMaxCapacity();
    // int current = 0;
    // try {
    // current = sa.getCurrentCapacity();
    // } catch (Exception ignored) {
    // }
    // return new AreaInfo(area.getId(), area.getName(), false, max, max - current,
    // List.of());
    // }
    // return new AreaInfo(area.getId(), area.getName(), false, 0, 0, List.of());
    // }

    private AreaInfo toAreaInfo(Area area, UUID showId, Map<Long, Boolean> seatAvail) {
        if (area instanceof SeatedArea sa) {
            List<BlockData> blocks = new ArrayList<>();

            for (Block block : sa.getBlocks()) {
                List<RowData> rows = new ArrayList<>();

                for (Row row : block.getRows()) {
                    List<SeatData> seats = new ArrayList<>();

                    for (Seat seat : row.getSeats()) {
                        boolean available = seatAvail.getOrDefault(seat.getId(), true);
                        seats.add(new SeatData(seat.getId(), seat.getSeatNumber(), available));
                    }

                    rows.add(new RowData(row.getId(), row.getRowNumber(), seats));
                }

                blocks.add(new BlockData(block.getId(), block.getBlockIdentifier(), rows));
            }

            return new AreaInfo(area.getId(), area.getName(), true, 0, 0, blocks);
        }

        if (area instanceof StandingArea sa) {
            int max = sa.getMaxCapacity();
            int available = ticketService.getStandingAvailableCount(showId, area.getId(), max);

            return new AreaInfo(
                    area.getId(),
                    area.getName(),
                    false,
                    max,
                    available,
                    List.of());
        }

        return new AreaInfo(area.getId(), area.getName(), false, 0, 0, List.of());
    }

    private static List<AreaInfo> mockAreaInfoList() {
        Random rnd = new Random(42);
        List<BlockData> blocks = new ArrayList<>();
        long sid = 1;
        String[] labels = { "A", "B", "C" };
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
                new AreaInfo(UUID.randomUUID(), "Floor GA (Standing)", false, 300, 175, List.of()));
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

    private record SeatCount(int total, int available) {
    }

    private SeatCount countAreaSeats(UUID showId, Area area, Map<Long, Boolean> seatAvailability) {
        if (area instanceof StandingArea standingArea) {
            int total = standingArea.getMaxCapacity();
            int available = ticketService.getStandingAvailableCount(showId, area.getId(), total);

            return new SeatCount(total, available);
        }

        if (area instanceof SeatedArea seatedArea) {
            int total = 0;
            int available = 0;

            for (Block block : seatedArea.getBlocks()) {
                if (block.getRows() == null) {
                    continue;
                }

                for (Row row : block.getRows()) {
                    if (row.getSeats() == null) {
                        continue;
                    }

                    for (Seat seat : row.getSeats()) {
                        total++;

                        boolean isAvailable = seatAvailability.getOrDefault(seat.getId(), true);
                        if (isAvailable) {
                            available++;
                        }
                    }
                }
            }

            return new SeatCount(total, available);
        }

        return new SeatCount(0, 0);
    }
}
