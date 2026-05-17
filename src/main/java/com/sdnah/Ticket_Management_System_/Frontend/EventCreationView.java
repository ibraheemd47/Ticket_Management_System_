package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.time.ZoneId;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ShowDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
//need to add the area type for the show creartion and add the policy for selling ot discount on events / shows 
@Route("event-create")
public class EventCreationView extends VerticalLayout {

    private final company_managment_serivce companyService;
    private final EventService eventService;

    private final List<ShowDTO> shows = new ArrayList<>();
    private Div showsContainer;

    public EventCreationView(company_managment_serivce companyService, EventService eventService) {
        this.companyService = companyService;
        this.eventService   = eventService;
        addMockShows();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        Div content = new Div(buildEventCard(), buildShowsCard());
        content.getStyle()
                .set("max-width", "760px")
                .set("margin", "40px auto")
                .set("width", "100%")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "24px");

        add(buildHeader(), content);
    }

    // ── Mock data ────────────────────────────────────────────────────────────

    private void addMockShows() {
        ShowDTO s1 = new ShowDTO(null, "Opening Night", "Fireworks and live music to kick off the festival.", "The Midnight", LocalDate.of(2025, 7, 10));
        s1.standingCapacity = 500;
        s1.numBlocks = 4;
        s1.rowsPerBlock = 10;
        s1.seatsPerRow = 20;
        shows.add(s1);

        ShowDTO s2 = new ShowDTO(null, "Headline Act", "Main stage performance.", "Coldplay", LocalDate.of(2025, 7, 11));
        s2.standingCapacity = 800;
        shows.add(s2);

        ShowDTO s3 = new ShowDTO(null, "Closing Night", "Grand finale with special guests.", "Dua Lipa", LocalDate.of(2025, 7, 12));
        s3.numBlocks = 6;
        s3.rowsPerBlock = 12;
        s3.seatsPerRow = 15;
        shows.add(s3);
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
        title.getStyle()
                .set("margin", "0 0 24px 0")
                .set("font-size", "26px")
                .set("color", "#111");

        TextField nameField = new TextField("Event Name");
        nameField.setPlaceholder("e.g. Summer Concert 2025");
        nameField.setWidthFull();

        TextField venueField = new TextField("Venue / Location");
        venueField.setPlaceholder("e.g. BGU Auditorium, Tel Aviv Hall");
        venueField.setWidthFull();

        ComboBox<show_type> typeBox = new ComboBox<>("Event Type");
        typeBox.setItems(show_type.values());
        typeBox.setItemLabelGenerator(t -> capitalize(t.name()));
        typeBox.setWidthFull();

        DatePicker startDatePicker = new DatePicker("Start Date");
        startDatePicker.setWidthFull();
        startDatePicker.setPlaceholder("Choose start date");

        DatePicker endDatePicker = new DatePicker("End Date");
        endDatePicker.setWidthFull();
        endDatePicker.setPlaceholder("Choose end date");

        startDatePicker.addValueChangeListener(e -> {
            LocalDate start = e.getValue();
            if (start != null) {
                endDatePicker.setMin(start);
                if (endDatePicker.getValue() != null && endDatePicker.getValue().isBefore(start))
                    endDatePicker.clear();
            }
        });
        endDatePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) startDatePicker.setMax(e.getValue());
        });

        Button createBtn = new Button("Create Event");
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.getStyle().set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("padding", "10px 28px");

        Button cancelBtn = new Button("Cancel", e -> UI.getCurrent().navigate("company"));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        createBtn.addClickListener(e ->
                handleCreate(nameField, venueField, typeBox, startDatePicker, endDatePicker));

        card.add(title, nameField, venueField, typeBox, startDatePicker, endDatePicker,
                new HorizontalLayout(createBtn, cancelBtn));
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

        // ── Basic info ──
        TextField nameField   = new TextField("Show Name");
        nameField.setWidthFull();
        TextField singerField = new TextField("Singer / Performer");
        singerField.setWidthFull();
        TextArea descField    = new TextArea("Description");
        descField.setWidthFull();
        descField.setMinHeight("72px");
        DatePicker datePicker = new DatePicker("Show Date");
        datePicker.setWidthFull();

        // ── Standing area ──
        TextField standingCapField = new TextField("Standing Area — Capacity");
        standingCapField.setPlaceholder("0 = no standing area");
        standingCapField.setWidthFull();

        // ── Seated area ──
        TextField blocksField   = new TextField("Number of Blocks");
        blocksField.setPlaceholder("e.g. 5");
        blocksField.setWidthFull();

        TextField rowsField     = new TextField("Rows per Block");
        rowsField.setPlaceholder("e.g. 10");
        rowsField.setWidthFull();

        TextField seatsField    = new TextField("Seats per Row");
        seatsField.setPlaceholder("e.g. 20");
        seatsField.setWidthFull();

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
        }

        Button saveBtn = new Button(isEdit ? "Save" : "Add", e -> {
            String name  = nameField.getValue();
            if (name == null || name.isBlank()) {
                Notification.show("Show name is required", 2500, Notification.Position.MIDDLE);
                return;
            }

            int standingCap = parseIntOrZero(standingCapField.getValue());
            int numBlocks   = parseIntOrZero(blocksField.getValue());
            int rowsPerBlock = parseIntOrZero(rowsField.getValue());
            int seatsPerRow  = parseIntOrZero(seatsField.getValue());

            // Validate seated config: all-or-nothing
            boolean hasSeated = numBlocks > 0 || rowsPerBlock > 0 || seatsPerRow > 0;
            if (hasSeated && (numBlocks <= 0 || rowsPerBlock <= 0 || seatsPerRow <= 0)) {
                Notification.show("Seated area requires blocks, rows, and seats all > 0",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (isEdit) {
                existing.name            = name.trim();
                existing.singer          = singerField.getValue().trim();
                existing.description     = descField.getValue().trim();
                existing.showDate        = datePicker.getValue();
                existing.standingCapacity = standingCap;
                existing.numBlocks       = numBlocks;
                existing.rowsPerBlock    = rowsPerBlock;
                existing.seatsPerRow     = seatsPerRow;
            } else {
                ShowDTO dto = new ShowDTO(null, name.trim(),
                        descField.getValue().trim(), singerField.getValue().trim(),
                        datePicker.getValue());
                dto.standingCapacity = standingCap;
                dto.numBlocks        = numBlocks;
                dto.rowsPerBlock     = rowsPerBlock;
                dto.seatsPerRow      = seatsPerRow;
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
                standingCapField,
                sectionLabel("Seated Area"),
                blocksField, rowsField, seatsField
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

    // ── Submit ───────────────────────────────────────────────────────────────

    private void handleCreate(
            TextField nameField,
            TextField venueField,
            ComboBox<show_type> typeBox,
            DatePicker startDatePicker,
            DatePicker endDatePicker) {

        String name     = nameField.getValue();
        String venue    = venueField.getValue();
        show_type type  = typeBox.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();

        if (name == null || name.isBlank())  { error("Event name is required");          return; }
        if (venue == null || venue.isBlank()) { error("Venue is required");               return; }
        if (type == null)                     { error("Please select an event type");     return; }
        if (start == null)                    { error("Start date is required");          return; }
        if (end == null)                      { error("End date is required");            return; }
        if (end.isBefore(start))              { error("End date cannot be before start"); return; }

        Object tokenObj     = UI.getCurrent().getSession().getAttribute("token");
        Object companyIdObj = UI.getCurrent().getSession().getAttribute("managingCompanyId");

        if (tokenObj == null) {
            Notification.show("Not logged in — sign in first to save the event",
                    4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (companyIdObj == null) {
            Notification.show("No company selected — navigate from your company page",
                    4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        String token    = tokenObj.toString();
        int    companyId = Integer.parseInt(companyIdObj.toString());

        EventDto dto   = new EventDto();
        dto.name       = name.trim();
        dto.venue      = venue.trim();
        dto.eventType  = type;
        dto.startDate  = start;
        dto.endDate    = end;

        try {
            EventDto created = companyService.addEvent(token, companyId, dto);

            // Persist shows
            Long memberId = companyService.getMemberIdByToken(token);
            for (ShowDTO s : shows) {
                java.util.Date showDate = s.showDate != null
                    ? java.util.Date.from(s.showDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;
                show newShow = new show(created.id, s.name, s.description, s.singer, showDate);
                try {
                    eventService.addShowToEvent(created.id, newShow, memberId);
                } catch (Exception ignored) {
                    // show added to event even if area config fails separately
                }
            }

            Notification.show("Event \"" + created.name + "\" created with " + shows.size() + " show(s)!",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            UI.getCurrent().getSession().setAttribute("eventId", created.id.toString());
            UI.getCurrent().navigate("company");
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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
}
