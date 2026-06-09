package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Frontend.Presenters.CompanyCreationPresenter;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("company-create")
public class CompanyCreationView extends VerticalLayout
        implements BeforeEnterObserver, CompanyCreationPresenter.View {

    private final CompanyCreationPresenter presenter;

    // Form fields promoted to instance fields so handleCreate can read them
    private final IntegerField companyIdField;
    private final TextField    companyNameField;

    public CompanyCreationView(CompanyCreationPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        companyIdField   = new IntegerField("Company ID");
        companyNameField = new TextField("Company Name");

        Div content = new Div(buildCompanyCard());
        content.getStyle()
                .set("max-width", "620px")
                .set("margin", "40px auto")
                .set("width", "100%");

        add(buildHeader(), content);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (UI.getCurrent().getSession().getAttribute("token") == null) {
            event.forwardTo(LoginView.class);
        }
    }

    // ── Card ─────────────────────────────────────────────────────────────────

    private Div buildCompanyCard() {
        Div card = card();

        H1 title = new H1("Create New Company");
        title.getStyle()
                .set("margin", "0 0 24px 0")
                .set("font-size", "26px")
                .set("color", "#111");

        companyIdField.setPlaceholder("e.g. 1001");
        companyIdField.setWidthFull();
        companyIdField.setMin(1);

        companyNameField.setPlaceholder("e.g. Live Nation Israel");
        companyNameField.setWidthFull();

        Button createBtn = new Button("Create Company",
                e -> presenter.handleCreate(companyIdField.getValue(), companyNameField.getValue()));
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "10px 28px");

        Button cancelBtn = new Button("Cancel",
                e -> UI.getCurrent().navigate("company"));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        card.add(title, companyIdField, companyNameField,
                new HorizontalLayout(createBtn, cancelBtn));
        return card;
    }

    // ── Header ────────────────────────────────────────────────────────────────

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
                clickable("Home",       () -> UI.getCurrent().navigate("main")),
                clickable("Companies",  () -> UI.getCurrent().navigate("company")),
                clickable("👤 My Account", () -> UI.getCurrent().navigate("profile")));

        header.add(logo, nav);
        return header;
    }

    // ── CompanyCreationPresenter.View implementation ──────────────────────────

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
        Notification.show(message, 4000, Notification.Position.MIDDLE)
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

    // ── UI helpers ────────────────────────────────────────────────────────────

    private static Span clickable(String text, Runnable onClick) {
        Span s = new Span(text);
        s.getStyle().set("cursor", "pointer").set("font-weight", "700");
        s.addClickListener(e -> onClick.run());
        return s;
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
