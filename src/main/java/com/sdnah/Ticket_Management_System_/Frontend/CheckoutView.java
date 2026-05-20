package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("checkout")
public class CheckoutView extends VerticalLayout implements BeforeEnterObserver {

    public CheckoutView() {

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(createHeader());
        add(createCheckoutContent());
    }

 @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object token = event.getUI().getSession().getAttribute("token");

        // If there is no token, reroute them to the login page immediately
        if (token == null) {
            event.rerouteTo("login");
        }
    }
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
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));
        logo.getStyle().set("cursor", "pointer");

        Div nav = new Div();

        nav.getStyle()
                .set("display", "flex")
                .set("gap", "40px")
                .set("align-items", "center");

        Span home = createNavItem("Home", "");
        Span orders = createNavItem("My Orders", "orders?tab=active");
        Span account = createNavItem("👤 My Account", "profile");

        nav.add(home, orders, account);

        header.add(logo, nav);

        return header;
    }

    private Div createCheckoutContent() {

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

        Div paymentCard = createPaymentCard();
        Div summaryCard = createSummaryCard();

        layout.add(paymentCard, summaryCard);

        wrapper.add(layout);

        return wrapper;
    }

    private Div createPaymentCard() {

        Div card = createCard();

        H1 title = new H1("Checkout");

        title.getStyle()
                .set("font-size", "34px")
                .set("margin", "0 0 8px 0");

        Paragraph subtitle = new Paragraph(
                "Enter your details to complete the purchase."
        );

        subtitle.getStyle()
                .set("color", "#6b7280")
                .set("margin", "0 0 28px 0");

        TextField fullName = new TextField("Full Name");
        fullName.setWidthFull();

        TextField email = new TextField("Email");
        email.setWidthFull();

        TextField cardNumber = new TextField("Card Number");
        cardNumber.setWidthFull();
        cardNumber.setPlaceholder("1234 5678 9012 3456");

        Div row = new Div();

        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "16px");

        TextField expiry = new TextField("Expiry Date");
        expiry.setPlaceholder("MM/YY");

        TextField cvv = new TextField("CVV");
        cvv.setPlaceholder("123");

        row.add(expiry, cvv);

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

            if (fullName.isEmpty()
                    || email.isEmpty()
                    || cardNumber.isEmpty()
                    || expiry.isEmpty()
                    || cvv.isEmpty()) {

                Notification.show("Please fill all checkout fields");
                return;
            }

            Notification.show("Purchase completed successfully");

            getUI().ifPresent(ui ->
                    ui.navigate("orders?tab=past")
            );
        });

        Button cancel = new Button("Cancel");

        cancel.setWidthFull();

        cancel.getStyle()
                .set("background", "white")
                .set("border", "2px solid #026cdf")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("margin-top", "12px");

        cancel.addClickListener(e ->
                getUI().ifPresent(ui ->
                        ui.navigate("orders?tab=active")
                )
        );

        card.add(
                title,
                subtitle,
                fullName,
                email,
                cardNumber,
                row,
                confirm,
                cancel
        );

        return card;
    }

   private Div createSummaryCard() {
    Div card = createCard();
    H2 title = new H2("Order Summary");
    title.getStyle().set("margin", "0 0 24px 0").set("font-size", "26px");

    // Pre-total display
    Span preTotalLabel = new Span("Total: ");
    Span preTotalValue = new Span("$100.00"); // Mock price
    Div summaryRow = new Div(preTotalLabel, preTotalValue);

    // Coupon Toggle
    Span couponToggle = new Span("Do you have a Coupon code?");
    couponToggle.getStyle().set("color", "#026cdf").set("cursor", "pointer").set("font-weight", "bold");

    VerticalLayout couponContainer = new VerticalLayout();
    couponContainer.setVisible(false); // Hidden by default
    TextField couponField = new TextField();
    couponField.setPlaceholder("Enter code");
    Button applyBtn = new Button("Apply");

    couponToggle.addClickListener(e -> couponContainer.setVisible(!couponContainer.isVisible()));
    couponContainer.add(couponField, applyBtn);

    // Coupon logic
    applyBtn.addClickListener(e -> {
        // Assume you have a DiscountService to check policy
        // boolean isValid = discountService.isValid(couponField.getValue());
        boolean isValid = couponField.getValue().equals("SAVE20"); // Mock check

        if (isValid) {
            // Update UI
            preTotalValue.getStyle().set("text-decoration", "line-through").set("color", "gray");
            
            Span discountValue = new Span("Discount: -$20.00");
            discountValue.getStyle().set("color", "green");
            
            Span newTotal = new Span("New Total: $80.00");
            newTotal.getStyle().set("font-weight", "bold").set("font-size", "20px");
            
            card.add(discountValue, newTotal);
            Notification.show("Coupon applied!");
        } else {
            Notification.show("Invalid coupon code.");
        }
    });

    card.add(title, summaryRow, couponToggle, couponContainer);
    return card;
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

        item.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "700");

        item.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(route))
        );

        return item;
    }
}