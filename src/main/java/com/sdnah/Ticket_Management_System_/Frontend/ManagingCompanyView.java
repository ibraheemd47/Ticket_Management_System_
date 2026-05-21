package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Manager / owner dashboard for a single company. Three tabs:
 *  • Events   — list events, open event details, create new event.
 *  • Roles    — owners list + managers w/ permissions, appoint / remove.
 *  • Policies — placeholder for the discount + purchase policy editor
 *               (policy implementation lives on main; wire this tab to it
 *               once the policy service API is available).
 *
 * <p>Convention: the previous view sets the target {@code companyId} on the
 * Vaadin session under {@code "managingCompanyId"} before navigating to
 * {@code "company"}.
 */
@Route("company")
public class ManagingCompanyView extends VerticalLayout implements BeforeEnterObserver {

    private static final String SESSION_TOKEN      = "token";
    private static final String SESSION_COMPANY_ID = "managingCompanyId";

    private final company_managment_serivce companyService;

    private String token;
    private int companyId;

    private final Div tabContent = new Div();
    private final Tab eventsTab   = new Tab("Events");
    private final Tab rolesTab    = new Tab("Roles");
    private final Tab policiesTab = new Tab("Policies");

    public ManagingCompanyView(company_managment_serivce companyService) {
        this.companyService = companyService;

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
        Object c = UI.getCurrent().getSession().getAttribute(SESSION_COMPANY_ID);

        this.token     = t != null ? t.toString()                : "dev-token";
        this.companyId = c != null ? Integer.parseInt(c.toString()) : 1;

        add(buildShell());
        renderEventsTab();
    }

