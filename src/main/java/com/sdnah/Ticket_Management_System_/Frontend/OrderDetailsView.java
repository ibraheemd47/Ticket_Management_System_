package com.sdnah.Ticket_Management_System_.Frontend;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

/**
 * "My Orders" page — Active tab pulls from {@code getPendingOrdersByBuyer},
 * Past tab pulls from {@code getPurchaseHistory}.
 *
 * <p>The previous version was a static placeholder that always rendered
 * "No orders" — the data fetch never happened. This rewrite calls the backend
 * and only falls back to the empty-state card when the list is actually empty.
 */
@Route("orders")
public class OrderDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private final ActiveOrderService orderService;

    private String selectedTab = "active"; // default to "active" (was "past" before)
    private String token;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public OrderDetailsView(ActiveOrderService orderService) {
        this.orderService = orderService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("font-family", "Arial, sans-serif")
                .set("background", "#f4f4f4");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object t = event.getUI().getSession().getAttribute("token");
        if (t == null) {
            event.rerouteTo("login");
            return;
        }
        this.token = t.toString();

        // Read ?tab=… from the URL.
        QueryParameters qp = event.getLocation().getQueryParameters();
        Map<String, List<String>> params = qp.getParameters();
        if (params.containsKey("tab") && !params.get("tab").isEmpty()) {
            selectedTab = params.get("tab").get(0);
        }

        removeAll();
        add(createHeader());
        add(createOrdersSection());
    }

    // ── Data + body ──────────────────────────────────────────────────────────

    private Div createOrdersSection() {
        if ("past".equals(selectedTab)) {
            return renderPastOrders();
        }
        return renderActiveOrders();
    }

    private Div renderActiveOrders() {
        List<OrderDTO> orders;
        try {
            orders = orderService.getPendingOrdersByBuyer(token);
        } catch (RuntimeException ex) {
            return errorCard("Couldn't load your active orders: " + ex.getMessage());
        }

        if (orders == null || orders.isEmpty()) {
            return emptyStateCard(
                    "No active orders",
                    "Active orders you reserve or start purchasing will appear here.");
        }

        Grid<OrderDTO> grid = new Grid<>(OrderDTO.class, false);
        grid.addColumn(o -> shortId(o.getOrderId())).setHeader("Order").setAutoWidth(true);
        grid.addColumn(o -> o.getStatus() == null ? "—" : o.getStatus()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(o -> o.getItems() == null ? 0 : o.getItems().size()).setHeader("Items").setAutoWidth(true);
        grid.addColumn(o -> "$" + o.getFinalPrice()).setHeader("Total").setAutoWidth(true);
        grid.addColumn(o -> o.getExpiresAt() == null ? "—" : o.getExpiresAt().format(DATE_FMT))
                .setHeader("Expires").setAutoWidth(true);
        grid.addComponentColumn(o -> {
            Button open = new Button("Open", e -> {
                getUI().ifPresent(ui -> {
                    ui.getSession().setAttribute("managerOrderId", o.getOrderId().toString());
                    ui.navigate("manager/order");
                });
            });
            open.getStyle()
                    .set("background", "#026cdf").set("color", "white")
                    .set("font-weight", "700").set("padding", "6px 18px")
                    .set("border-radius", "8px");
            return open;
        }).setHeader("");

        grid.setItems(orders);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        return wrapInCard(grid, "Your active orders (" + orders.size() + ")");
    }

    private Div renderPastOrders() {
        List<PurchaseDTO> purchases;
        try {
            purchases = orderService.getPurchaseHistory(token);
        } catch (RuntimeException ex) {
            return errorCard("Couldn't load your purchase history: " + ex.getMessage());
        }

        if (purchases == null || purchases.isEmpty()) {
            return emptyStateCard(
                    "No past orders",
                    "Tickets you bought before will automatically appear here.");
        }

        Grid<PurchaseDTO> grid = new Grid<>(PurchaseDTO.class, false);
        grid.addColumn(p -> shortId(p.getPurchaseId())).setHeader("Purchase").setAutoWidth(true);
        grid.addColumn(p -> shortId(p.getOrderId())).setHeader("Order").setAutoWidth(true);
        grid.addColumn(p -> p.getTicketCodes() == null ? 0 : p.getTicketCodes().size())
                .setHeader("Tickets").setAutoWidth(true);
        grid.addColumn(p -> "$" + p.getFinalPrice()).setHeader("Paid").setAutoWidth(true);
        grid.addColumn(p -> p.getPurchasedAt() == null ? "—" : p.getPurchasedAt().format(DATE_FMT))
                .setHeader("Purchased").setAutoWidth(true);

        grid.setItems(purchases);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        return wrapInCard(grid, "Your past orders (" + purchases.size() + ")");
    }

    // ── Card wrappers ────────────────────────────────────────────────────────

    private Div wrapInCard(com.vaadin.flow.component.Component body, String title) {
        Div section = new Div();
        section.getStyle()
                .set("background", "#f4f4f4")
                .set("padding", "40px 24px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        Div card = new Div();
        card.getStyle()
                .set("max-width", "1080px")
                .set("margin", "0 auto")
                .set("padding", "24px 28px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)");

        H2 h = new H2(title);
        h.getStyle().set("margin", "0 0 16px 0").set("font-size", "20px");

        card.add(h, body);
        section.add(card);
        return section;
    }

    private Div emptyStateCard(String title, String body) {
        Div section = new Div();
        section.getStyle()
                .set("background", "#f4f4f4")
                .set("padding", "90px 24px")
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        Div empty = new Div();
        empty.getStyle().set("text-align", "center").set("max-width", "430px");

        Div icon = new Div();
        icon.setText("🧾");
        icon.getStyle()
                .set("width", "72px").set("height", "72px").set("border-radius", "50%")
                .set("background", "white").set("display", "inline-flex")
                .set("align-items", "center").set("justify-content", "center")
                .set("box-shadow", "0 1px 5px rgba(0,0,0,0.12)")
                .set("font-size", "30px").set("margin-bottom", "22px");

        H2 t = new H2(title);
        t.getStyle().set("font-size", "25px").set("margin", "0 0 10px 0");

        Paragraph p = new Paragraph(body);
        p.getStyle().set("color", "#555").set("line-height", "1.5").set("margin", "0 0 26px 0");

        Button browse = new Button("Browse Events", e ->
                getUI().ifPresent(ui -> ui.navigate("main")));
        browse.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("padding", "12px 30px")
                .set("cursor", "pointer");

        empty.add(icon, t, p, browse);
        section.add(empty);
        return section;
    }

    private Div errorCard(String msg) {
        Notification.show(msg, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return emptyStateCard("Something went wrong",
                "We couldn't load your orders just now. Please refresh in a moment.");
    }

    // ── Header (unchanged shape, just kept readable) ─────────────────────────

    private Div createHeader() {
        Div header = new Div();
        header.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "26px 52px 0 52px")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        Div top = new Div();
        top.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "30px");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle()
                .set("margin", "0").set("font-size", "22px").set("font-weight", "900")
                .set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Span events = createNavItem("Events", "main");

        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        Span account = new Span("👤 My Account");
        account.getStyle().set("font-weight", "700").set("cursor", "pointer");
        account.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("profile")));

        top.add(logo, events, spacer, account);

        Paragraph crumbs = new Paragraph("Home / My Orders");
        crumbs.getStyle().set("margin", "34px 0 0 0").set("font-size", "14px");

        H1 title = new H1("My Orders");
        title.getStyle().set("font-size", "38px").set("margin", "14px 0 26px 0");

        Div tabs = new Div();
        tabs.getStyle().set("display", "flex").set("gap", "34px")
                .set("border-bottom", "1px solid rgba(255,255,255,0.3)");

        tabs.add(createTab("Active Orders", "active"),
                 createTab("Past Orders",   "past"));
        header.add(top, crumbs, title, tabs);
        return header;
    }

    private Span createTab(String text, String tabValue) {
        Span tab = new Span(text);
        tab.getStyle().set("padding", "14px 2px").set("font-weight", "700").set("cursor", "pointer");
        if (selectedTab.equals(tabValue)) {
            tab.getStyle().set("border-bottom", "4px solid white");
        }
        tab.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("orders?tab=" + tabValue)));
        return tab;
    }

    private Span createNavItem(String text, String route) {
        Span item = new Span(text);
        item.getStyle().set("font-weight", "700").set("cursor", "pointer");
        item.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));
        return item;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String shortId(java.util.UUID id) {
        if (id == null) return "—";
        String s = id.toString();
        return s.substring(0, Math.min(8, s.length()));
    }
}
