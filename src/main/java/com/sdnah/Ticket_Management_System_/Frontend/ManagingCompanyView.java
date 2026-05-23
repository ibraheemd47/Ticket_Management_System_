package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
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
    private final UserService userService;
    private final PolicyService policyService;

    private String token;
    private UUID companyId;

    private final Div tabContent = new Div();
    private final Tab eventsTab   = new Tab("Events");
    private final Tab rolesTab    = new Tab("Roles");
    private final Tab policiesTab = new Tab("Policies");

    public ManagingCompanyView(company_managment_serivce companyService,
                               UserService userService,
                               PolicyService policyService) {
        this.companyService = companyService;
        this.userService = userService;
        this.policyService = policyService;

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
        Object t = UI.getCurrent().getSession().getAttribute(SESSION_TOKEN);
        if (t == null) {
            event.forwardTo(LoginView.class);
            return;
        }
        this.token = t.toString();

        Object c = UI.getCurrent().getSession().getAttribute(SESSION_COMPANY_ID);
        if (c == null) {
            // No specific company picked — show the list of the user's companies.
            add(buildCompanyChooser());
            return;
        }
        this.companyId = UUID.fromString(c.toString());
        add(buildShell());
        renderEventsTab();
    }

    // ── Chooser (list mode) ─────────────────────────────────────────────────

    private Component buildCompanyChooser() {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "1080px")
                .set("margin", "40px auto")
                .set("padding", "24px 32px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)");

        H1 title = new H1("My companies");
        title.getStyle().set("margin", "0 0 4px 0");

        Paragraph blurb = new Paragraph(
                "Pick a company below to manage its events, roles and policies. " +
                "Use \"Create new company\" if you don't have any yet.");
        blurb.getStyle().set("color", "#666").set("margin-top", "0");

        Button create = new Button("+ Create new company", e -> {
            // Make sure no stale id leaks into the create flow.
            UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, null);
            UI.getCurrent().navigate("company-create");
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.getStyle().set("margin", "12px 0");

        card.add(title, blurb, create);

        // Resolve the user's companies via their role assignments.
        List<CompanyRow> myCompanies;
        try {
            Member me = userService.getMemberByToken(token);
            myCompanies = resolveMyCompanies(me);
        } catch (RuntimeException ex) {
            card.add(error("Couldn't load your companies: " + ex.getMessage()));
            Div outer = new Div(card);
            outer.setWidthFull();
            return outer;
        }

        if (myCompanies.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "You don't own or manage any company yet. Click \"+ Create new company\" to start one.");
            empty.getStyle().set("color", "#666").set("padding", "24px 0");
            card.add(empty);
        } else {
            Grid<CompanyRow> grid = new Grid<>(CompanyRow.class, false);
            grid.addColumn(r -> "#" + r.companyId).setHeader("ID").setAutoWidth(true);
            grid.addColumn(r -> r.name == null ? "—" : r.name).setHeader("Name").setFlexGrow(2);
            grid.addColumn(r -> r.role).setHeader("Your role").setAutoWidth(true);
            grid.addComponentColumn(r -> {
                Button manage = new Button("Manage", ev -> {
                    UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, r.companyId);
                    UI.getCurrent().getPage().reload();
                });
                manage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                return manage;
            }).setHeader("");
            grid.setItems(myCompanies);
            grid.setAllRowsVisible(true);
            grid.setWidthFull();
            card.add(grid);
        }

        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    /** Cross-reference the member's roles with the active-companies list to get names. */
    private List<CompanyRow> resolveMyCompanies(Member me) {
        Set<UUID> myCompanyIds = new java.util.HashSet<>();
        Map<UUID, String> roleByCompany = new java.util.HashMap<>();
        for (CompanyRoleAssignment a : me.getCompanyRoles()) {
            myCompanyIds.add(a.getCompanyId());
            // First role wins if there are duplicates.
            roleByCompany.putIfAbsent(a.getCompanyId(),
                    a.isOwner() ? "Owner" : a.isManager() ? "Manager" : a.getRoleType().name());
        }

        // Best available source for company names today; switch to a dedicated
        // "find by ids" query if/when one is added.
        Map<UUID, String> nameById = new java.util.HashMap<>();
        try {
            for (CompanyDTO dto : companyService.getActiveCompanies()) {
                nameById.put(dto.getCompanyId(), dto.getCompanyName());
            }
        } catch (RuntimeException ignored) {
            // If the lookup blows up we still show IDs.
        }

        List<CompanyRow> out = new java.util.ArrayList<>();
        for (UUID cid : myCompanyIds) {
            out.add(new CompanyRow(cid, nameById.get(cid), roleByCompany.get(cid)));
        }
        out.sort((a, b) -> a.companyId.compareTo(b.companyId));
        return out;
    }

    private static final class CompanyRow {
        final UUID companyId;
        final String name;
        final String role;
        CompanyRow(UUID companyId, String name, String role) {
            this.companyId = companyId;
            this.name = name;
            this.role = role;
        }
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

        Button back = new Button("← Back to my companies", e -> {
            UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, null);
            UI.getCurrent().getPage().reload();
        });
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("margin-bottom", "8px");

        H1 title = new H1("Company #" + companyId);
        title.getStyle().set("margin", "0 0 16px 0");
        card.add(back);

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

        Div wrap = new Div();
        wrap.getStyle().set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "24px");

        wrap.add(buildDiscountEditor(), buildPurchaseEditor());

        Paragraph note = new Paragraph(
                "Rules are added to the company-wide policy. " +
                "Event-specific overrides live on the Event details page.");
        note.getStyle().set("color", "#666").set("margin-top", "16px");

        tabContent.add(wrap, note);
    }

    // ── Policies → Discount editor ──────────────────────────────────────────

    private Component buildDiscountEditor() {
        Div card = policyCard("Add a discount rule");

        Select<String> type = new Select<>();
        type.setLabel("Rule type");
        type.setItems("Percentage", "Conditional (min qty)", "Coupon code");
        type.setValue("Percentage");

        NumberField percent = new NumberField("Percentage (0–100)");
        percent.setValue(10.0);
        percent.setMin(0); percent.setMax(100); percent.setStep(1);

        IntegerField minQty = new IntegerField("Min tickets");
        minQty.setValue(2);
        minQty.setMin(1);
        minQty.setVisible(false);

        TextField code = new TextField("Coupon code");
        code.setVisible(false);

        TextField description = new TextField("Description (optional)");

        type.addValueChangeListener(e -> {
            minQty.setVisible("Conditional (min qty)".equals(e.getValue()));
            code.setVisible("Coupon code".equals(e.getValue()));
        });

        Button add = new Button("Add discount rule", ev -> {
            try {
                DiscountRule rule = buildDiscountRule(
                        type.getValue(), percent.getValue(), minQty.getValue(),
                        code.getValue(), description.getValue());
                policyService.addDiscountRuleToCompany(token, companyId, rule);
                Notification.show("Discount rule added", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show("Couldn't add: " + ex.getMessage(),
                                3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        card.add(type, percent, minQty, code, description, add);
        return card;
    }

    private DiscountRule buildDiscountRule(String type, Double percent, Integer minQty,
                                           String code, String description) {
        double p = percent == null ? 0.0 : percent;
        String desc = (description == null || description.isBlank())
                ? defaultDiscountDescription(type, p) : description;
        return switch (type) {
            case "Conditional (min qty)" -> new QuantityConditionalDiscountRule(
                    minQty == null ? 1 : minQty, p);
            case "Coupon code"           -> {
                if (code == null || code.isBlank())
                    throw new IllegalArgumentException("Coupon code required");
                yield new CouponDiscountRule(p, code.trim());
            }
            default                      -> new PercentageDiscountRule(p, desc);
        };
    }

    private String defaultDiscountDescription(String type, double percent) {
        return "%.0f%% %s".formatted(percent,
                type == null ? "discount" : type.toLowerCase() + " discount");
    }

    // ── Policies → Purchase editor ──────────────────────────────────────────

    private Component buildPurchaseEditor() {
        Div card = policyCard("Add a purchase rule");

        Select<String> type = new Select<>();
        type.setLabel("Rule type");
        type.setItems("Minimum age", "Max tickets per order", "Min tickets per order");
        type.setValue("Minimum age");

        IntegerField value = new IntegerField("Value");
        value.setValue(18);
        value.setMin(1);

        type.addValueChangeListener(e -> {
            // sane default for each rule type
            switch (e.getValue()) {
                case "Minimum age"             -> value.setValue(18);
                case "Max tickets per order"   -> value.setValue(5);
                case "Min tickets per order"   -> value.setValue(2);
                default -> {}
            }
        });

        Button add = new Button("Add purchase rule", ev -> {
            try {
                PurchaseRule rule = buildPurchaseRule(type.getValue(), value.getValue());
                policyService.addPurchaseRuleToCompany(token, companyId, rule);
                Notification.show("Purchase rule added", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show("Couldn't add: " + ex.getMessage(),
                                3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        card.add(type, value, add);
        return card;
    }

    private PurchaseRule buildPurchaseRule(String type, Integer value) {
        int v = value == null ? 0 : value;
        if (v <= 0) throw new IllegalArgumentException("Value must be positive");
        return switch (type) {
            case "Max tickets per order" -> new MaxTicketsRule(v);
            case "Min tickets per order" -> new MinTicketsRule(v);
            default                      -> new MinAgeRule(v);
        };
    }

    private Div policyCard(String title) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "20px")
                .set("background", "#f9fbff")
                .set("border", "1px solid #e3eaf5")
                .set("border-radius", "12px");
        H2 t = new H2(title);
        t.getStyle().set("margin", "0 0 12px 0").set("font-size", "18px");
        card.add(t);
        return card;
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
