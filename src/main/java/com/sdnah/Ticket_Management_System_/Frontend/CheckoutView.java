package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Route("checkout")
public class CheckoutView extends VerticalLayout implements BeforeEnterObserver {

    private final TicketService ticketService;
    private final PolicyRepository policyRepo;
    private final Div contentArea = new Div();
    private final ActiveOrderService order;

    public CheckoutView(TicketService ticketService, PolicyRepository policyRepo, ActiveOrderService order) {
        this.ticketService = ticketService;
        this.policyRepo = policyRepo;
        this.order = order;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(createHeader());
        contentArea.getStyle().set("width", "100%");
        add(contentArea);
    }

    // ── Route guard + data load ────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public void beforeEnter(BeforeEnterEvent event) {
        Object token = event.getUI().getSession().getAttribute("token");
        if (token == null) {
            event.rerouteTo("login");
            return;
        }
        String orderIdStr = (String) event.getUI().getSession().getAttribute("checkoutOrderId");
        List<Map<String, String>> items = (List<Map<String, String>>) event.getUI().getSession()
                .getAttribute("checkoutItems");

        String showName = (String) event.getUI().getSession().getAttribute("checkoutShowName");
        String eventIdStr = (String) event.getUI().getSession().getAttribute("checkoutEventId");

        if (orderIdStr == null || orderIdStr.isBlank() || items == null || items.isEmpty()) {
            event.rerouteTo("main");
            return;
        }
        UUID orderId;

        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (Exception ex) {
            event.rerouteTo("main");
            return;
        }

        contentArea.removeAll();
        contentArea.add(buildCheckoutContent(orderId, items, showName, eventIdStr));

        // List<String> ticketIds = (List<String>)
        // event.getUI().getSession().getAttribute("checkoutTicketIds");
        // List<Map<String, String>> items = (List<Map<String, String>>)
        // event.getUI().getSession()
        // .getAttribute("checkoutItems");
        // String userId = (String)
        // event.getUI().getSession().getAttribute("checkoutUserId");

        // if (ticketIds == null || ticketIds.isEmpty() || items == null ||
        // items.isEmpty()) {
        // event.rerouteTo("main");
        // return;
        // }

        // contentArea.add(buildCheckoutContent(ticketIds, items, userId, showName,
        // eventIdStr));
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private Div createHeader() {
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
        logo.getStyle()
                .set("margin", "0").set("font-size", "24px")
                .set("font-weight", "900").set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Div nav = new Div();
        nav.getStyle().set("display", "flex").set("gap", "40px").set("align-items", "center");
        nav.add(
                createNavItem("Home", "main"),
                createNavItem("My Orders", "orders?tab=active"),
                createNavItem("👤 My Account", "profile"));

        header.add(logo, nav);
        return header;
    }

    // ── Two-column checkout layout ─────────────────────────────────────────────