    // ── Shell ────────────────────────────────────────────────────────────────

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
                clickable("Home", () -> UI.getCurrent().navigate("main")),
                clickable("👤 My Account", () -> UI.getCurrent().navigate("profile")));
        header.add(logo, nav);
        return header;
    }

    private Component buildShell() {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "1080px")
                .set("margin", "40px auto")
                .set("padding", "24px 32px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)");

        H1 title = new H1("Company #" + companyId);
        title.getStyle().set("margin", "0 0 16px 0");

        Tabs tabs = new Tabs(eventsTab, rolesTab, policiesTab);
        tabs.addSelectedChangeListener(e -> {
            tabContent.removeAll();
            Tab selected = e.getSelectedTab();
            if (selected == eventsTab)         renderEventsTab();
            else if (selected == rolesTab)     renderRolesTab();
            else if (selected == policiesTab)  renderPoliciesTab();
        });

        tabContent.getStyle().set("padding-top", "16px");

        card.add(title, tabs, tabContent);

        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    // ── Tab: Events ──────────────────────────────────────────────────────────

    private void renderEventsTab() {
        tabContent.removeAll();

        List<UUID> eventIds;
        try {
            eventIds = companyService.getAllEventsByCompany(companyId);
        } catch (RuntimeException ex) {
            tabContent.add(error("Couldn't load events: " + ex.getMessage()));
            return;
        }

        Button addEvent = new Button("+ New event", e -> {
            UI.getCurrent().getSession().setAttribute("managingCompanyId", this.companyId);
            UI.getCurrent().navigate("event-create");
        });
        addEvent.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Grid<UUID> grid = new Grid<>(UUID.class, false);
        grid.addColumn(id -> id.toString().substring(0, 8)).setHeader("Event ID");
        grid.addComponentColumn(id -> {
            Button open = new Button("Open", ev -> {
                UI.getCurrent().getSession().setAttribute("eventId", id.toString());
                UI.getCurrent().navigate("EventDetails");
            });
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return open;
        }).setHeader("");
        grid.setItems(eventIds);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        tabContent.add(addEvent, grid);
    }

    // ── Tab: Roles ───────────────────────────────────────────────────────────

    private void renderRolesTab() {
        tabContent.removeAll();

        CompanyRolesViewDTO roles;
        try {
            roles = companyService.viewRolesAndPermissions(token, companyId);
        } catch (RuntimeException ex) {
            tabContent.add(error("Couldn't load roles: " + ex.getMessage()));
            return;
        }

        Div section = new Div();
        section.add(sectionTitle("Founder"));
        section.add(new Paragraph(roles.getFounderId()));

        section.add(sectionTitle("Owners"));
        section.add(buildOwnersList(roles.getOwnerIds()));
        section.add(appointBox("Appoint owner",
                memberId -> companyService.appointAdditionalOwner(token, companyId, memberId)));

        section.add(sectionTitle("Managers + permissions"));
        section.add(buildManagersGrid(roles.getManagerPermissions()));
        section.add(appointBox("Appoint manager",
                memberId -> companyService.appointManager(token, companyId, memberId, Set.of())));

        tabContent.add(section);
    }

    private Component buildOwnersList(List<String> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return new Paragraph("No additional owners.");
        }
        Div list = new Div();
        list.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "8px");
        for (String oid : ownerIds) {
            Span chip = new Span(oid);
            chip.getStyle()
                    .set("padding", "6px 12px")
                    .set("background", "#e3f2fd")
                    .set("border-radius", "999px")
                    .set("font-size", "13px");
            list.add(chip);
        }
        return list;
    }

    private Component buildManagersGrid(Map<String, Set<CompanyPermission>> managerPermissions) {
        if (managerPermissions == null || managerPermissions.isEmpty()) {
            return new Paragraph("No managers yet.");
        }
        Grid<Map.Entry<String, Set<CompanyPermission>>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey).setHeader("Manager");
        grid.addColumn(e -> String.join(", ",
                e.getValue().stream().map(Enum::name).sorted().toList())).setHeader("Permissions");
        grid.addComponentColumn(entry -> {
            Button remove = new Button("Remove", ev -> {
                try {
                    companyService.removeManagerAppointment(token, companyId, entry.getKey());
                    Notification.show("Removed " + entry.getKey(), 2500,
                                    Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    renderRolesTab();
                } catch (RuntimeException ex) {
                    Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return remove;
        }).setHeader("");
        grid.setItems(managerPermissions.entrySet());
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        return grid;
    }

    private Component appointBox(String label, java.util.function.Consumer<String> action) {
        TextField id = new TextField();
        id.setPlaceholder("member id");
        Button go = new Button(label, e -> {
            String v = id.getValue();
            if (v == null || v.isBlank()) {
                Notification.show("Member id required", 2500, Notification.Position.MIDDLE);
                return;
            }
            try {
                action.accept(v.trim());
                Notification.show("Done", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                renderRolesTab();
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        go.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(id, go);
        row.getStyle().set("margin-top", "8px");
        return row;
    }

    // ── Tab: Policies ────────────────────────────────────────────────────────

    private void renderPoliciesTab() {
        tabContent.removeAll();

        // Placeholder. The Composite-pattern policy editor lives on main; wire
        // this tab to PolicyService once the API is merged here. UI shape:
        //   - Purchase policy: tree of predicates with AND/OR group nodes.
        //   - Discount policy: list of rules (simple / conditional / coupon)
        //                      with composition mode (MAX vs SUM).
        Div placeholder = new Div();
        placeholder.getStyle()
                .set("padding", "32px")
                .set("text-align", "center")
                .set("color", "#666");

        H2 t = new H2("Policies (coming up)");
        t.getStyle().set("margin", "0 0 8px 0").set("color", "#333");

        Paragraph p1 = new Paragraph(
                "The discount + purchase policy editor will plug in here once the policy API is available on this branch.");
        Paragraph p2 = new Paragraph(
                "Will support: simple, conditional, and coupon discounts; AND/OR composition of purchase rules.");

        Button refresh = new Button("Reload policies", e -> renderPoliciesTab());
        refresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        placeholder.add(t, p1, p2, refresh);
        tabContent.add(placeholder);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static H2 sectionTitle(String text) {
        H2 h = new H2(text);
        h.getStyle()
                .set("font-size", "18px")
                .set("margin", "20px 0 8px 0")
                .set("color", "#333");
        return h;
    }

    private static Span clickable(String text, Runnable onClick) {
        Span s = new Span(text);
        s.getStyle().set("cursor", "pointer").set("font-weight", "700");
        s.addClickListener(e -> onClick.run());
        return s;
    }

    private static Paragraph error(String msg) {
        Paragraph p = new Paragraph(msg);
        p.getStyle().set("color", "#c62828").set("font-weight", "600");
        return p;
    }
}
