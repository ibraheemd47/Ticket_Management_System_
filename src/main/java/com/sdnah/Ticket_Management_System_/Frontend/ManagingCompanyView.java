package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CompositeDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DateRangeDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
<<<<<<< HEAD
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.ManagingCompanyPresenter;
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.ManagingCompanyPresenter.CompanyRow;
=======
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
>>>>>>> main
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Manager / owner dashboard for a single company. Three tabs:
 *  • Events   — list events, open event details, create new event.
 *  • Roles    — owners list + managers w/ permissions, appoint / remove.
 *  • Policies — discount + purchase rule editors (company-wide).
 *
 * <p>Convention: the previous view sets the target {@code companyId} on the
 * Vaadin session under {@code "managingCompanyId"} before navigating to
 * {@code "company"}.
 *
 * <p>All service interactions go through {@link ManagingCompanyPresenter};
 * this class only builds the UI and exposes display methods.
 */
@Route("company")
public class ManagingCompanyView extends VerticalLayout implements BeforeEnterObserver {

    private static final String SESSION_TOKEN      = "token";
    private static final String SESSION_COMPANY_ID = "managingCompanyId";

    private final ManagingCompanyPresenter presenter;

    
    private final Div tabContent = new Div();
    private final Tab eventsTab   = new Tab("Events");
    private final Tab rolesTab    = new Tab("Roles");
    private final Tab policiesTab = new Tab("Policies");

    /** Container the chooser is rendered into so we can replace its body on errors / refreshes. */
    private final Div chooserSlot = new Div();

    public ManagingCompanyView(ManagingCompanyPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);

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
        String token = t.toString();