    private Div buildCheckoutContent(UUID orderId, // List<String> ticketIds,
            List<Map<String, String>> items,
            // String userId,
            String showName,
            String eventIdStr) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "60px 20px");

        Div layout = new Div();
        layout.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1.2fr 0.8fr")
                .set("gap", "28px")
                .set("width", "900px")
                .set("max-width", "100%");

        BigDecimal rawTotal = computeTotal(items);
        BigDecimal[] finalTotalRef = { rawTotal }; // mutated by coupon section
        layout.add(
                buildPaymentCard(orderId),
                buildSummaryCard(items, rawTotal, finalTotalRef, eventIdStr, showName, orderId));
        // layout.add(
        // buildPaymentCard(ticketIds, userId, finalTotalRef),
        // buildSummaryCard(items, rawTotal, finalTotalRef, eventIdStr, showName));
        wrapper.add(layout);
        return wrapper;
    }

    // ── Left card: payment form ────────────────────────────────────────────────
    private Div buildPaymentCard(UUID orderId) {
        Div card = createCard();

        H1 title = new H1("Checkout");
        title.getStyle().set("font-size", "34px").set("margin", "0 0 8px 0");

        Paragraph subtitle = new Paragraph("Enter your details to complete the purchase.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 28px 0");

        TextField fullName = new TextField("Full Name");
        fullName.setWidthFull();

        TextField email = new TextField("Email");
        email.setWidthFull();

        TextField cardNumber = new TextField("Card Number");
        cardNumber.setWidthFull();
        cardNumber.setPlaceholder("1234 5678 9012 3456");

        Div rowDiv = new Div();
        rowDiv.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "16px");

        TextField expiry = new TextField("Expiry Date");
        expiry.setPlaceholder("MM/YY");

        TextField cvv = new TextField("CVV");
        cvv.setPlaceholder("123");

        rowDiv.add(expiry, cvv);

        Button confirm = new Button("Confirm Purchase");
        confirm.setWidthFull();
        confirm.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("margin-top", "24px");

        confirm.addClickListener(e -> {
            try {
                if (fullName.isEmpty()
                        || email.isEmpty()
                        || cardNumber.isEmpty()
                        || expiry.isEmpty()
                        || cvv.isEmpty()) {
                    Notification.show("Please fill all checkout fields");
                    return;
                }

                Object tokenObj = getUI()
                        .map(ui -> ui.getSession().getAttribute("token"))
                        .orElse(null);

                if (tokenObj == null || tokenObj.toString().isBlank()) {
                    Notification.show("Session expired — please log in again");
                    getUI().ifPresent(ui -> ui.navigate("login"));
                    return;
                }

                String token = tokenObj.toString();

                String cleanCard = cardNumber.getValue().replaceAll("\\s+", "");
                String last4 = cleanCard.length() >= 4
                        ? cleanCard.substring(cleanCard.length() - 4)
                        : cleanCard;

                PaymentDetailsDTO paymentDTO = new PaymentDetailsDTO(
                        "CARD-" + last4,
                        fullName.getValue().trim(),
                        "CREDIT_CARD");

                PurchaseDTO purchase = order.checkout(orderId, token, paymentDTO);

                getUI().ifPresent(ui -> {
                    ui.getSession().setAttribute("checkoutOrderId", null);
                    ui.getSession().setAttribute("checkoutTicketIds", null);
                    ui.getSession().setAttribute("checkoutItems", null);
                    ui.getSession().setAttribute("checkoutUserId", null);
                    ui.getSession().setAttribute("checkoutShowName", null);
                    ui.getSession().setAttribute("checkoutEventId", null);
                });

                Notification.show(
                        "Purchase completed! Purchase ID: " + shortId(purchase.getPurchaseId()),
                        4000,
                        Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                getUI().ifPresent(ui -> ui.navigate("orders?tab=past"));

            } catch (Exception ex) {
                Notification.show(
                        "Checkout failed: " + ex.getMessage(),
                        5000,
                        Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancel = new Button("Cancel — go back");
        cancel.setWidthFull();
        cancel.getStyle()
                .set("background", "white")
                .set("border", "2px solid #026cdf")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("margin-top", "12px");

        cancel.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("orders?tab=active")));

        card.add(title, subtitle, fullName, email, cardNumber, rowDiv, confirm, cancel);
        return card;
    }

    private static String shortId(UUID id) {
        return id == null ? "—" : id.toString().substring(0, 8);
    }

    private Div buildPaymentCard(List<String> ticketIds,
            String userId,
            BigDecimal[] finalTotalRef) {
        Div card = createCard();

        H1 title = new H1("Checkout");
        title.getStyle().set("font-size", "34px").set("margin", "0 0 8px 0");

        Paragraph subtitle = new Paragraph("Enter your details to complete the purchase.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 28px 0");

        TextField fullName = new TextField("Full Name");
        fullName.setWidthFull();
        TextField email = new TextField("Email");
        email.setWidthFull();
        TextField cardNumber = new TextField("Card Number");
        cardNumber.setWidthFull();
        cardNumber.setPlaceholder("1234 5678 9012 3456");

        Div rowDiv = new Div();
        rowDiv.getStyle().set("display", "grid")
                .set("grid-template-columns", "1fr 1fr").set("gap", "16px");
        TextField expiry = new TextField("Expiry Date");
        expiry.setPlaceholder("MM/YY");
        TextField cvv = new TextField("CVV");
        cvv.setPlaceholder("123");
        rowDiv.add(expiry, cvv);

        // ── Confirm button ────────────────────────────────────────────────────
        Button confirm = new Button("Confirm Purchase");
        confirm.setWidthFull();
        confirm.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("padding", "14px")
                .set("border-radius", "8px").set("margin-top", "24px");

        confirm.addClickListener(e -> {
            try {
                if (fullName.isEmpty() || email.isEmpty()
                        || cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                    Notification.show("Please fill all checkout fields");
                    return;
                }
                if (userId == null || userId.isBlank()) {
                    Notification.show("Session expired — please log in again");
                    getUI().ifPresent(ui -> ui.navigate("login"));
                    return;
                }

                UUID userUUID = UUID.fromString(userId);
                int confirmed = 0;
                for (String tid : ticketIds) {
                    try {
                        if (ticketService.confirmPurchase(UUID.fromString(tid), userUUID)) {
                            confirmed++;
                        }
                    } catch (Exception ex) {
                        Notification.show("Failed to confirm a ticket: " + ex.getMessage(),
                                4000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }

                if (confirmed == ticketIds.size()) {
                    // Clear checkout session data
                    getUI().ifPresent(ui -> {
                        ui.getSession().setAttribute("checkoutTicketIds", null);
                        ui.getSession().setAttribute("checkoutItems", null);
                        ui.getSession().setAttribute("checkoutUserId", null);
                        ui.getSession().setAttribute("checkoutShowName", null);
                        ui.getSession().setAttribute("checkoutEventId", null);
                    });
                    Notification.show("Purchase completed! Enjoy the show 🎉",
                            4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    getUI().ifPresent(ui -> ui.navigate("orders?tab=past"));
                } else {
                    Notification.show(confirmed + " of " + ticketIds.size()
                            + " tickets confirmed. Check My Orders for details.",
                            5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } catch (Exception ex) {
                Notification.show("An error occurred: " + ex.getMessage(),
                        4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        // ── Cancel button ─────────────────────────────────────────────────────
        Button cancel = new Button("Cancel — go back");
        cancel.setWidthFull();
        cancel.getStyle()
                .set("background", "white").set("border", "2px solid #026cdf")
                .set("color", "#026cdf").set("font-weight", "700")
                .set("padding", "14px").set("border-radius", "8px").set("margin-top", "12px");
        cancel.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("orders?tab=active")));

        card.add(title, subtitle, fullName, email, cardNumber, rowDiv, confirm, cancel);
        return card;
    }

    // ── Right card: order summary + coupon ─────────────────────────────────────

    // private Div buildSummaryCard(List<Map<String, String>> items,
    // BigDecimal rawTotal,
    // BigDecimal[] finalTotalRef,
    // String eventIdStr,
    // String showName)
    private Div buildSummaryCard(List<Map<String, String>> items,
            BigDecimal rawTotal,
            BigDecimal[] finalTotalRef,
            String eventIdStr,
            String showName,
            UUID orderId) {
        Div card = createCard();

        H2 titleEl = new H2("Order Summary");
        titleEl.getStyle().set("margin", "0 0 8px 0").set("font-size", "26px");
        card.add(titleEl);

        if (showName != null && !showName.isBlank()) {
            Paragraph showEl = new Paragraph("Show: " + showName);
            showEl.getStyle()
                    .set("color", "#555").set("margin", "0 0 20px 0").set("font-size", "14px");
            card.add(showEl);
        } else {
            Div spacer = new Div();
            spacer.getStyle().set("height", "12px");
            card.add(spacer);
        }

        // ── Line items ────────────────────────────────────────────────────────
        for (Map<String, String> item : items) {
            String desc = item.getOrDefault("description", "Ticket");
            String uStr = item.getOrDefault("unitPrice", "0");
            String qStr = item.getOrDefault("quantity", "1");
            int qty = parseIntSafe(qStr, 1);
            BigDecimal line;
            try {
                line = new BigDecimal(uStr)
                        .multiply(BigDecimal.valueOf(qty))
                        .setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                line = BigDecimal.ZERO;
            }

            Div lineRow = new Div();
            lineRow.getStyle()
                    .set("display", "flex").set("justify-content", "space-between")
                    .set("font-size", "14px").set("margin-bottom", "8px").set("color", "#333");
            String label = qty > 1 ? desc + " × " + qty : desc;
            Span left = new Span(label);
            Span right = new Span("$" + line.toPlainString());
            right.getStyle().set("font-weight", "600");
            lineRow.add(left, right);
            card.add(lineRow);
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Div divider = new Div();
        divider.getStyle().set("border-top", "1px solid #e0e0e0").set("margin", "10px 0 12px");
        card.add(divider);

        // ── Subtotal ──────────────────────────────────────────────────────────
        card.add(buildPriceRow(
                "Subtotal:",
                "$" + rawTotal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                false, null));

        // ── Discount row (hidden until coupon applied) ────────────────────────
        Span discountValueSpan = new Span("-$0.00");
        discountValueSpan.getStyle().set("color", "#2e7d32").set("font-weight", "600");
        Span discountLabelSpan = new Span("Discount:");
        discountLabelSpan.getStyle().set("color", "#2e7d32");
        Div discountRow = new Div(discountLabelSpan, discountValueSpan);
        discountRow.getStyle()
                .set("display", "flex").set("justify-content", "space-between")
                .set("font-size", "14px").set("margin-bottom", "6px");
        discountRow.setVisible(false);
        card.add(discountRow);

        // ── Total ─────────────────────────────────────────────────────────────
        Span totalValueSpan = new Span(
                "$" + rawTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
        totalValueSpan.getStyle()
                .set("font-weight", "700").set("font-size", "18px").set("color", "#026cdf");
        Span totalLabelSpan = new Span("Total:");
        totalLabelSpan.getStyle().set("font-weight", "700").set("font-size", "16px");
        Div totalRow = new Div(totalLabelSpan, totalValueSpan);
        totalRow.getStyle()
                .set("display", "flex").set("justify-content", "space-between")
                .set("margin", "6px 0 20px");
        card.add(totalRow);

        // ── Coupon section ────────────────────────────────────────────────────
        Span couponToggle = new Span("🏷 Have a coupon code?");
        couponToggle.getStyle()
                .set("color", "#026cdf").set("cursor", "pointer")
                .set("font-weight", "700").set("font-size", "14px");

        Div couponBox = new Div();
        couponBox.setVisible(false);
        couponBox.getStyle()
                .set("margin-top", "10px").set("display", "flex")
                .set("gap", "8px").set("align-items", "flex-end");

        TextField couponField = new TextField();
        couponField.setPlaceholder("Enter coupon code");
        couponField.getStyle().set("flex", "1");

        Button applyBtn = new Button("Apply");
        applyBtn.getStyle()
                .set("background", "#026cdf").set("color", "white")
                .set("font-weight", "700").set("border-radius", "6px")
                .set("padding", "0 16px");

        couponBox.add(couponField, applyBtn);
        couponToggle.addClickListener(ev -> couponBox.setVisible(!couponBox.isVisible()));

        applyBtn.addClickListener(ev -> {
            String code = couponField.getValue();
            if (code == null || code.isBlank()) {
                Notification.show("Please enter a coupon code");
                return;
            }

            // // ── Locate matching, non-expired CouponDiscountRule ───────────────
            // CouponDiscountRule matched = null;
            // if (eventIdStr != null && !eventIdStr.isBlank()) {
            // try {
            // List<Policy> policies =
            // policyRepo.findByEventId(UUID.fromString(eventIdStr));
            // for (Policy p : policies) {
            // if (p instanceof DiscountPolicy dp
            // && dp.getRootRule() instanceof CouponDiscountRule cr
            // && cr.getCouponCode().equalsIgnoreCase(code.trim())) {
            // if (cr.getExpiry() == null || LocalDateTime.now().isBefore(cr.getExpiry())) {
            // matched = cr;
            // break;
            // }
            // }
            // }
            // } catch (Exception ignored) {
            // }
            // }

            // if (matched == null) {
            // Notification.show("Invalid or expired coupon code",
            // 3000, Notification.Position.TOP_CENTER)
            // .addThemeVariants(NotificationVariant.LUMO_ERROR);
            // return;
            // }

            // // ── Apply discount ────────────────────────────────────────────────
            // double pct = matched.getPercentage();
            // BigDecimal disc = rawTotal.multiply(BigDecimal.valueOf(pct / 100.0))
            // .setScale(2, RoundingMode.HALF_UP);
            // BigDecimal newTotal = rawTotal.subtract(disc).max(BigDecimal.ZERO)
            // .setScale(2, RoundingMode.HALF_UP);

            // finalTotalRef[0] = newTotal;

            // discountValueSpan.setText("-$" + disc.toPlainString());
            // discountRow.setVisible(true);
            // totalValueSpan.setText("$" + newTotal.toPlainString());

            // couponField.setReadOnly(true);
            // applyBtn.setEnabled(false);
            // couponToggle.getStyle()
            // .set("text-decoration", "line-through").set("color", "#aaa");

            // Notification.show(String.format("%.0f%% coupon applied!", pct),
            // 3000, Notification.Position.TOP_CENTER)
            // .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // }

            try {
                Object tokenObj = getUI()
                        .map(ui -> ui.getSession().getAttribute("token"))
                        .orElse(null);

                if (tokenObj == null || tokenObj.toString().isBlank()) {
                    Notification.show("Session expired — please log in again");
                    return;
                }

                OrderDTO updatedOrder = order.applyCoupon(
                        orderId,
                        tokenObj.toString(),
                        code.trim());

                BigDecimal discount = updatedOrder.getDiscount()
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal newTotal = updatedOrder.getFinalPrice()
                        .setScale(2, RoundingMode.HALF_UP);

                finalTotalRef[0] = newTotal;

                discountValueSpan.setText("-$" + discount.toPlainString());
                discountRow.setVisible(true);
                totalValueSpan.setText("$" + newTotal.toPlainString());

                couponField.setReadOnly(true);
                applyBtn.setEnabled(false);
                couponToggle.getStyle()
                        .set("text-decoration", "line-through")
                        .set("color", "#aaa");

                Notification.show("Coupon applied!",
                        3000,
                        Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Coupon failed: " + ex.getMessage(),
                        3000,
                        Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }

        );

        card.add(couponToggle, couponBox);
        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BigDecimal computeTotal(List<Map<String, String>> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, String> item : items) {
            try {
                BigDecimal unit = new BigDecimal(item.getOrDefault("unitPrice", "0"));
                int qty = parseIntSafe(item.getOrDefault("quantity", "1"), 1);
                total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
            } catch (NumberFormatException ignored) {
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** A flex row with label on the left and value on the right. */
    private Div buildPriceRow(String label, String value,
            boolean bold, String color) {
        Span lbl = new Span(label);
        Span val = new Span(value);
        if (bold) {
            lbl.getStyle().set("font-weight", "700");
            val.getStyle().set("font-weight", "700");
        }
        if (color != null)
            val.getStyle().set("color", color);
        Div row = new Div(lbl, val);
        row.getStyle()
                .set("display", "flex").set("justify-content", "space-between")
                .set("font-size", "14px").set("margin-bottom", "6px");
        return row;
    }

    private Div createCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("padding", "34px")
                .set("border-radius", "14px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.08)")
                .set("box-sizing", "border-box");
        return card;
    }

    private Span createNavItem(String text, String route) {
        Span item = new Span(text);
        item.getStyle().set("cursor", "pointer").set("font-weight", "700");
        item.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));
        return item;
    }
}
