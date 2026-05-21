package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import static com.vaadin.flow.data.binder.Result.error;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("company-create")

public class CompanyCreationView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (UI.getCurrent().getSession().getAttribute("token") == null) {
            event.forwardTo(LoginView.class);
        }
    }

    private final company_managment_serivce companyService;

    public CompanyCreationView(company_managment_serivce companyService) {
        this.companyService = companyService ;
         setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        Div content = new Div(buildCompanyCard());
        content.getStyle()
                .set("max-width", "620px")
                .set("margin", "40px auto")
                .set("width", "100%");

        add(buildHeader(), content);
    }

    private Div buildCompanyCard() {
        Div card = card();

        H1 title = new H1("Create New Company");
        title.getStyle()
                .set("margin", "0 0 24px 0")
                .set("font-size", "26px")
                .set("color", "#111");

        IntegerField companyIdField = new IntegerField("Company ID");
        companyIdField.setPlaceholder("e.g. 1001");
        companyIdField.setWidthFull();
        companyIdField.setMin(1);

        TextField companyNameField = new TextField("Company Name");
        companyNameField.setPlaceholder("e.g. Live Nation Israel");
        companyNameField.setWidthFull();

        Button createBtn = new Button("Create Company");
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "10px 28px");

        Button cancelBtn = new Button("Cancel", e -> UI.getCurrent().navigate("company"));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        createBtn.addClickListener(e ->
                handleCreate(companyIdField, companyNameField)
        );

        card.add(
                title,
                companyIdField,
                companyNameField,
                new HorizontalLayout(createBtn, cancelBtn)
        );

        return card;
    }

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
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900")
                .set("cursor", "pointer");

        logo.addClickListener(e -> UI.getCurrent().navigate("main"));

        Div nav = new Div();
        nav.getStyle()
                .set("display", "flex")
                .set("gap", "32px")
                .set("align-items", "center");

        nav.add(
                clickable("Home", () -> UI.getCurrent().navigate("main")),
                clickable("Companies", () -> UI.getCurrent().navigate("company")),
                clickable("👤 My Account", () -> UI.getCurrent().navigate("profile"))
        );

        header.add(logo, nav);
        return header;
    }
    private static Span clickable(String text, Runnable onClick) {
        Span s = new Span(text);
        s.getStyle().set("cursor", "pointer").set("font-weight", "700");
        s.addClickListener(e -> onClick.run());
        return s;
    }
        private void handleCreate(IntegerField companyIdField, TextField companyNameField) {
        Integer companyId = companyIdField.getValue();
        String companyName = companyNameField.getValue();

        if (companyId == null) {
            error("Company ID is required");
            return;
        }

        if (companyId <= 0) {
            error("Company ID must be positive");
            return;
        }

        if (companyName == null || companyName.isBlank()) {
            error("Company name is required");
            return;
        }

        Object tokenObj = UI.getCurrent().getSession().getAttribute("token");

        if (tokenObj == null) {
            Notification.show("Not logged in — sign in first to create a company",
                    4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        String token = tokenObj.toString();

        try {
            companyService.openCompany(token, companyId, companyName.trim());

            Notification.show("Company \"" + companyName.trim() + "\" created successfully!",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            UI.getCurrent().getSession().setAttribute("managingCompanyId", companyId);

            UI.getCurrent().navigate("company");

        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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
}
