package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route("orders")
public class OrderDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private String selectedTab = "past";

    public OrderDetailsView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("font-family", "Arial, sans-serif")
                .set("background", "#f4f4f4");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters queryParameters = event.getLocation().getQueryParameters();
        Map<String, List<String>> params = queryParameters.getParameters();

        if (params.containsKey("tab") && !params.get("tab").isEmpty()) {
            selectedTab = params.get("tab").get(0);
        }

        removeAll();
        add(createHeader());
        add(createEmptyOrdersSection());
    }

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
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "30px");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "22px")
                .set("font-weight", "900");
                logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));
                logo.getStyle().set("cursor", "pointer");

        Span events = createNavItem("Events", "main");
        

        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        Span account = new Span("👤 My Account");
        account.getStyle()
                .set("font-weight", "700")
                .set("cursor", "pointer");

        account.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("profile"))
        );

        top.add(logo, events, spacer, account);

        Paragraph crumbs = new Paragraph("Home / My Orders");
        crumbs.getStyle()
                .set("margin", "34px 0 0 0")
                .set("font-size", "14px");

        H1 title = new H1("My Orders");
        title.getStyle()
                .set("font-size", "38px")
                .set("margin", "14px 0 26px 0");

        Div tabs = new Div();
        tabs.getStyle()
                .set("display", "flex")
                .set("gap", "34px")
                .set("border-bottom", "1px solid rgba(255,255,255,0.3)");

        Span active = createTab("Active Orders", "active");
        Span past = createTab("Past Orders", "past");

        tabs.add(active, past);
        header.add(top, crumbs, title, tabs);

        return header;
    }

    private Span createTab(String text, String tabValue) {
        Span tab = new Span(text);
        tab.getStyle()
                .set("padding", "14px 2px")
                .set("font-weight", "700")
                .set("cursor", "pointer");

        if (selectedTab.equals(tabValue)) {
            tab.getStyle().set("border-bottom", "4px solid white");
        }

        tab.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("orders?tab=" + tabValue))
        );

        return tab;
    }

    private Span createNavItem(String text, String route) {
        Span item = new Span(text);
        item.getStyle()
                .set("font-weight", "700")
                .set("cursor", "pointer");

        item.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(route))
        );

        return item;
    }

    private Div createEmptyOrdersSection() {
        Div section = new Div();
        section.getStyle()
                .set("background", "#f4f4f4")
                .set("padding", "90px 24px")
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        Div empty = new Div();
        empty.getStyle()
                .set("text-align", "center")
                .set("max-width", "430px");

        Div icon = new Div();
        icon.setText("🧾");
        icon.getStyle()
                .set("width", "72px")
                .set("height", "72px")
                .set("border-radius", "50%")
                .set("background", "white")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("box-shadow", "0 1px 5px rgba(0,0,0,0.12)")
                .set("font-size", "30px")
                .set("margin-bottom", "22px");

        boolean isActive = selectedTab.equals("active");

        H2 title = new H2(isActive ? "No active orders" : "No past orders");
        title.getStyle()
                .set("font-size", "25px")
                .set("margin", "0 0 10px 0");

        Paragraph text = new Paragraph(
                isActive
                        ? "Active orders you reserve or start purchasing will appear here."
                        : "Tickets you bought before will automatically appear here."
        );
        text.getStyle()
                .set("color", "#555")
                .set("line-height", "1.5")
                .set("margin", "0 0 26px 0");

        Button browse = new Button("Browse Events");
        browse.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "12px 30px")
                .set("cursor", "pointer");

        browse.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("main"))
        );

        empty.add(icon, title, text, browse);
        section.add(empty);

        return section;
    }
}