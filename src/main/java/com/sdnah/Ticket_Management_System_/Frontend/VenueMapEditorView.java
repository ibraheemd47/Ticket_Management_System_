package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueAreaRefDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VenueMapDTO;
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.VenueMapPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Graphical venue layout / event-map editor (II.4.2). Owners/managers place the
 * stage, entrances and seated/standing areas on a canvas, link seated/standing
 * elements to inventory (pricing/seating) areas, and save the layout.
 *
 * <p>The canvas is a live SVG preview; clicking it moves the selected element,
 * and the right-hand panel offers add / position / size / colour / link / delete
 * controls. Convention: the previous view sets {@code eventId} on the session
 * before navigating to {@code "venue-map"}.
 */
@Route("venue-map")
public class VenueMapEditorView extends VerticalLayout implements BeforeEnterObserver {

    private static final String SESSION_TOKEN    = "token";
    private static final String SESSION_EVENT_ID = "eventId";

    private static final String STAGE = "STAGE", ENTRANCE = "ENTRANCE",
            SEATED = "SEATED", STANDING = "STANDING";
    private static final String[] SWATCHES =
            { "#37474f", "#1565c0", "#2e7d32", "#8e24aa", "#c62828", "#ef6c00", "#00838f" };

    private final VenueMapPresenter presenter;

    private VenueMapDTO map = new VenueMapDTO();
    private List<VenueAreaRefDTO> areas = List.of();
    private VenueMapDTO.Element selected;

    private final Div canvasSlot = new Div();
    private final Div elementListSlot = new Div();
    private final Div selectedEditorSlot = new Div();

    private boolean syncing = false;

    public VenueMapEditorView(VenueMapPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "#f4f4f4").set("font-family", "Arial, sans-serif");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object t = UI.getCurrent().getSession().getAttribute(SESSION_TOKEN);
        if (t == null || t.toString().startsWith("GUEST_")) {
            event.forwardTo(LoginView.class);
            return;
        }
        Object e = UI.getCurrent().getSession().getAttribute(SESSION_EVENT_ID);
        if (e == null) {
            event.forwardTo(MainView.class);
            return;
        }
        UUID eventId;
        try {
            eventId = UUID.fromString(e.toString());
        } catch (IllegalArgumentException bad) {
            event.forwardTo(MainView.class);
            return;
        }
        if (!presenter.bind(t.toString(), eventId)) {
            Notification.show("You don't have permission to edit this event's map.",
                    3500, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.forwardTo("EventDetails");
            return;
        }

        map = presenter.loadMap();
        areas = presenter.getAreas();
        selected = null;
        // First time (no saved map): seed the canvas from the seating already
        // defined for this event, so the editor shows the existing blocks/areas.
        seedFromSeatingIfEmpty();

        removeAll();
        add(buildHeader(), buildBody());
        renderCanvas();
        renderElementList();
        renderSelectedEditor();
    }

    /**
     * When there's no saved map yet, generate starter elements from the event's
     * inventory: a stage, a standing band (if any), and one card per seated
     * block — so opening the editor reflects what was already created (II.4.2).
     */
    private void seedFromSeatingIfEmpty() {
        if (map.elements != null && !map.elements.isEmpty()) return;
        VenueMapDTO seeded = VenueMapSeeder.buildDefault(presenter.getSeatingDims(), areas);
        map.canvasWidth = seeded.canvasWidth;
        map.canvasHeight = seeded.canvasHeight;
        map.elements = seeded.elements;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private Div buildHeader() {
        Div header = new Div();
        header.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("padding", "24px 52px").set("width", "100%")
                .set("box-sizing", "border-box")
                .set("display", "flex").set("justify-content", "space-between").set("align-items", "center");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle().set("margin", "0").set("font-size", "22px").set("font-weight", "900");

        Span back = new Span("⟵ Back to event");
        back.getStyle().set("cursor", "pointer").set("font-weight", "700");
        back.addClickListener(ev -> UI.getCurrent().navigate("EventDetails"));

        header.add(logo, back);
        return header;
    }

    private Div buildBody() {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "1180px").set("margin", "32px auto").set("padding", "24px 28px")
                .set("background", "white").set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)");

        H1 title = new H1("Venue map — " + presenter.getEventName());
        title.getStyle().set("margin", "0 0 4px 0").set("font-size", "26px");
        Paragraph blurb = new Paragraph(
                "The map is seeded from the seating you've already defined. Click an element to select it, "
                + "then move / resize / recolour it and link seated or standing areas to your inventory so "
                + "the map matches what's on sale.");
        blurb.getStyle().set("color", "#666").set("margin", "0 0 16px 0");