        Object c = UI.getCurrent().getSession().getAttribute(SESSION_COMPANY_ID);
        if (c == null) {
            // No specific company picked — show the list of the user's companies.
            presenter.bind(token, null);
            add(chooserSlot);
            chooserSlot.add(buildChooserShell());
            presenter.loadMyCompanies();
            return;
        }
        presenter.bind(token, UUID.fromString(c.toString()));
        add(buildShell());
        presenter.loadEventsForCurrentCompany();
    }

    // ── Display methods called by the presenter ──────────────────────────────

    public void showMyCompanies(List<CompanyRow> companies) {
        if (companies.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "You don't own or manage any company yet. " +
                    "Click \"+ Create new company\" to start one.");
            empty.getStyle().set("color", "#666").set("padding", "24px 0");
            chooserSlot.add(empty);
            return;
        }
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
        grid.setItems(companies);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        chooserSlot.add(grid);
    }

    public void showCompanyChooserError(String message) {
        chooserSlot.add(error(message));
    }

    public void showEvents(List<UUID> eventIds) {
        tabContent.removeAll();

        Button addEvent = new Button("+ New event", e -> {
            UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, presenter.getCompanyId());
            UI.getCurrent().navigate("event-create");
        });
        addEvent.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Grid<UUID> grid = new Grid<>(UUID.class, false);
        grid.addColumn(id -> id.toString().substring(0, 8)).setHeader("Event ID");
        grid.addComponentColumn(id -> {
            Button open = new Button("Open", ev -> {
                UI.getCurrent().getSession().setAttribute("eventId", id.toString());
                UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, presenter.getCompanyId());
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

    public void showEventsError(String message) {
        tabContent.removeAll();
        tabContent.add(error(message));
    }

    public void showRoles(CompanyRolesViewDTO roles) {
        tabContent.removeAll();

        Div section = new Div();
        section.add(sectionTitle("Founder"));
        section.add(new Paragraph(roles.getFounderId()));

        section.add(sectionTitle("Owners"));
        section.add(buildOwnersList(roles.getOwnerIds()));
        section.add(appointBox("Appoint owner", presenter::appointOwner));

        section.add(sectionTitle("Managers + permissions"));
        section.add(buildManagersGrid(roles.getManagerPermissions()));
        section.add(appointBox("Appoint manager", presenter::appointManager));

        tabContent.add(section);
    }

    public void showRolesError(String message) {
        tabContent.removeAll();
        tabContent.add(error(message));
    }

    /** Called after appoint/remove — show a confirmation then refetch the roles. */
    public void onRoleMutationSucceeded(String message) {
        Notification.show(message, 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        presenter.loadRolesForCurrentCompany();
    }

    public void showSuccess(String message) {
        Notification.show(message, 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void showError(String message) {
        Notification.show(message, 3500, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // ── Chooser shell (list mode) ───────────────────────────────────────────

    private Component buildChooserShell() {
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
            grid.addColumn(r -> r.name == null ? "Unnamed company" : r.name)
        .setHeader("Company Name")
        .setFlexGrow(2);

grid.addColumn(r -> r.role)
        .setHeader("Your role")
        .setAutoWidth(true);
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

    // ── Shell (detail mode) ──────────────────────────────────────────────────

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

        H1 title = new H1(getCurrentCompanyName());
        title.getStyle().set("margin", "0 0 16px 0");
        card.add(back);

        Tabs tabs = new Tabs(eventsTab, rolesTab, policiesTab);
        tabs.addSelectedChangeListener(e -> {
            tabContent.removeAll();
            Tab selected = e.getSelectedTab();
            if (selected == eventsTab)         presenter.loadEventsForCurrentCompany();
            else if (selected == rolesTab)     presenter.loadRolesForCurrentCompany();
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
                UI.getCurrent().getSession().setAttribute("managingCompanyId", this.companyId);
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
        section.add(new Paragraph(getMemberDisplayName(roles.getFounderId())));
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
    list.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "8px");

    for (String ownerId : ownerIds) {
        Span chip = new Span(getMemberDisplayName(ownerId));

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
        grid.addColumn(entry -> getMemberDisplayName(entry.getKey()))
        .setHeader("Manager");
                grid.addColumn(e -> String.join(", ",
                e.getValue().stream().map(Enum::name).sorted().toList())).setHeader("Permissions");
        grid.addComponentColumn(entry -> {
            Button remove = new Button("Remove", ev -> {
    try {
        String username = getMemberDisplayName(entry.getKey());

        companyService.removeManagerAppointment(token, companyId, entry.getKey());

        Notification.show("Removed " + username, 2500,
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
    TextField usernameField = new TextField();
    usernameField.setPlaceholder("username");

    Button go = new Button(label, e -> {
        String username = usernameField.getValue();

        if (username == null || username.isBlank()) {
            Notification.show("Username required", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            String memberId = getMemberIdByUsername(username);

            action.accept(memberId);

            Notification.show("Done", 2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            renderRolesTab();

        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    });

    go.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout row = new HorizontalLayout(usernameField, go);
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

    private Component buildDiscountEditor() {
        Div card = policyCard("Company Discount Policy");

        Div rulesContainer = new Div();
        rulesContainer.setWidthFull();
        rulesContainer.getStyle().set("display","flex").set("flex-direction","column").set("gap","8px");

        List<DiscountRule> discountRules = new ArrayList<>();

        RadioButtonGroup<String> andOrGroup = new RadioButtonGroup<>();
        andOrGroup.setLabel("Combine rules with:");
        andOrGroup.setItems("OR (best discount)", "AND (sum discounts)");
        andOrGroup.setValue("OR (best discount)");
        andOrGroup.setWidthFull();

        java.util.List<Policy> existing = policyService.getPoliciesForCompany(companyId);
        existing.stream()
            .filter(p -> p instanceof DiscountPolicy)
            .map(p -> (DiscountPolicy) p)
            .findFirst()
            .ifPresent(dp -> {
                andOrGroup.setValue(dp.isAdditive() ? "AND (sum discounts)" : "OR (best discount)");
                if (dp.getRootRule() instanceof CompositeDiscountRule composite) {
                    discountRules.addAll(composite.getRules());
                } else if (dp.getRootRule() != null) {
                    discountRules.add(dp.getRootRule());
                }
            });

        Runnable[] rebuildRuleRows = {null};
        rebuildRuleRows[0] = () -> {
            rulesContainer.removeAll();
            for (int i = 0; i < discountRules.size(); i++) {
                final int idx = i;
                Span desc = new Span(discountRules.get(i).describe());
                Button delBtn = new Button("✕", ev -> {
                    discountRules.remove(idx);
                    rebuildRuleRows[0].run();
                });
                delBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                Div row = new Div(desc, delBtn);
                row.getStyle().set("display","flex").set("justify-content","space-between")
                .set("align-items","center").set("background","#f0f4ff")
                .set("padding","6px 10px").set("border-radius","6px");
                rulesContainer.add(row);
            }
        };
        rebuildRuleRows[0].run();

        Select<String> ruleTypeBox = new Select<>();
        ruleTypeBox.setLabel("Rule Type");
        ruleTypeBox.setItems("Percentage", "Coupon", "Quantity (min tickets)", "Date Range");
        ruleTypeBox.setWidthFull();

        NumberField rulePercentage = new NumberField("Percentage (0-100)");
        rulePercentage.setMin(0); rulePercentage.setMax(100); rulePercentage.setWidthFull();
        TextField ruleCouponCode = new TextField("Coupon Code");
        ruleCouponCode.setWidthFull();
        DatePicker ruleCouponExpiry = new DatePicker("Expiry (optional)");
        ruleCouponExpiry.setWidthFull();
        IntegerField ruleMinTickets = new IntegerField("Minimum Tickets");
        ruleMinTickets.setMin(1); ruleMinTickets.setWidthFull();
        DateTimePicker ruleFromPicker = new DateTimePicker("From (optional)");
        ruleFromPicker.setWidthFull();
        DateTimePicker ruleUntilPicker = new DateTimePicker("Until (optional)");
        ruleUntilPicker.setWidthFull();

        Div ruleFields = new Div();
        ruleFields.setWidthFull();
        ruleTypeBox.addValueChangeListener(ev -> {
            ruleFields.removeAll();
            switch (ev.getValue() != null ? ev.getValue() : "") {
                case "Percentage" -> ruleFields.add(rulePercentage);
                case "Coupon" -> ruleFields.add(ruleCouponCode, rulePercentage, ruleCouponExpiry);
                case "Quantity (min tickets)" -> ruleFields.add(ruleMinTickets, rulePercentage);
                case "Date Range" -> ruleFields.add(rulePercentage, ruleFromPicker, ruleUntilPicker);
            }
        });

        Button addRuleBtn = new Button("+ Add Rule", ev -> {
            String rt = ruleTypeBox.getValue();
            Double pct = rulePercentage.getValue();
            if (rt == null) { Notification.show("Select rule type"); return; }
            if (pct == null || pct <= 0) { Notification.show("Percentage must be > 0"); return; }
            if ("Percentage".equals(rt)) {
                discountRules.add(new PercentageDiscountRule(pct, pct + "% off"));
            } else if ("Coupon".equals(rt)) {
                String c = ruleCouponCode.getValue();
                if (c == null || c.isBlank()) { Notification.show("Coupon code required"); return; }
                java.time.LocalDateTime expiry = ruleCouponExpiry.getValue() != null
                        ? ruleCouponExpiry.getValue().atTime(23, 59, 59) : null;
                discountRules.add(new CouponDiscountRule(pct, c.trim().toUpperCase(), expiry));
            } else if ("Quantity (min tickets)".equals(rt)) {
                Integer minTix = ruleMinTickets.getValue();
                if (minTix == null || minTix <= 0) { Notification.show("Min tickets must be > 0"); return; }
                discountRules.add(new QuantityConditionalDiscountRule(minTix, pct));
                ruleMinTickets.clear();
            } else if ("Date Range".equals(rt)) {
                java.time.LocalDateTime from = ruleFromPicker.getValue();
                java.time.LocalDateTime until = ruleUntilPicker.getValue();
                discountRules.add(new DateRangeDiscountRule(pct, from, until));
                ruleFromPicker.clear(); ruleUntilPicker.clear();
            }
            rebuildRuleRows[0].run();
            rulePercentage.clear(); ruleCouponCode.clear(); ruleCouponExpiry.clear();
        });
        addRuleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button saveBtn = new Button("Save Discount Policy", ev -> {
            try {
                if (discountRules.isEmpty()) { Notification.show("Add at least one rule"); return; }
                boolean isAdditive = "AND (sum discounts)".equals(andOrGroup.getValue());
                policyService.setDiscountRulesForCompany(token, companyId, discountRules, isAdditive);
                Notification.show("Discount policy saved", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show("Error: " + ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        //card.add(andOrGroup, rulesContainer, ruleTypeBox, ruleFields, addRuleBtn, saveBtn);
        Div addRow = new Div(addRuleBtn);
        addRow.getStyle().set("margin-top", "8px");

        Div saveRow = new Div(saveBtn);
        saveRow.getStyle().set("margin-top", "16px").set("border-top", "1px solid #e3eaf5").set("padding-top", "16px");

        card.add(andOrGroup, rulesContainer, ruleTypeBox, ruleFields, addRow, saveRow);
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

    private Component buildPurchaseEditor() {
        Div card = policyCard("Company Purchase Policy");

        RadioButtonGroup<String> operatorGroup = new RadioButtonGroup<>();
        operatorGroup.setLabel("Combine rules with:");
        operatorGroup.setItems("AND (all must pass)", "OR (at least one must pass)");
        operatorGroup.setValue("AND (all must pass)");
        operatorGroup.setWidthFull();

        IntegerField minAgeField = new IntegerField("Minimum Age");
        minAgeField.setMin(0); minAgeField.setWidthFull();
        minAgeField.setPlaceholder("Leave empty = no restriction");

        IntegerField minTicketsField = new IntegerField("Minimum Tickets per Purchase");
        minTicketsField.setMin(1); minTicketsField.setWidthFull();
        minTicketsField.setPlaceholder("Leave empty = no minimum");

        IntegerField maxTicketsField = new IntegerField("Maximum Tickets per Purchase");
        maxTicketsField.setMin(1); maxTicketsField.setWidthFull();
        maxTicketsField.setPlaceholder("Leave empty = no limit");

        policyService.getPoliciesForCompany(companyId).stream()
            .filter(p -> p instanceof PurchasePolicy)
            .map(p -> (PurchasePolicy) p)
            .findFirst()
            .ifPresent(pp -> {
                operatorGroup.setValue(pp.getOperator() == PurchasePolicy.Operator.OR
                        ? "OR (at least one must pass)" : "AND (all must pass)");
                extractMinAge(pp).ifPresent(minAgeField::setValue);
                extractMinTickets(pp).ifPresent(minTicketsField::setValue);
                extractMaxTickets(pp).ifPresent(maxTicketsField::setValue);
            });

        Button saveBtn = new Button("Save Purchase Policy", ev -> {
            try {
                Integer minAge = minAgeField.getValue();
                Integer minTix = minTicketsField.getValue();
                Integer maxTix = maxTicketsField.getValue();
                if (minAge == null && minTix == null && maxTix == null) {
                    Notification.show("At least one restriction is required"); return;
                }
                PurchasePolicy.Operator op = "OR (at least one must pass)".equals(operatorGroup.getValue())
                        ? PurchasePolicy.Operator.OR : PurchasePolicy.Operator.AND;
                java.util.List<PurchaseRule> rules = new ArrayList<>();
                if (minAge != null && minAge >= 0) rules.add(new MinAgeRule(minAge));
                if (minTix != null && minTix > 0)  rules.add(new MinTicketsRule(minTix));
                if (maxTix != null && maxTix > 0)  rules.add(new MaxTicketsRule(maxTix));
                policyService.setPurchaseRulesForCompany(token, companyId, rules, op);
                Notification.show("Purchase policy saved", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (RuntimeException ex) {
                Notification.show("Error: " + ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        card.add(operatorGroup, minAgeField, minTicketsField, maxTicketsField, saveBtn);
        return card;
    }

    private static Optional<Integer> extractMinAge(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MinAgeRule.class).map(MinAgeRule::getMinimumAge);
    }

    private static Optional<Integer> extractMinTickets(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MinTicketsRule.class).map(MinTicketsRule::getMinTickets);
    }

    private static Optional<Integer> extractMaxTickets(PurchasePolicy pp) {
        return extractPurchaseRule(pp.getRootRule(), MaxTicketsRule.class).map(MaxTicketsRule::getMaxTickets);
    }

    @SuppressWarnings("unchecked")
    private static <T extends com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule> Optional<T> extractPurchaseRule(
            com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule root, Class<T> type) {
        if (root == null) return Optional.empty();
        if (type.isInstance(root)) return Optional.of((T) root);
        if (root instanceof com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.CompositePurchaseRule c) {
            for (var r : c.getRules()) {
                Optional<T> found = extractPurchaseRule(r, type);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
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
    //helper 
    private String getCurrentCompanyName() {
    try {
        for (CompanyDTO dto : companyService.getActiveCompanies()) {
            if (dto.getCompanyId().equals(companyId)) {
                return dto.getCompanyName();
            }
        }
    } catch (RuntimeException ignored) {
    }

    return "Company";
}
    private String getMemberDisplayName(String memberId) {
    try {
        Member member = userService.getMemberById(memberId);

        if (member == null) {
            return memberId;
        }

        // Use the real method names from your Member class.
        // Example options:
        if (member.getUsername() != null && !member.getUsername().isBlank()) {
            return member.getUsername();
        }

        return memberId;

    } catch (RuntimeException ex) {
        return memberId;
    }
}
    private String getMemberIdByUsername(String username) {
    if (username == null || username.isBlank()) {
        throw new RuntimeException("Username is required");
    }

    Member member = userService.getMemberByUsername(username.trim());

    if (member == null) {
        throw new RuntimeException("User not found: " + username);
    }

    return member.getMemberId();
}
}
