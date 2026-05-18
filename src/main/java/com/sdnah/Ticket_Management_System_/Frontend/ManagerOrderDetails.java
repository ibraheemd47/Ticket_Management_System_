package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderItemDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Read-only / lightly mutating view a company manager opens to inspect a
 * single ActiveOrder + its purchase: items, totals, applied coupon, status.
 * Provides a "Cancel order" button (delegates to {@link ActiveOrderService}).
 *
 * <p>Routing convention used here:
 * the previous view sets the target orderId on the Vaadin session under
 * {@code "managerOrderId"} before navigating to {@code "manager/order"}.
 */
@Route("manager/order")
public class ManagerOrderDetails extends VerticalLayout implements BeforeEnterObserver {

    private static final String SESSION_TOKEN     = "token";
    private static final String SESSION_ORDER_ID  = "managerOrderId";

    private final ActiveOrderService orderService;

    private String token;
    private UUID orderId;
    private OrderDTO order;

    public ManagerOrderDetails(ActiveOrderService orderService) {
        this.orderService = orderService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(buildHeader());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Auth-light for now (signup/verify flow still WIP). When auth is back,
        // re-add: if (t == null) event.forwardTo(LoginView.class);
        Object t = UI.getCurrent().getSession().getAttribute(SESSION_TOKEN);
        Object o = UI.getCurrent().getSession().getAttribute(SESSION_ORDER_ID);

        this.token = t != null ? t.toString() : "dev-token";

        if (o == null) {
            add(emptyState("No order selected.",
                    "Click an order from the manager dashboard, or set the "
                            + "\"managerOrderId\" session attribute, then come back."));
            return;
        }
        try {
            this.orderId = UUID.fromString(o.toString());
            this.order   = orderService.getOrderById(orderId, token);
            add(buildContent(order));
        } catch (IllegalArgumentException badUuid) {
            add(emptyState("Invalid order id",
                    "Session value isn't a UUID: " + o));
        } catch (RuntimeException ex) {
            add(emptyState("Couldn't load order", ex.getMessage()));
        }
    }

    private Div emptyState(String title, String detail) {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "560px")
                .set("margin", "60px auto")
                .set("padding", "32px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)")
                .set("text-align", "center");
        H1 t = new H1(title);
        t.getStyle().set("margin", "0 0 8px 0").set("color", "#333");
        Paragraph d = new Paragraph(detail);
        d.getStyle().set("color", "#666").set("margin", "0");
        card.add(t, d);
        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    // ── Layout ───────────────────────────────────────────────────────────────

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

        Span back = new Span("⟵ Back");
        back.getStyle().set("cursor", "pointer").set("font-weight", "700");
        back.addClickListener(e -> UI.getCurrent().navigate("company"));

        header.add(logo, back);
        return header;
    }

    private Div buildContent(OrderDTO o) {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "880px")
                .set("margin", "40px auto")
                .set("padding", "32px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)");

        H1 title = new H1("Order " + shortId(o.getOrderId()));
        title.getStyle().set("margin", "0 0 4px 0");

        Span status = statusBadge(o.getStatus());

        Paragraph meta = new Paragraph(
                "Buyer: " + o.getbuyerId()
                        + "   ·   Event: " + shortId(o.getEventId())
                        + "   ·   Expires: " + (o.getExpiresAt() == null ? "—" : o.getExpiresAt().toString()));
        meta.getStyle().set("color", "#666").set("margin-top", "0");

        HorizontalLayout totals = new HorizontalLayout(
                cell("Original",   money(o.getOriginalPrice())),
                cell("Discount",   money(o.getDiscount())),
                cell("Final",      money(o.getFinalPrice())),
                cell("Coupon",     o.getAppliedCouponCode() == null ? "—" : o.getAppliedCouponCode()));
        totals.setSpacing(true);
        totals.getStyle().set("margin", "16px 0 8px 0");

        Grid<OrderItemDTO> grid = new Grid<>(OrderItemDTO.class, false);
        grid.addColumn(it -> shortId(safeUuid(it.getTicketId()))).setHeader("Ticket");
        grid.addColumn(it -> it.getSeatId() == null ? "GA" : String.valueOf(it.getSeatId())).setHeader("Seat");
        grid.addColumn(it -> shortId(it.getAreaId())).setHeader("Area");
        grid.addColumn(it -> money(it.getPrice())).setHeader("Price");
        grid.setItems(o.getItems());
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        Button cancel = new Button("Cancel order", e -> confirmAndCancel());
        cancel.getStyle()
                .set("background", "#c62828")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "10px 22px")
                .set("border-radius", "8px");
        cancel.setEnabled("ACTIVE".equalsIgnoreCase(o.getStatus()));

        HorizontalLayout actions = new HorizontalLayout(cancel);
        actions.getStyle().set("margin-top", "16px");

        card.add(title, status, meta, totals, grid, actions);

        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    private void confirmAndCancel() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Cancel this order?");
        dialog.setText("This will release all locks and mark the order CANCELLED. The buyer will see it gone from their pending list.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Cancel order");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> doCancel());
        dialog.open();
    }

    private void doCancel() {
        try {
            orderService.cancelOrder(orderId, token);
            Notification.show("Order cancelled", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate("company");
        } catch (RuntimeException ex) {
            Notification.show("Couldn't cancel: " + ex.getMessage(), 4000,
                            Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ── Small UI helpers ─────────────────────────────────────────────────────

    private static Div cell(String label, String value) {
        Div d = new Div();
        d.getStyle()
                .set("min-width", "120px")
                .set("padding", "10px 14px")
                .set("background", "#f6f8fb")
                .set("border-radius", "10px");
        Paragraph k = new Paragraph(label);
        k.getStyle().set("margin", "0").set("font-size", "12px").set("color", "#666");
        Paragraph v = new Paragraph(value);
        v.getStyle().set("margin", "0").set("font-weight", "700").set("font-size", "16px");
        d.add(k, v);
        return d;
    }

    private static Span statusBadge(String status) {
        Span s = new Span(status == null ? "?" : status);
        String bg = "#9e9e9e", fg = "white";
        if (status != null) {
            switch (status.toUpperCase()) {
                case "ACTIVE":    bg = "#1565c0"; break;
                case "COMPLETED": bg = "#2e7d32"; break;
                case "CANCELLED": bg = "#c62828"; break;
                case "EXPIRED":   bg = "#6a1b9a"; break;
            }
        }
        s.getStyle()
                .set("display", "inline-block")
                .set("background", bg)
                .set("color", fg)
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("font-weight", "700");
        return s;
    }

    private static String shortId(UUID id) {
        if (id == null) return "—";
        String s = id.toString();
        return s.substring(0, Math.min(8, s.length()));
    }

    private static UUID safeUuid(String s) {
        try { return s == null ? null : UUID.fromString(s); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static String money(java.math.BigDecimal x) {
        return x == null ? "—" : ("$" + x.toPlainString());
    }
}