        // Canvas (left) + controls (right). Sized to the map; elements are
        // absolutely-positioned child Divs (reliable, selectable).
        canvasSlot.getStyle()
                .set("position", "relative").set("flex-shrink", "0")
                .set("width", map.canvasWidth + "px").set("max-width", "100%")
                .set("border", "1px solid #d0d7e2").set("border-radius", "10px")
                .set("overflow", "auto").set("align-self", "flex-start")
                .set("background", "#eef2f7");

        Div controls = new Div(buildAddPanel(), selectedEditorSlot, buildElementsPanel(), buildActionsRow());
        controls.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "16px").set("flex", "1");

        Div row = new Div(canvasSlot, controls);
        row.getStyle().set("display", "flex").set("gap", "24px").set("align-items", "flex-start").set("flex-wrap", "wrap");

        card.add(title, blurb, row);

        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    private Div buildAddPanel() {
        Div panel = sectionCard("Add element");

        Select<String> typeSel = new Select<>();
        typeSel.setLabel("Type");
        typeSel.setItems(STAGE, ENTRANCE, SEATED, STANDING);
        typeSel.setItemLabelGenerator(VenueMapEditorView::prettyType);
        typeSel.setValue(SEATED);
        typeSel.setWidthFull();

        TextField labelField = new TextField("Label");
        labelField.setPlaceholder("e.g. VIP, Main entrance");
        labelField.setWidthFull();

        ComboBox<VenueAreaRefDTO> areaLink = new ComboBox<>("Link to inventory area");
        areaLink.setItemLabelGenerator(a -> a.name + "  (" + prettyType(a.type) + ")");
        areaLink.setClearButtonVisible(true);
        areaLink.setWidthFull();
        areaLink.setHelperText("Seated / standing only");

        Runnable refreshAreaItems = () -> {
            String type = typeSel.getValue();
            boolean linkable = SEATED.equals(type) || STANDING.equals(type);
            areaLink.setVisible(linkable);
            if (linkable) {
                areaLink.setItems(areas.stream().filter(a -> type.equals(a.type)).toList());
            }
            areaLink.clear();
        };
        typeSel.addValueChangeListener(ev -> refreshAreaItems.run());
        refreshAreaItems.run();

        Button add = new Button("+ Add to map", ev -> {
            VenueAreaRefDTO linked = areaLink.isVisible() ? areaLink.getValue() : null;
            String label = labelField.getValue();
            if ((label == null || label.isBlank()) && linked != null) label = linked.name;
            addElement(typeSel.getValue(), label, linked);
            labelField.clear();
            areaLink.clear();
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        panel.add(typeSel, labelField, areaLink, add);
        return panel;
    }

    private Div buildElementsPanel() {
        Div panel = sectionCard("Elements");
        panel.add(elementListSlot);
        return panel;
    }

    private Div buildActionsRow() {
        Button save = new Button("Save venue map", ev -> {
            if (presenter.saveMap(map)) {
                Notification.show("Venue map saved", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button clear = new Button("Clear all", ev -> {
            map.elements.clear();
            selected = null;
            renderCanvas();
            renderElementList();
            renderSelectedEditor();
        });
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        Div row = new Div(save, clear);
        row.getStyle().set("display", "flex").set("gap", "10px").set("margin-top", "4px");
        return row;
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    private void renderCanvas() {
        canvasSlot.removeAll();
        Div inner = new Div();
        inner.getStyle()
                .set("position", "relative")
                .set("width", map.canvasWidth + "px").set("height", map.canvasHeight + "px")
                .set("background-color", "#eef2f7")
                .set("background-image",
                        "linear-gradient(#dde3ec 1px, transparent 1px),"
                                + "linear-gradient(90deg, #dde3ec 1px, transparent 1px)")
                .set("background-size", "50px 50px");
        for (VenueMapDTO.Element el : map.elements) {
            inner.add(buildCanvasElement(el));
        }
        canvasSlot.add(inner);
    }

    /** One map element rendered as an absolutely-positioned, click-to-select Div. */
    private Div buildCanvasElement(VenueMapDTO.Element el) {
        boolean sel = el == selected;
        String fill = el.color != null ? el.color : defaultColor(el.type);

        Div box = new Div();
        box.setText(displayLabel(el));
        box.getStyle()
                .set("position", "absolute")
                .set("left", Math.round(el.x) + "px").set("top", Math.round(el.y) + "px")
                .set("width", Math.round(el.w) + "px").set("height", Math.round(el.h) + "px")
                .set("background", fill).set("color", "white")
                .set("border-radius", "8px")
                .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                .set("text-align", "center").set("font-size", "12px").set("font-weight", "700")
                .set("box-sizing", "border-box").set("padding", "4px")
                .set("cursor", "pointer").set("overflow", "hidden")
                .set("border", sel ? "3px dashed #ff6f00" : "2px solid rgba(0,0,0,0.25)")
                .set("box-shadow", sel ? "0 0 0 3px rgba(255,111,0,0.25)" : "none");
        box.addClickListener(ev -> {
            selected = el;
            renderCanvas();
            renderElementList();
            renderSelectedEditor();
        });
        return box;
    }

    private void renderElementList() {
        elementListSlot.removeAll();
        if (map.elements.isEmpty()) {
            Paragraph empty = new Paragraph("No elements yet. Add a stage, entrances and areas above.");
            empty.getStyle().set("color", "#888").set("margin", "4px 0");
            elementListSlot.add(empty);
            return;
        }
        for (VenueMapDTO.Element el : map.elements) {
            elementListSlot.add(buildElementRow(el));
        }
    }

    private Div buildElementRow(VenueMapDTO.Element el) {
        boolean sel = el == selected;

        Span swatch = new Span();
        swatch.getStyle()
                .set("width", "14px").set("height", "14px").set("border-radius", "3px")
                .set("background", el.color != null ? el.color : defaultColor(el.type))
                .set("display", "inline-block").set("flex-shrink", "0");

        Span label = new Span(prettyType(el.type) + " · " + displayLabel(el));
        label.getStyle().set("font-size", "13px").set("color", "#222");

        Div row = new Div(swatch, label);
        row.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "8px")
                .set("padding", "6px 10px").set("border-radius", "8px").set("cursor", "pointer")
                .set("background", sel ? "#e3f0ff" : "transparent")
                .set("border", sel ? "1px solid #90c2ff" : "1px solid transparent");
        row.addClickListener(ev -> {
            selected = el;
            renderCanvas();
            renderElementList();
            renderSelectedEditor();
        });
        return row;
    }

    private void renderSelectedEditor() {
        selectedEditorSlot.removeAll();
        if (selected == null) {
            return;
        }
        Div panel = sectionCard("Selected: " + prettyType(selected.type));

        TextField label = new TextField("Label");
        label.setValue(selected.label == null ? "" : selected.label);
        label.setWidthFull();
        label.addValueChangeListener(ev -> {
            if (syncing) return;
            selected.label = ev.getValue();
            renderCanvas();
            renderElementList();
        });

        NumberField x = posField("X", selected.x, v -> { selected.x = clamp(v, 0, map.canvasWidth - selected.w); });
        NumberField y = posField("Y", selected.y, v -> { selected.y = clamp(v, 0, map.canvasHeight - selected.h); });
        NumberField w = posField("Width", selected.w, v -> { selected.w = clamp(v, 20, map.canvasWidth - selected.x); });
        NumberField h = posField("Height", selected.h, v -> { selected.h = clamp(v, 20, map.canvasHeight - selected.y); });

        HorizontalLayout dims = new HorizontalLayout(x, y, w, h);
        dims.setWidthFull();

        // Nudge / resize buttons for tactile positioning.
        HorizontalLayout nudge = new HorizontalLayout(
                nudgeBtn("◀", () -> selected.x = clamp(selected.x - 10, 0, map.canvasWidth - selected.w)),
                nudgeBtn("▲", () -> selected.y = clamp(selected.y - 10, 0, map.canvasHeight - selected.h)),
                nudgeBtn("▼", () -> selected.y = clamp(selected.y + 10, 0, map.canvasHeight - selected.h)),
                nudgeBtn("▶", () -> selected.x = clamp(selected.x + 10, 0, map.canvasWidth - selected.w)),
                nudgeBtn("－", () -> { selected.w = clamp(selected.w - 10, 20, map.canvasWidth); selected.h = clamp(selected.h - 10, 20, map.canvasHeight); }),
                nudgeBtn("＋", () -> { selected.w = clamp(selected.w + 10, 20, map.canvasWidth - selected.x); selected.h = clamp(selected.h + 10, 20, map.canvasHeight - selected.y); }));
        nudge.setSpacing(false);
        nudge.getStyle().set("gap", "4px");

        // Colour swatches.
        HorizontalLayout colors = new HorizontalLayout();
        colors.setSpacing(false);
        colors.getStyle().set("gap", "6px").set("flex-wrap", "wrap");
        for (String c : SWATCHES) {
            Button sw = new Button("", ev -> { selected.color = c; renderCanvas(); renderElementList(); });
            sw.getStyle()
                    .set("background", c).set("min-width", "24px").set("width", "24px").set("height", "24px")
                    .set("padding", "0").set("border-radius", "5px").set("border", "1px solid rgba(0,0,0,0.2)");
            colors.add(sw);
        }

        panel.add(label, dims, fieldLabel("Move / resize"), nudge, fieldLabel("Colour"), colors);

        // Link to inventory area (seated/standing only).
        if (SEATED.equals(selected.type) || STANDING.equals(selected.type)) {
            ComboBox<VenueAreaRefDTO> areaLink = new ComboBox<>("Linked inventory area");
            areaLink.setItems(areas.stream().filter(a -> selected.type.equals(a.type)).toList());
            areaLink.setItemLabelGenerator(a -> a.name + "  (" + prettyType(a.type) + ")");
            areaLink.setClearButtonVisible(true);
            areaLink.setWidthFull();
            if (selected.areaId != null) {
                areas.stream().filter(a -> selected.areaId.equals(a.id)).findFirst().ifPresent(areaLink::setValue);
            }
            areaLink.addValueChangeListener(ev -> {
                VenueAreaRefDTO v = ev.getValue();
                selected.areaId = v == null ? null : v.id;
                selected.areaName = v == null ? null : v.name;
                renderElementList();
            });
            panel.add(areaLink);
        }

        Button delete = new Button("Delete element", ev -> {
            map.elements.remove(selected);
            selected = null;
            renderCanvas();
            renderElementList();
            renderSelectedEditor();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        delete.getStyle().set("margin-top", "4px");
        panel.add(delete);

        selectedEditorSlot.add(panel);
    }

    // ── Element operations ──────────────────────────────────────────────────────

    private void addElement(String type, String label, VenueAreaRefDTO linked) {
        VenueMapDTO.Element el = new VenueMapDTO.Element();
        el.id = "el-" + UUID.randomUUID();
        el.type = type;
        el.label = label;
        double[] size = defaultSize(type);
        el.w = size[0];
        el.h = size[1];
        int n = map.elements.size();
        el.x = clamp(40 + (n * 28) % 360, 0, map.canvasWidth - el.w);
        el.y = clamp(40 + (n * 24) % 240, 0, map.canvasHeight - el.h);
        el.color = defaultColor(type);
        if (linked != null) {
            el.areaId = linked.id;
            el.areaName = linked.name;
        }
        map.elements.add(el);
        selected = el;
        renderCanvas();
        renderElementList();
        renderSelectedEditor();
    }

    // ── Small helpers ────────────────────────────────────────────────────────────

    public void showError(String message) {
        Notification.show(message, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private NumberField posField(String label, double value, java.util.function.DoubleConsumer apply) {
        NumberField f = new NumberField(label);
        f.setStep(10);
        f.setValue(value);
        f.setWidth("90px");
        f.addValueChangeListener(ev -> {
            if (syncing || ev.getValue() == null) return;
            apply.accept(ev.getValue());
            renderCanvas();
            // Re-sync all fields (clamping may have changed values) without re-rendering the whole editor.
            syncSelectedEditorFields();
        });
        return f;
    }

    private Button nudgeBtn(String glyph, Runnable action) {
        Button b = new Button(glyph, ev -> {
            action.run();
            renderCanvas();
            syncSelectedEditorFields();
        });
        b.getStyle().set("min-width", "34px").set("padding", "4px 6px");
        return b;
    }

    /** Rebuild the selected-element editor so its number fields reflect clamped values. */
    private void syncSelectedEditorFields() {
        syncing = true;
        try {
            renderSelectedEditor();
        } finally {
            syncing = false;
        }
    }

    private Div sectionCard(String title) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "14px 16px").set("background", "#f9fbff")
                .set("border", "1px solid #e3eaf5").set("border-radius", "12px")
                .set("display", "flex").set("flex-direction", "column").set("gap", "10px");
        H2 h = new H2(title);
        h.getStyle().set("margin", "0").set("font-size", "16px");
        card.add(h);
        return card;
    }

    private static Span fieldLabel(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-size", "12px").set("color", "#666").set("margin-top", "2px");
        return s;
    }

    private static String displayLabel(VenueMapDTO.Element el) {
        if (el.label != null && !el.label.isBlank()) return el.label;
        return prettyType(el.type);
    }

    private static String prettyType(String type) {
        if (type == null) return "Area";
        return switch (type) {
            case STAGE    -> "Stage";
            case ENTRANCE -> "Entrance";
            case SEATED   -> "Seated area";
            case STANDING -> "Standing area";
            default       -> type;
        };
    }

    private static String defaultColor(String type) {
        if (type == null) return "#1565c0";
        return switch (type) {
            case STAGE    -> "#37474f";
            case ENTRANCE -> "#8e24aa";
            case SEATED   -> "#1565c0";
            case STANDING -> "#2e7d32";
            default       -> "#1565c0";
        };
    }

    private static double[] defaultSize(String type) {
        if (type == null) return new double[]{ 160, 110 };
        return switch (type) {
            case STAGE    -> new double[]{ 260, 70 };
            case ENTRANCE -> new double[]{ 100, 44 };
            default       -> new double[]{ 160, 110 };
        };
    }

    private static double clamp(double v, double lo, double hi) {
        if (hi < lo) hi = lo;
        return Math.max(lo, Math.min(hi, v));
    }

}
