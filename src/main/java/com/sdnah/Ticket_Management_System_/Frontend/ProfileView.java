package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("profile")
public class ProfileView extends VerticalLayout implements BeforeEnterObserver {

    public ProfileView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(createHeader());
        add(createProfileContent());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object token = event.getUI().getSession().getAttribute("token");
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
                .set("font-weight", "900")
                .set("cursor", "pointer");

        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Div nav = new Div();
        nav.getStyle()
                .set("display", "flex")
                .set("gap", "40px")
                .set("align-items", "center");

        Span home = createNavItem("Home", "main");

        Span account = new Span("👤 My Account");
        account.getStyle().set("font-weight", "700");

        nav.add(home, account);
        header.add(logo, nav);

        return header;
    }

    private Div createProfileContent() {
        Div wrapper = new Div();

        wrapper.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "70px 20px");

        Div card = new Div();

        card.getStyle()
                .set("background", "white")
                .set("width", "430px")
                .set("padding", "42px")
                .set("border-radius", "14px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.08)")
                .set("text-align", "center");

        Div avatar = new Div();
        avatar.setText("👤");
        avatar.getStyle()
                .set("width", "95px")
                .set("height", "95px")
                .set("background", "#e8f0ff")
                .set("border-radius", "50%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "44px")
                .set("margin", "0 auto 26px auto");

        H1 title = new H1("My Profile");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "38px")
                .set("color", "#111827");

        Paragraph subtitle = new Paragraph("Manage your account and view your order history.");
        subtitle.getStyle()
                .set("color", "#6b7280")
                .set("font-size", "16px")
                .set("margin", "14px 0 34px 0")
                .set("line-height", "1.5");

        Button myDetails = createMainButton("My Details", "profile-details");
        Button myOrders = createMainButton("My Orders", "orders?tab=active");
        Button browseEvents = createSecondaryButton("Browse Events", "main");
        Button myComplaints = createSecondaryButton("My Complaints", "my-complaints");
        

        card.add(avatar, title, subtitle, myDetails, myOrders, myComplaints, browseEvents);
        wrapper.add(card);

        return wrapper;
    }

    private Span createNavItem(String text, String route) {
        Span item = new Span(text);

        item.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "700");

        item.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        return item;
    }

    private Button createMainButton(String text, String route) {
        Button button = new Button(text);
        button.setWidthFull();

        button.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("margin-bottom", "16px")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("cursor", "pointer");

        button.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        return button;
    }

    private Button createSecondaryButton(String text, String route) {
        Button button = new Button(text);
        button.setWidthFull();

        button.getStyle()
                .set("background", "white")
                .set("border", "2px solid #026cdf")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("cursor", "pointer");

        button.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        return button;
    }
}