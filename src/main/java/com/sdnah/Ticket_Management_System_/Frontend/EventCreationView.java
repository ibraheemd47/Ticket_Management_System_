package com.sdnah.Ticket_Management_System_.Frontend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
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
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.EventCreationPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
@Route("event-create")
public class EventCreationView extends VerticalLayout
        implements BeforeEnterObserver, EventCreationPresenter.View {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object token = UI.getCurrent().getSession().getAttribute("token");
        if (token == null || token.toString().startsWith("GUEST_")) event.forwardTo(LoginView.class);
    }

    private final EventCreationPresenter presenter;

    private final List<ShowDTO> shows = new ArrayList<>();
    private Div showsContainer;

    // Event fields
    private TextField nameField;
    private TextField venueField;
    private ComboBox<show_type> typeBox;
    private DateTimePicker startDatePicker;
    private DateTimePicker endDatePicker;

    // Policy fields — selling
    private ComboBox<SellingType> sellingTypeBox;
    // — lottery (shown only when LOTTERY selling type is selected)
    private DateTimePicker lotteryDeadlinePicker;
    private DateTimePicker lotteryDrawTimePicker;
    private Div lotteryFieldsDiv;
    // — purchase
    private IntegerField minAgeField;
    private IntegerField minTicketsField;
    private IntegerField maxTicketsField;
    // — percentage discount
    private NumberField percentageField;
    // — coupon discount
    private TextField couponCodeField;
    private NumberField couponPctField;
    private DatePicker couponExpiryPicker;

    public EventCreationView(EventCreationPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);

        // ── Single action button at the bottom ──────────────────────────────
        Button createBtn = new Button("Create Event");
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("padding", "10px 36px")
                .set("font-size", "16px");
        createBtn.addClickListener(e -> handleCreate());

        Button cancelBtn = new Button("Cancel", e -> UI.getCurrent().navigate("company"));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(createBtn, cancelBtn);
        actions.getStyle()
                .set("max-width", "760px")
                .set("margin", "0 auto 48px auto")
                .set("width", "100%");

        VerticalLayout content = new VerticalLayout(buildEventCard(), buildShowsCard(), buildPoliciesCard());
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("max-width", "760px").set("margin", "0 auto").set("width", "100%");

        add(buildHeader(), content, actions);
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
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));
        logo.getStyle().set("cursor", "pointer");

        Div nav = new Div();
        nav.getStyle().set("display", "flex").set("gap", "32px").set("align-items", "center");
        nav.add(
                clickable("Home",          () -> UI.getCurrent().navigate("main")),
                clickable("← Company",    () -> UI.getCurrent().navigate("company")),
                clickable("👤 My Account", () -> UI.getCurrent().navigate("profile")));
        header.add(logo, nav);
        return header;
    }

    // ── Event card ───────────────────────────────────────────────────────────

    private Div buildEventCard() {
        Div card = card();

        H1 title = new H1("Create New Event");
        title.getStyle().set("margin", "0 0 24px 0").set("font-size", "26px").set("color", "#111");

        nameField = new TextField("Event Name");
        nameField.setPlaceholder("e.g. Summer Concert 2025");
        nameField.setWidthFull();

        venueField = new TextField("Venue / Location");
        venueField.setPlaceholder("e.g. BGU Auditorium, Tel Aviv Hall");
        venueField.setWidthFull();

        typeBox = new ComboBox<>("Event Type");
        typeBox.setItems(show_type.values());
        typeBox.setItemLabelGenerator(t -> capitalize(t.name()));
        typeBox.setWidthFull();

        startDatePicker = new DateTimePicker("Start Date and Time");
        startDatePicker.setWidthFull();
        startDatePicker.setStep(java.time.Duration.ofMinutes(15));

        endDatePicker = new DateTimePicker("End Date and Time");
        endDatePicker.setWidthFull();
        endDatePicker.setStep(java.time.Duration.ofMinutes(15));

        startDatePicker.addValueChangeListener(e -> {
            LocalDateTime start = e.getValue();
            if (start != null) {
                endDatePicker.setMin(start);
                if (endDatePicker.getValue() != null && endDatePicker.getValue().isBefore(start))
                    endDatePicker.clear();
            }
        });
        endDatePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) startDatePicker.setMax(e.getValue());
        });

        card.add(title, nameField, venueField, typeBox, startDatePicker, endDatePicker);
        return card;
    }

    // ── Shows card ───────────────────────────────────────────────────────────

    private Div buildShowsCard() {
        Div card = card();

        H2 title = new H2("Shows");
        title.getStyle().set("margin", "0 0 16px 0").set("font-size", "20px").set("color", "#111");

        showsContainer = new Div();
        showsContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "100%");

        refreshShowCards();

        Button addShowBtn = new Button("+ Add Show", e -> openShowDialog(null));
        addShowBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addShowBtn.getStyle().set("margin-top", "4px").set("align-self", "flex-start");

        card.add(title, showsContainer, addShowBtn);
        return card;
    }

    private Div buildShowCard(ShowDTO s) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#f8faff")
                .set("border", "1px solid #d0e4ff")
                .set("border-radius", "12px")
                .set("padding", "16px 20px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        Div topRow = new Div();
        topRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-bottom", "10px");

        Span nameSpan = new Span(s.name != null ? s.name : "Untitled Show");
        nameSpan.getStyle().set("font-weight", "700").set("font-size", "16px").set("color", "#111");

        Button editBtn = new Button("Edit", e -> openShowDialog(s));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editBtn.getStyle().set("margin-right", "4px");

        Button deleteBtn = new Button("Delete", e -> {
            shows.remove(s);
            refreshShowCards();
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        Div actions = new Div(editBtn, deleteBtn);
        topRow.add(nameSpan, actions);

        Div details = new Div();
        details.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "4px 24px")
                .set("font-size", "14px")
                .set("color", "#555");

        details.add(
                showInfoRow("Singer", s.singer != null ? s.singer : "—"),
                showInfoRow("Date", s.showDate != null ? s.showDate.toString() : "—")
        );

        String seatsText = buildSeatsText(s);
        if (!seatsText.equals("—")) {
            Div seatsRow = showInfoRow("Seats", seatsText);
            seatsRow.getStyle().set("grid-column", "1 / -1");
            details.add(seatsRow);
        }

        String priceText = buildPriceText(s);
        if (!priceText.isEmpty()) {
            Div priceRow = showInfoRow("Prices", priceText);
            priceRow.getStyle().set("grid-column", "1 / -1");
            details.add(priceRow);
        }

        card.add(topRow, details);
        return card;
    }

    private static String buildSeatsText(ShowDTO s) {
        int total = s.totalCapacity();
        if (total == 0) return "—";
        StringBuilder sb = new StringBuilder();
        if (s.standingCapacity > 0)
            sb.append("Standing: ").append(s.standingCapacity);
        if (s.numBlocks > 0) {
            if (sb.length() > 0) sb.append("  |  ");
            sb.append("Seated: ")
              .append(s.numBlocks).append("B × ")
              .append(s.rowsPerBlock).append("R × ")
              .append(s.seatsPerRow).append("S = ")
              .append(s.totalSeatedCapacity());
        }
        return sb.toString();
    }

    private static String buildPriceText(ShowDTO s) {
        StringBuilder sb = new StringBuilder();
        if (s.standingCapacity > 0 && s.standingPrice != null)
            sb.append("Standing: $").append(s.standingPrice.toPlainString());
        if (s.numBlocks > 0 && s.seatedPrice != null) {
            if (sb.length() > 0) sb.append("  |  ");
            sb.append("Seated: $").append(s.seatedPrice.toPlainString());
        }
        return sb.toString();
    }

    private static Div showInfoRow(String label, String value) {
        Div row = new Div();
        Span lbl = new Span(label + ": ");
        lbl.getStyle().set("font-weight", "600").set("color", "#666");
        Span val = new Span(value);
        row.add(lbl, val);
        return row;
    }

    // ── Show dialog (add / edit) ──────────────────────────────────────────────

    private void openShowDialog(ShowDTO existing) {
        boolean isEdit = existing != null;

        Dialog dialog = new Dialog();
        dialog.setWidth("680px");
        dialog.setHeaderTitle(isEdit ? "Edit Show" : "Add Show");

        TextField nameField   = new TextField("Show Name");
        nameField.setWidthFull();
        TextField singerField = new TextField("Singer / Performer");
        singerField.setWidthFull();
        TextArea descField    = new TextArea("Description");
        descField.setWidthFull();
        descField.setMinHeight("72px");
        DatePicker datePicker = new DatePicker("Show Date");
        datePicker.setWidthFull();
        // Constrain to the event's date range (set by startDatePicker / endDatePicker above)
        if (startDatePicker.getValue() != null)
            datePicker.setMin(startDatePicker.getValue().toLocalDate());
        if (endDatePicker.getValue() != null)
            datePicker.setMax(endDatePicker.getValue().toLocalDate());

        TextField standingCapField = new TextField("Standing Area — Capacity");
        standingCapField.setPlaceholder("0 = no standing area");
        standingCapField.setWidthFull();

        NumberField standingPriceField = new NumberField("Standing Ticket Price ($)");
        standingPriceField.setMin(0);
        standingPriceField.setPlaceholder("e.g. 30");
        standingPriceField.setWidthFull();

        TextField blocksField = new TextField("Number of Blocks");
        blocksField.setPlaceholder("e.g. 5");
        blocksField.setWidthFull();

        TextField rowsField = new TextField("Rows per Block");
        rowsField.setPlaceholder("e.g. 10");
        rowsField.setWidthFull();

        TextField seatsField = new TextField("Seats per Row");
        seatsField.setPlaceholder("e.g. 20");
        seatsField.setWidthFull();

        NumberField seatedPriceField = new NumberField("Seated Ticket Price ($)");
        seatedPriceField.setMin(0);
        seatedPriceField.setPlaceholder("e.g. 50");
        seatedPriceField.setWidthFull();

        if (isEdit) {
            nameField.setValue(nullSafe(existing.name));
            singerField.setValue(nullSafe(existing.singer));
            descField.setValue(nullSafe(existing.description));
            if (existing.showDate != null) datePicker.setValue(existing.showDate);
            if (existing.standingCapacity > 0)
                standingCapField.setValue(String.valueOf(existing.standingCapacity));
            if (existing.numBlocks > 0)    blocksField.setValue(String.valueOf(existing.numBlocks));
            if (existing.rowsPerBlock > 0) rowsField.setValue(String.valueOf(existing.rowsPerBlock));
            if (existing.seatsPerRow > 0)  seatsField.setValue(String.valueOf(existing.seatsPerRow));
            if (existing.standingPrice != null) standingPriceField.setValue(existing.standingPrice.doubleValue());
            if (existing.seatedPrice   != null) seatedPriceField.setValue(existing.seatedPrice.doubleValue());
        }

        // ── Live seat-map preview (II.4.2), redraws as you type ───────────────
        standingCapField.setValueChangeMode(ValueChangeMode.EAGER);
        blocksField.setValueChangeMode(ValueChangeMode.EAGER);
        rowsField.setValueChangeMode(ValueChangeMode.EAGER);
        seatsField.setValueChangeMode(ValueChangeMode.EAGER);

        Div seatPreview = new Div();
        seatPreview.getStyle()
                .set("width", "100%").set("overflow-x", "auto")
                .set("border", "1px solid #e3eaf5").set("border-radius", "10px")
                .set("background", "#fff").set("padding", "10px")
                .set("min-height", "60px").set("box-sizing", "border-box");
        Runnable refreshSeatPreview = () -> SeatPreviewRenderer.render(seatPreview,
                parseIntOrZero(standingCapField.getValue()), parseIntOrZero(blocksField.getValue()),
                parseIntOrZero(rowsField.getValue()), parseIntOrZero(seatsField.getValue()));
        standingCapField.addValueChangeListener(ev -> refreshSeatPreview.run());
        blocksField.addValueChangeListener(ev -> refreshSeatPreview.run());
        rowsField.addValueChangeListener(ev -> refreshSeatPreview.run());
        seatsField.addValueChangeListener(ev -> refreshSeatPreview.run());
        refreshSeatPreview.run();

        Span mapHint = new Span(
                "🗺 Tip: after the event is created, open it from Event Details to arrange the full visual venue map.");
        mapHint.getStyle().set("font-size", "12px").set("color", "#666");

        Button saveBtn = new Button(isEdit ? "Save" : "Add", e -> {
            String name  = nameField.getValue();
            if (name == null || name.isBlank()) {
                Notification.show("Show name is required", 2500, Notification.Position.MIDDLE);
                return;
            }

            int standingCap  = parseIntOrZero(standingCapField.getValue());
            int numBlocks    = parseIntOrZero(blocksField.getValue());
            int rowsPerBlock = parseIntOrZero(rowsField.getValue());
            int seatsPerRow  = parseIntOrZero(seatsField.getValue());

            boolean hasSeated = numBlocks > 0 || rowsPerBlock > 0 || seatsPerRow > 0;
            if (hasSeated && (numBlocks <= 0 || rowsPerBlock <= 0 || seatsPerRow <= 0)) {
                Notification.show("Seated area requires blocks, rows, and seats all > 0",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            java.math.BigDecimal sPriceSeated   = seatedPriceField.getValue() != null
                    ? java.math.BigDecimal.valueOf(seatedPriceField.getValue()) : new java.math.BigDecimal("50.00");
            java.math.BigDecimal sPriceStanding = standingPriceField.getValue() != null
                    ? java.math.BigDecimal.valueOf(standingPriceField.getValue()) : new java.math.BigDecimal("30.00");

            if (isEdit) {
                existing.name             = name.trim();
                existing.singer           = singerField.getValue().trim();
                existing.description      = descField.getValue().trim();
                existing.showDate         = datePicker.getValue();
                existing.standingCapacity = standingCap;
                existing.numBlocks        = numBlocks;
                existing.rowsPerBlock     = rowsPerBlock;
                existing.seatsPerRow      = seatsPerRow;
                existing.seatedPrice      = sPriceSeated;
                existing.standingPrice    = sPriceStanding;
            } else {
                ShowDTO dto = new ShowDTO(null, name.trim(),
                        descField.getValue().trim(), singerField.getValue().trim(),
                        datePicker.getValue());
                dto.standingCapacity = standingCap;
                dto.numBlocks        = numBlocks;
                dto.rowsPerBlock     = rowsPerBlock;
                dto.seatsPerRow      = seatsPerRow;
                dto.seatedPrice      = sPriceSeated;
                dto.standingPrice    = sPriceStanding;
                shows.add(dto);
            }

            refreshShowCards();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout body = new VerticalLayout(
                nameField, singerField, descField, datePicker,
                sectionLabel("Standing Area"),
                standingCapField, standingPriceField,
                sectionLabel("Seated Area"),
                blocksField, rowsField, seatsField, seatedPriceField,
                sectionLabel("Live preview"),
                seatPreview, mapHint
        );
        body.setPadding(true);
        body.setSpacing(true);
        body.setWidthFull();

        dialog.add(body);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private static Span sectionLabel(String text) {
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

    private static int parseIntOrZero(String value) {
        if (value == null || value.isBlank()) return 0;
        try { return Math.max(0, Integer.parseInt(value.trim())); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── Policies card ────────────────────────────────────────────────────────

    private Div buildPoliciesCard() {
        Div card = card();

        H2 title = new H2("Policies");
        title.getStyle().set("margin", "0 0 16px 0").set("font-size", "20px").set("color", "#111");

        sellingTypeBox = new ComboBox<>("Selling Type");
        sellingTypeBox.setItems(SellingType.values());
        sellingTypeBox.setItemLabelGenerator(t -> capitalize(t.name()));
        sellingTypeBox.setValue(SellingType.REGULAR);
        sellingTypeBox.setWidthFull();

        // ── Lottery fields (visible only when LOTTERY is selected) ──
        lotteryDeadlinePicker = new DateTimePicker("Registration Deadline");
        lotteryDeadlinePicker.setWidthFull();
        lotteryDeadlinePicker.setHelperText("Last date/time users can register for the lottery");

        lotteryDrawTimePicker = new DateTimePicker("Draw Time");
        lotteryDrawTimePicker.setWidthFull();
        lotteryDrawTimePicker.setHelperText("When the lottery winners will be drawn (must be after deadline)");

        lotteryDeadlinePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                lotteryDrawTimePicker.setMin(e.getValue().plusMinutes(1));
                if (lotteryDrawTimePicker.getValue() != null &&
                        !lotteryDrawTimePicker.getValue().isAfter(e.getValue()))
                    lotteryDrawTimePicker.clear();
            }
        });

        // Lottery deadline and draw time must both be before the event starts
        java.util.function.Consumer<java.time.LocalDateTime> applyEventStartMax = eventStart -> {
            if (eventStart != null) {
                lotteryDeadlinePicker.setMax(eventStart);
                lotteryDrawTimePicker.setMax(eventStart);
            }
        };
        applyEventStartMax.accept(startDatePicker.getValue());
        startDatePicker.addValueChangeListener(ev -> applyEventStartMax.accept(ev.getValue()));

        lotteryFieldsDiv = new Div(
                sectionLabel("Lottery Configuration"),
                lotteryDeadlinePicker,
                lotteryDrawTimePicker);
        lotteryFieldsDiv.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");
        lotteryFieldsDiv.setVisible(false);

        sellingTypeBox.addValueChangeListener(e ->
                lotteryFieldsDiv.setVisible(e.getValue() == SellingType.LOTTERY));

        minAgeField = new IntegerField("Minimum Age");
        minAgeField.setMin(0);
        minAgeField.setPlaceholder("Leave empty = no restriction");
        minAgeField.setWidthFull();

        minTicketsField = new IntegerField("Minimum Tickets per Purchase");
        minTicketsField.setMin(1);
        minTicketsField.setPlaceholder("Leave empty = no minimum");
        minTicketsField.setWidthFull();

        maxTicketsField = new IntegerField("Maximum Tickets per Purchase");
        maxTicketsField.setMin(1);
        maxTicketsField.setPlaceholder("Leave empty = no limit");
        maxTicketsField.setWidthFull();

        percentageField = new NumberField("Percentage Off (0–100)");
        percentageField.setMin(0);
        percentageField.setMax(100);
        percentageField.setPlaceholder("Leave empty = no percentage discount");
        percentageField.setWidthFull();

        couponCodeField = new TextField("Coupon Code");
        couponCodeField.setPlaceholder("e.g. SUMMER25  —  leave empty to skip");
        couponCodeField.setWidthFull();

        couponPctField = new NumberField("Coupon Percentage Off (0–100)");
        couponPctField.setMin(0);
        couponPctField.setMax(100);
        couponPctField.setPlaceholder("Required when coupon code is set");
        couponPctField.setWidthFull();

        couponExpiryPicker = new DatePicker("Coupon Expiry Date (optional)");
        couponExpiryPicker.setPlaceholder("Leave empty = never expires");
        couponExpiryPicker.setWidthFull();

        card.add(title,
                sectionLabel("Selling Policy"),
                sellingTypeBox,
                lotteryFieldsDiv,
                sectionLabel("Purchase Policy (optional)"),
                minAgeField, minTicketsField, maxTicketsField,
                sectionLabel("Percentage Discount (optional)"),
                percentageField,
                sectionLabel("Coupon Discount (optional)"),
                couponCodeField, couponPctField, couponExpiryPicker);
        return card;
    }

    // ── Single submit handler ────────────────────────────────────────────────

    private void handleCreate() {
        LocalDateTime startDT = startDatePicker.getValue();
        LocalDateTime endDT   = endDatePicker.getValue();
        LocalDate start = startDT != null ? startDT.toLocalDate() : null;
        LocalDate end   = endDT   != null ? endDT.toLocalDate()   : null;

        LocalDate couponExpiry = couponExpiryPicker.getValue();

        presenter.handleCreate(
                nameField.getValue(),
                venueField.getValue(),
                typeBox.getValue(),
                start, end,
                shows,
                sellingTypeBox.getValue(),
                minAgeField.getValue(),
                minTicketsField.getValue(),
                maxTicketsField.getValue(),
                percentageField.getValue(),
                couponCodeField.getValue(),
                couponPctField.getValue(),
                couponExpiry,
                lotteryDeadlinePicker.getValue(),
                lotteryDrawTimePicker.getValue());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void refreshShowCards() {
        showsContainer.removeAll();
        for (ShowDTO s : shows)
            showsContainer.add(buildShowCard(s));
    }

    private static void error(String msg) {
        Notification.show(msg, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
        card.addClassNames(LumoUtility.Padding.Vertical.XLARGE, LumoUtility.AlignSelf.START);
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
        return s == null ? "" : s;
    }

    private static java.util.List<Area> buildAreas(ShowDTO s) {
        java.util.List<Area> areas = new java.util.ArrayList<>();

        if (s.standingCapacity > 0)
            areas.add(new StandingArea("Standing Area", s.standingCapacity));

        if (s.numBlocks > 0 && s.rowsPerBlock > 0 && s.seatsPerRow > 0) {
            SeatedArea seated = new SeatedArea("Seated Area", s.numBlocks);
            java.util.List<Block> blocks = new java.util.ArrayList<>();
            long idSeq = 1;
            for (int b = 0; b < s.numBlocks; b++) {
                String blockLabel = String.valueOf((char) ('A' + b));
                Block block = new Block(idSeq++, blockLabel, s.rowsPerBlock, seated);
                java.util.List<Row> rows = new java.util.ArrayList<>();
                for (int r = 0; r < s.rowsPerBlock; r++) {
                    Row row = new Row(idSeq++, String.valueOf(r + 1), s.seatsPerRow, block);
                    java.util.List<Seat> seats = new java.util.ArrayList<>();
                    for (int seat = 1; seat <= s.seatsPerRow; seat++)
                        seats.add(new Seat(idSeq++, String.valueOf(seat), row));
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

    // ── EventCreationPresenter.View implementation ───────────────────────────

    @Override
    public Object getSessionAttribute(String key) {
        return UI.getCurrent().getSession().getAttribute(key);
    }

    @Override
    public void setSessionAttribute(String key, Object value) {
        UI.getCurrent().getSession().setAttribute(key, value);
    }

    @Override
    public void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    @Override
    public void showError(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void showWarning(String message) {
        Notification.show(message, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    @Override
    public void navigateToCompany() {
        UI.getCurrent().navigate("company");
    }

}
