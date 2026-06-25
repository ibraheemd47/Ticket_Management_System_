package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.PurchaseHistoryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.SalesReportDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
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
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.ManagingCompanyPresenter;
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.ManagingCompanyPresenter.CompanyRow;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.textfield.TextArea;
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
    private final Tab eventsTab     = new Tab("Events");
    private final Tab rolesTab      = new Tab("Roles");
    private final Tab policiesTab   = new Tab("Policies");
    private final Tab reportTab     = new Tab("Sales report");
    private final Tab historyTab    = new Tab("Purchase history");
    private final Tab complaintsTab = new Tab("Complaints");

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
        if (t == null || t.toString().startsWith("GUEST_")) {
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
        if (!presenter.userHasAccessToCurrentCompany()) {
            // Stale session id left behind by a previous user — drop it and
            // fall back to the chooser instead of showing a company the
            // current user has no role in.
            UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, null);
            UI.getCurrent().getPage().reload();
            return;
        }
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

    public void showEvents(List<EventDto> events) {
        tabContent.removeAll();

        Button addEvent = new Button("+ New event", e -> {
            UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, presenter.getCompanyId());
            UI.getCurrent().navigate("event-create");
        });
        addEvent.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Grid<EventDto> grid = new Grid<>(EventDto.class, false);
        grid.addColumn(dto -> dto.name != null ? dto.name : dto.id.toString().substring(0, 8))
                .setHeader("Event Name").setFlexGrow(1);
        grid.addComponentColumn(dto -> {
            Button open = new Button("Open", ev -> {
                UI.getCurrent().getSession().setAttribute("eventId", dto.id.toString());
                UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, presenter.getCompanyId());
                UI.getCurrent().navigate("EventDetails");
            });
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return open;
        }).setHeader("").setWidth("100px").setFlexGrow(0);
        grid.setItems(events);
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

        String me = presenter.getCurrentMemberId();
        boolean iAmFounder = me != null && me.equals(roles.getFounderId());
        boolean iAmOwner = iAmFounder
                || (me != null && roles.getOwnerIds() != null && roles.getOwnerIds().contains(me));

        Div section = new Div();
        section.add(sectionTitle("Founder"));
        section.add(new Paragraph(presenter.getMemberDisplayName(roles.getFounderId())));

        section.add(sectionTitle("Owners"));
        section.add(buildOwnersList(roles.getOwnerIds(), me,
                roles.getOwnerAppointedBy(), roles.getFounderId()));
        section.add(appointBox("Appoint owner", presenter::appointOwner));

        // II.4.10 — a non-founder owner may resign their own ownership.
        if (iAmOwner && !iAmFounder) {
            Button resign = new Button("Resign ownership", e -> confirmResign());
            resign.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            resign.getStyle().set("margin-top", "12px");
            section.add(resign);
        }

        section.add(sectionTitle("Managers + permissions"));
        section.add(buildManagersGrid(roles.getManagerPermissions(), roles.getManagerAppointedBy()));
        section.add(buildAppointManagerBox());

        // II.4.13 / II.4.14 — owners can suspend / reopen the company.
        if (iAmOwner) {
            section.add(sectionTitle("Company status"));
            boolean open = presenter.isCurrentCompanyOpen();
            Paragraph status = new Paragraph(
                    "This company is currently " + (open ? "open." : "suspended."));
            status.getStyle().set("color", "#666").set("font-size", "13px").set("margin", "0 0 8px 0");
            Button toggle;
            if (open) {
                toggle = new Button("Suspend company", e -> confirmSuspend());
                toggle.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            } else {
                toggle = new Button("Reopen company", e -> presenter.reopenCompany());
                toggle.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            }
            section.add(status, toggle);
        }

        // Item 12 — owners can delete the whole company (and its events).
        if (iAmOwner) {
            section.add(sectionTitle("Danger zone"));
            Paragraph warn = new Paragraph(
                    "Permanently deletes this company and all of its events. This cannot be undone.");
            warn.getStyle().set("color", "#666").set("font-size", "13px").set("margin", "0 0 8px 0");
            Button del = new Button("Delete company", e -> confirmDelete());
            del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            section.add(warn, del);
        }

        tabContent.add(section);
    }

    public void showRolesError(String message) {
        tabContent.removeAll();
        tabContent.add(error(message));
    }

    public void showSalesReport(SalesReportDTO report) {
        tabContent.removeAll();
        if (report == null) {
            tabContent.add(new Paragraph("No data."));
            return;
        }
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        section.add(sectionTitle("Sales report — " + report.getCompanyName()));
        section.add(new Paragraph("Total orders: " + report.getTotalOrders()));
        section.add(new Paragraph("Total revenue: " + report.getTotalRevenue().toPlainString()));

        if (report.getPerEvent().isEmpty()) {
            section.add(new Paragraph("No events for this company yet."));
        } else {
            Grid<SalesReportDTO.EventLine> grid = new Grid<>();
            grid.addColumn(SalesReportDTO.EventLine::getEventName).setHeader("Event");
            grid.addColumn(SalesReportDTO.EventLine::getOrders).setHeader("Orders");
            grid.addColumn(line -> line.getRevenue().toPlainString()).setHeader("Revenue");
            grid.setItems(report.getPerEvent());
            grid.setAllRowsVisible(true);
            grid.setWidthFull();
            section.add(grid);
        }
        tabContent.add(section);
    }

    public void showPurchaseHistory(List<PurchaseHistoryEntryDTO> rows) {
        tabContent.removeAll();
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(sectionTitle("Purchase history"));

        if (rows == null || rows.isEmpty()) {
            section.add(new Paragraph("No purchases yet for this company's events."));
            tabContent.add(section);
            return;
        }

        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        Grid<PurchaseHistoryEntryDTO> grid = new Grid<>();
        grid.addColumn(r -> r.getPurchasedAt() == null ? "—" : r.getPurchasedAt().format(fmt))
                .setHeader("Purchased at");
        grid.addColumn(PurchaseHistoryEntryDTO::getEventName).setHeader("Event");
        grid.addColumn(r -> getMemberDisplayName(r.getBuyerId())).setHeader("Buyer");
        grid.addColumn(PurchaseHistoryEntryDTO::getTicketCount).setHeader("Tickets");
        grid.addColumn(r -> r.getTotalPrice().toPlainString()).setHeader("Total");
        grid.setItems(rows);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        section.add(grid);
        tabContent.add(section);
    }

    // ── Complaints tab (company-side, parallel to admin) ─────────────────────

    public void showComplaints(List<ComplaintDTO> complaints) {
        tabContent.removeAll();
        Div section = new Div();
        section.add(sectionTitle("Complaints about this company"));
        Paragraph note = new Paragraph(
                "Complaints buyers filed against your company. You can respond here in parallel "
                + "with the system admin.");
        note.getStyle().set("color", "#666");
        section.add(note);

        if (complaints == null || complaints.isEmpty()) {
            section.add(new Paragraph("No complaints for this company."));
            tabContent.add(section);
            return;
        }

        Grid<ComplaintDTO> grid = new Grid<>(ComplaintDTO.class, false);
        grid.addColumn(ComplaintDTO::getSubject).setHeader("Subject").setFlexGrow(2);
        grid.addColumn(c -> c.getStatus() == null ? "—" : c.getStatus().name())
                .setHeader("Status").setAutoWidth(true);
        grid.addColumn(c -> getMemberDisplayName(c.getReporterMemberId()))
                .setHeader("From").setAutoWidth(true);
        grid.addColumn(c -> c.getCreatedAt() == null ? "—" : c.getCreatedAt().toLocalDate().toString())
                .setHeader("Filed").setAutoWidth(true);
        grid.addComponentColumn(c -> {
            Button respond = new Button("View / Respond", ev -> openRespondDialog(c));
            respond.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return respond;
        }).setHeader("");
        grid.setItems(complaints);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        section.add(grid);
        tabContent.add(section);
    }

    public void showComplaintsError(String message) {
        tabContent.removeAll();
        tabContent.add(error(message));
    }

    private void openRespondDialog(ComplaintDTO c) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Complaint — " + c.getSubject());
        dialog.setWidth("520px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.getStyle().set("gap", "8px");
        body.add(new Paragraph("From: " + getMemberDisplayName(c.getReporterMemberId())));

        Paragraph desc = new Paragraph(c.getDescription());
        desc.getStyle().set("background", "#f6f8fb").set("padding", "10px")
                .set("border-radius", "8px").set("white-space", "pre-wrap");
        body.add(desc);

        if (c.getAdminResponse() != null && !c.getAdminResponse().isBlank()) {
            Paragraph ar = new Paragraph("Admin response: " + c.getAdminResponse());
            ar.getStyle().set("color", "#555").set("font-size", "13px");
            body.add(ar);
        }
        if (c.getCompanyResponse() != null && !c.getCompanyResponse().isBlank()) {
            Paragraph cr = new Paragraph("Your last response: " + c.getCompanyResponse());
            cr.getStyle().set("color", "#2e7d32").set("font-size", "13px");
            body.add(cr);
        }

        TextArea response = new TextArea("Your response");
        response.setWidthFull();
        response.setMinHeight("110px");
        if (c.getCompanyResponse() != null) response.setValue(c.getCompanyResponse());
        body.add(response);

        Button send = new Button("Send response", e -> {
            if (response.getValue() == null || response.getValue().isBlank()) {
                showError("Write a response first");
                return;
            }
            presenter.respondToComplaint(c.getComplaintId(), response.getValue(), false);
            dialog.close();
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button resolve = new Button("Send & resolve", e -> {
            if (response.getValue() == null || response.getValue().isBlank()) {
                showError("Write a response first");
                return;
            }
            presenter.respondToComplaint(c.getComplaintId(), response.getValue(), true);
            dialog.close();
        });
        resolve.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(body);
        dialog.getFooter().add(cancel, send, resolve);
        dialog.open();
    }

    /** Called after appoint/remove — show a confirmation then refetch the roles. */
    public void onRoleMutationSucceeded(String message) {
        Notification.show(message, 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        presenter.loadRolesForCurrentCompany();
    }

    /**
     * Called after the current user resigns (II.4.10) or deletes the company
     * (item 12) — either way they no longer have a role here, so drop the
     * session company id and bounce back to the chooser.
     */
    public void onLeftCompany(String message) {
        Notification.show(message, 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().getSession().setAttribute(SESSION_COMPANY_ID, null);
        UI.getCurrent().getPage().reload();
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

        // Companies are populated into chooserSlot via presenter.loadMyCompanies() → showMyCompanies()
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

        Tabs tabs = new Tabs(eventsTab, rolesTab, policiesTab, reportTab, historyTab, complaintsTab);
        tabs.addSelectedChangeListener(e -> {
            tabContent.removeAll();
            Tab selected = e.getSelectedTab();
            if (selected == eventsTab)          presenter.loadEventsForCurrentCompany();
            else if (selected == rolesTab)      presenter.loadRolesForCurrentCompany();
            else if (selected == policiesTab)   renderPoliciesTab();
            else if (selected == reportTab)     presenter.loadSalesReport();
            else if (selected == historyTab)    presenter.loadPurchaseHistory();
            else if (selected == complaintsTab) presenter.loadComplaints();
        });

        tabContent.getStyle().set("padding-top", "16px");

        card.add(title, tabs, tabContent);

        Div outer = new Div(card);
        outer.setWidthFull();
        return outer;
    }

    // ── Tab: Events ──────────────────────────────────────────────────────────

    private void renderEventsTab() {
        presenter.loadEventsForCurrentCompany();
    }

    // ── Tab: Roles ───────────────────────────────────────────────────────────

    private void renderRolesTab() {
        presenter.loadRolesForCurrentCompany();
    }

    private Component buildOwnersList(List<String> ownerIds,
                                      String currentMemberId,
                                      Map<String, String> ownerAppointedBy,
                                      String founderId) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return new Paragraph("No additional owners.");
        }
        final Map<String, String> appointedBy = ownerAppointedBy == null
                ? java.util.Collections.emptyMap() : ownerAppointedBy;

        Div list = new Div();
        list.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px");

        for (String ownerId : ownerIds) {
            Span chip = new Span(getMemberDisplayName(ownerId));
            chip.getStyle()
                    .set("padding", "6px 12px")
                    .set("background", "#e3f2fd")
                    .set("border-radius", "999px")
                    .set("font-size", "13px");

            HorizontalLayout row = new HorizontalLayout(chip);
            row.setDefaultVerticalComponentAlignment(
                    com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

            // "Appointed by …" provenance — founder doesn't have one.
            String byline;
            if (ownerId.equals(founderId)) {
                byline = "founder";
            } else {
                String appointer = appointedBy.get(ownerId);
                byline = (appointer == null || appointer.isBlank())
                        ? "appointed by —"
                        : "appointed by " + getMemberDisplayName(appointer);
            }
            Span byTag = new Span(byline);
            byTag.getStyle()
                    .set("color", "#555")
                    .set("font-size", "12px")
                    .set("margin-left", "4px");
            row.add(byTag);

            // II.4.9 — owners can remove owners they appointed (backend enforces
            // the "appointed by you" rule). Removing yourself goes through
            // "Resign ownership" instead, so don't offer Remove on your own row.
            if (!ownerId.equals(currentMemberId)) {
                Button remove = new Button("Remove", ev -> presenter.removeOwner(ownerId));
                remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY,
                        ButtonVariant.LUMO_SMALL);
                row.add(remove);
            }
            list.add(row);
        }

        return list;
    }

    private Component buildManagersGrid(Map<String, Set<CompanyPermission>> managerPermissions,
                                        Map<String, String> managerAppointedBy) {
        if (managerPermissions == null || managerPermissions.isEmpty()) {
            return new Paragraph("No managers yet.");
        }
        final Map<String, String> appointedBy = managerAppointedBy == null
                ? java.util.Collections.emptyMap() : managerAppointedBy;
        Grid<Map.Entry<String, Set<CompanyPermission>>> grid = new Grid<>();
        grid.addColumn(entry -> getMemberDisplayName(entry.getKey()))
        .setHeader("Manager");
                grid.addColumn(e -> String.join(", ",
                e.getValue().stream().map(Enum::name).sorted().toList())).setHeader("Permissions");
        grid.addColumn(e -> {
            String owner = appointedBy.get(e.getKey());
            return (owner == null || owner.isBlank()) ? "—" : getMemberDisplayName(owner);
        }).setHeader("Appointed by");
        grid.addComponentColumn(entry -> {
            // II.4.11 — edit this manager's permission set.
            Button edit = new Button("Edit permissions",
                    ev -> openEditPermissionsDialog(entry.getKey(), entry.getValue()));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return edit;
        }).setHeader("");
        grid.addComponentColumn(entry -> {
            Button remove = new Button("Remove", ev -> {
                // Presenter owns the full flow — it catches errors, shows
                // a success/error notification, and reloads the roles tab.
                // Adding our own try/catch + reload here would double-fire
                // the notification and re-render twice.
                presenter.removeManager(entry.getKey());
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

            // Resolve username → memberId in its own try/catch (this is a
            // local lookup, not the presenter's responsibility).
            String memberId;
            try {
                memberId = getMemberIdByUsername(username);
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Presenter owns the appoint flow — it catches its own errors,
            // shows success / error notifications, and reloads the roles
            // tab on success. We must NOT show our own "Done" notification
            // or call renderRolesTab() here, otherwise we'd double-notify
            // (or, on backend error, see both an error AND a "Done").
            action.accept(memberId);
        });

        go.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(usernameField, go);
        row.getStyle().set("margin-top", "8px");

        return row;
    }

    /**
     * Appoint-manager control (II.4.7) — like {@link #appointBox} but also lets
     * the owner pick the permission set to grant up-front.
     */
    private Component buildAppointManagerBox() {
        TextField usernameField = new TextField();
        usernameField.setPlaceholder("username");

        CheckboxGroup<CompanyPermission> perms = new CheckboxGroup<>();
        perms.setLabel("Permissions to grant");
        perms.setItems(CompanyPermission.values());
        perms.setItemLabelGenerator(ManagingCompanyView::permissionLabel);

        Button go = new Button("Appoint manager", e -> {
            String username = usernameField.getValue();
            if (username == null || username.isBlank()) {
                Notification.show("Username required", 2500, Notification.Position.MIDDLE);
                return;
            }
            String memberId;
            try {
                memberId = getMemberIdByUsername(username);
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            // Presenter owns the appoint flow (success / error notification +
            // roles reload), so we don't notify here.
            presenter.appointManager(memberId, new HashSet<>(perms.getValue()));
        });
        go.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout box = new VerticalLayout(usernameField, perms, go);
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("margin-top", "8px").set("gap", "8px");
        return box;
    }

    /** Dialog to replace a manager's permission set (II.4.11). */
    private void openEditPermissionsDialog(String managerId, Set<CompanyPermission> current) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Permissions — " + getMemberDisplayName(managerId));

        CheckboxGroup<CompanyPermission> perms = new CheckboxGroup<>();
        perms.setItems(CompanyPermission.values());
        perms.setItemLabelGenerator(ManagingCompanyView::permissionLabel);
        perms.setValue(current == null ? Set.of() : new HashSet<>(current));

        Button save = new Button("Save", e -> {
            // Presenter shows the success / error notification and reloads roles.
            presenter.modifyManagerPermissions(managerId, new HashSet<>(perms.getValue()));
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(perms);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** Confirmation for resigning ownership (II.4.10). */
    private void confirmResign() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Resign ownership?");
        dialog.setText("You will lose your owner role in this company.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Resign");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> presenter.resignOwnership());
        dialog.open();
    }

    /** Confirmation for suspending the company (II.4.13). */
    private void confirmSuspend() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Suspend this company?");
        dialog.setText("While suspended, its events won't be visible to buyers. You can reopen it later.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Suspend");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> presenter.closeCompany());
        dialog.open();
    }

    /** Confirmation for deleting the whole company (item 12). */
    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete this company?");
        dialog.setText("This permanently removes the company and all of its events. This cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> presenter.deleteCompany());
        dialog.open();
    }

    /** Human-friendly label for a {@link CompanyPermission}. */
    private static String permissionLabel(CompanyPermission p) {
        return switch (p) {
            case MANAGE_EVENTS        -> "Manage events & inventory";
            case VIEW_HISTORY         -> "View purchase / order history";
            case RESPOND_TO_INQUIRIES -> "Respond to inquiries";
            case VIEW_ROLES           -> "View roles & permissions";
        };
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

        java.util.List<Policy> existing = presenter.getPoliciesForCompany();
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
            // Local pre-validation only — keep its notification because the
            // presenter is never called in that case.
            if (discountRules.isEmpty()) {
                Notification.show("Add at least one rule");
                return;
            }
            boolean isAdditive = "AND (sum discounts)".equals(andOrGroup.getValue());
            // Presenter owns the save flow — it catches its own errors and
            // shows the success / error notification. Adding our own would
            // double-notify (the original bug behaviour).
            presenter.setDiscountRulesForCompany(discountRules, isAdditive);
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

        presenter.getPoliciesForCompany().stream()
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
            Integer minAge = minAgeField.getValue();
            Integer minTix = minTicketsField.getValue();
            Integer maxTix = maxTicketsField.getValue();
            // Local pre-validation only — presenter is never called below
            // when nothing was filled in.
            if (minAge == null && minTix == null && maxTix == null) {
                Notification.show("At least one restriction is required");
                return;
            }
            PurchasePolicy.Operator op = "OR (at least one must pass)".equals(operatorGroup.getValue())
                    ? PurchasePolicy.Operator.OR : PurchasePolicy.Operator.AND;
            java.util.List<PurchaseRule> rules = new ArrayList<>();
            if (minAge != null && minAge >= 0) rules.add(new MinAgeRule(minAge));
            if (minTix != null && minTix > 0)  rules.add(new MinTicketsRule(minTix));
            if (maxTix != null && maxTix > 0)  rules.add(new MaxTicketsRule(maxTix));
            // Presenter owns the save flow — it catches its own errors and
            // shows the success / error notification. Adding our own would
            // double-notify (the original bug behaviour).
            presenter.setPurchaseRulesForCompany(rules, op);
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
    private String getCurrentCompanyName() {
        return presenter.getCurrentCompanyName();
    }
    private String getMemberDisplayName(String memberId) {
        return presenter.getMemberDisplayName(memberId);
    }

    private String getMemberIdByUsername(String username) {
        return presenter.getMemberIdByUsername(username);
    }
}
