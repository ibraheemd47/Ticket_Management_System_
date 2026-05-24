package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Waiting_QueueService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import java.util.UUID;

@Route("admin")
public class SystemAdminView extends VerticalLayout implements BeforeEnterObserver {
    private String selectedTab = "users";
    private final SystemAdminService systemAdminService;
    private final company_managment_serivce companyManagmentService;
    private final NotificationService notificationService;
    private final ActiveOrderService activeOrderService;
    private String token;

    public SystemAdminView(SystemAdminService systemAdminService, company_managment_serivce companyManagmentService,
            NotificationService notificationService, ActiveOrderService activeOrderService) {
        this.systemAdminService = systemAdminService;
        this.companyManagmentService = companyManagmentService;
        this.notificationService = notificationService;
        this.activeOrderService = activeOrderService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        token = (String) VaadinSession.getCurrent().getAttribute("token");
        if (token == null || token.isBlank()) {
            event.rerouteTo("login");
            return;
        }
        var params = event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("tab") && !params.get("tab").isEmpty())
            selectedTab = params.get("tab").get(0);

        removeAll();
        add(buildHeader());
        add(buildContent());
    }

    private Div buildHeader() {
        Div header = new Div();
        header.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "26px 52px 0 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        // top row
        Div topRow = new Div();
        topRow.getStyle().set("display", "flex").set("align-items", "center").set("justify-content", "space-between");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle().set("margin", "0").set("font-size", "22px").set("font-weight", "900").set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Span back = new Span("← Back to Home");
        back.getStyle().set("cursor", "pointer").set("font-size", "14px").set("font-weight", "700");
        back.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        topRow.add(logo, back);

        // breadcrumb
        Paragraph crumb = new Paragraph("Home / Admin Dashboard");
        crumb.getStyle().set("margin", "28px 0 0 0").set("font-size", "14px");

        // title
        H1 title = new H1("Admin Dashboard");
        title.getStyle().set("font-size", "34px").set("margin", "10px 0 24px 0");

        // tabs
        Div tabs = new Div();
        tabs.getStyle()
                .set("display", "flex")
                .set("gap", "34px")
                .set("border-bottom", "1px solid rgba(255,255,255,0.3)");

        tabs.add(
                tab("All Users", "users"),
                tab("Suspended", "suspended"),
                tab("Companies", "companies"),
                tab("Complaints", "complaints"),
                tab("Messages", "messages"),
                tab("Analytics", "analytics"),
                tab("Queues", "queues"),
                tab("Purchase History", "purchases"));

        header.add(topRow, crumb, title, tabs);
        return header;
    }

    private Span tab(String text, String value) {
        Span t = new Span(text);
        t.getStyle()
                .set("padding", "14px 2px")
                .set("font-weight", "700")
                .set("cursor", "pointer");
        if (selectedTab.equals(value))
            t.getStyle().set("border-bottom", "4px solid white");
        t.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("admin?tab=" + value)));
        return t;
    }

    private Div buildContent() {
        Div content = new Div();
        content.getStyle()
                .set("padding", "40px 52px 48px 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        switch (selectedTab) {
            case "suspended" -> content.add(buildSuspended());
            case "companies" -> content.add(buildCompanies());
            case "complaints" -> content.add(buildComplaints());
            case "messages" -> content.add(buildMessages());
            case "analytics" -> content.add(buildAnalytics());
            case "queues" -> content.add(buildQueues());
            case "purchases" -> content.add(buildPurchaseHistory());
            default -> content.add(buildUsers());
        }

        return content;
    }

    // TAB: USERS — II.6.2 Remove Member + II.6.7 Suspend
    private Div buildUsers() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");

        Div usersListCard = actionCard("Registered Users",
                "All registered members on the platform.");
        usersListCard.getStyle().set("min-width", "600px");        
        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        Button loadBtn = actionButton("Load Users", "#026cdf");
        loadBtn.addClickListener(e -> {
            tableArea.removeAll();
            try {
                var users = systemAdminService.getAllUsers(token);
                if (users.isEmpty()) {
                    tableArea.add(new Paragraph("No users found."));
                    return;
                }

                Div headerRow = tableRow("#026cdf", "white");
                headerRow.add(
                        tableCell("Member ID", true),
                        tableCell("Username", true),
                        tableCell("Email", true),
                        tableCell("Status", true));
                tableArea.add(headerRow);

                for (int i = 0; i < users.size(); i++) {
                    var m = users.get(i);
                    Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                    String status = m.isSuspended() ? " Suspended"
                            : m.isActive() ? " Active"
                                    : " Inactive";
                    row.add(
                            tableCell(m.getMemberId(), false),
                            tableCell(m.getUsername(), false),
                            tableCell(m.getEmail() != null ? m.getEmail() : "—", false),
                            tableCell(status, false));
                    tableArea.add(row);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        usersListCard.add(loadBtn, tableArea);
        wrapper.add(usersListCard);

        // Suspend user card — II.6.7
        Div suspendCard = actionCard("Suspend User",
                "Enter the Username of the member you wish to suspend. Suspended members can browse the platform but cannot perform any actions.");

        TextField suspendUsername = styledField("Username to suspend");

        RadioButtonGroup<String> type = new RadioButtonGroup<>();
        type.setLabel("Suspension Type");
        type.setItems("Temporary", "Permanent");
        type.setValue("Temporary");
        type.getStyle().set("margin-top", "12px");

        NumberField hours = new NumberField("Duration (hours)");
        hours.setValue(24.0);
        hours.setMin(1);
        hours.setWidthFull();
        hours.getStyle().set("margin-top", "8px");
        type.addValueChangeListener(e -> hours.setVisible(e.getValue().equals("Temporary")));

        Button suspendBtn = actionButton("Suspend User", "#026cdf");

        suspendBtn.addClickListener(e -> {
            if (suspendUsername.isEmpty()) {
                showError("Please enter a Username.");
                return;
            }
            try {
                if (type.getValue().equals("Permanent")) {
                    systemAdminService.suspendPermanently(token, suspendUsername.getValue());
                    showSuccess("User '" + suspendUsername.getValue() + "' suspended permanently.");
                } else {
                    long h = hours.getValue() == null ? 24 : hours.getValue().longValue();
                    systemAdminService.suspendUser(token, suspendUsername.getValue(), h);
                    showSuccess("User '" + suspendUsername.getValue() + "' suspended for " + h + " hours.");
                }
                suspendUsername.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
            suspendUsername.clear();
        });

        suspendCard.add(suspendUsername, type, hours, suspendBtn);
        wrapper.add(suspendCard);

        // Remove member card — II.6.2
        Div removeCard = actionCard("Remove Member",
                "Enter the Username of the member you wish to remove. This action is irreversible and will revoke all their roles and permissions.");

        TextField removeUsername = styledField("Username to remove");
        Button removeBtn = actionButton("Remove Member", "#026cdf");

        removeBtn.addClickListener(e -> {
            if (removeUsername.isEmpty()) {
                showError("Please enter a Username.");
                return;
            }
            try {
                systemAdminService.removeMember(token, removeUsername.getValue());
                showSuccess("Member '" + removeUsername.getValue() + "' has been removed.");
                removeUsername.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        removeCard.add(removeUsername, removeBtn);
        wrapper.add(removeCard);
        return wrapper;
    }

    // TAB: SUSPENDED — II.6.8 Unsuspend + II.6.9 View
    private Div buildSuspended() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        // Unsuspend card — II.6.8
        Div unsuspendCard = actionCard("Unsuspend User",
                "Enter the Username of the suspended member you wish to reinstate. They will regain full access to the platform immediately.");

        TextField unsuspendUsername = styledField("Username to unsuspend");
        Button unsuspendBtn = actionButton("Unsuspend User", "#026cdf");
        unsuspendBtn.addClickListener(e -> {
            if (unsuspendUsername.isEmpty()) {
                showError("Please enter a Username.");
                return;
            }
            try {
                systemAdminService.unsuspendUser(token, unsuspendUsername.getValue());
                showSuccess("User '" + unsuspendUsername.getValue() + "' unsuspended.");
                unsuspendUsername.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        unsuspendCard.add(unsuspendUsername, unsuspendBtn);
        wrapper.add(unsuspendCard);

        // View suspensions card — II.6.9
        Div viewCard = actionCard("View Suspensions",
                "Load a list of all currently suspended members, including suspension start date and expiry.");

        Button viewBtn = actionButton("Load Suspended Users", "#026cdf");
        Div resultArea = new Div();
        resultArea.getStyle().set("margin-top", "16px");

        viewBtn.addClickListener(e -> {
            resultArea.removeAll();
            try {
                var suspensions = systemAdminService.getSuspensions(token);
                if (suspensions.isEmpty()) {
                    resultArea.add(new Paragraph("No suspended users found."));
                    return;
                }
                for (var s : suspensions) {
                    Div row = new Div();
                    row.getStyle().set("background", "#f3f4f6").set("border-radius", "8px")
                            .set("padding", "12px 16px")
                            .set("font-size", "14px");

                    String until = s.isSuspendedPermanently() ? "Permanent"
                            : (s.getSuspendedUntil() != null ? s.getSuspendedUntil().toString() : "—");
                    row.add(new Paragraph(
                            "👤 " + s.getUsername() +
                                    " (ID: " + s.getMemberId() + ")" +
                                    " — until: " + until +
                                    (s.getSuspensionStartedAt() != null
                                            ? " | since: " + s.getSuspensionStartedAt().toLocalDate()
                                            : "")));
                    resultArea.add(row);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        viewCard.add(viewBtn, resultArea);
        wrapper.add(viewCard);
        return wrapper;
    }

    // TAB: COMPANIES — II.6.1 Close Company
    private Div buildCompanies() {

        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("gap", "24px")
                .set("align-items", "flex-start")
                .set("width", "100%")
                .set("flex-wrap", "wrap");
        // LEFT SIDE — companies list
        Div companiesListCard = actionCard("Active Production Companies",
                "All active production companies on the platform.");
        companiesListCard.getStyle().set("min-width", "600px");        
        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        Button loadBtn = actionButton("Load Companies", "#026cdf");
        loadBtn.addClickListener(e -> {
            tableArea.removeAll();
            try {
                var companies = companyManagmentService.getActiveCompanies();
                if (companies.isEmpty()) {
                    tableArea.add(new Paragraph("No active companies found."));
                    return;
                }
                Div headerRow = tableRow("#026cdf", "white");
                headerRow.add(
                        tableCell("Company ID", true),
                        tableCell("Name", true),
                        tableCell("Rating", true),
                        tableCell("Status", true));
                tableArea.add(headerRow);

                for (int i = 0; i < companies.size(); i++) {
                    var c = companies.get(i);
                    Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                    row.add(
                            tableCell(String.valueOf(c.getCompanyId()), false),
                            tableCell(c.getCompanyName(), false),
                            tableCell(String.valueOf(c.getRating()), false),
                            tableCell("Active", false));
                    tableArea.add(row);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        companiesListCard.add(loadBtn, tableArea);
        wrapper.add(companiesListCard);

        // RIGHT SIDE — close company form
        Div card = actionCard(
                "Close Production Company",
                "Enter the ID of the production company you wish to close. "
                        + "All owners and managers will be notified and their roles will be revoked.");

        TextField companyId = styledField("Company ID to close");

        TextArea reason = new TextArea("Reason for closure");
        reason.setWidthFull();
        reason.setPlaceholder("e.g. Terms of service violation...");
        reason.getStyle().set("margin-top", "12px");

        Button closeBtn = actionButton("Close Company", "#026cdf");

        closeBtn.addClickListener(e -> {
            if (companyId.isEmpty()) {
                showError("Please enter a Company ID.");
                return;
            }
            try {
                companyManagmentService.adminCloseCompany(token, java.util.UUID.fromString(companyId.getValue()));
                showSuccess("Company '" + companyId.getValue() + "' has been closed.");
                companyId.clear();
                reason.clear();
            } catch (IllegalArgumentException ex) {
                showError("Company ID must be a valid UUID.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        card.add(companyId, reason, closeBtn);
        wrapper.add(card);
        return wrapper;
    }

    // TAB: COMPLAINTS — II.6.3
    private Div buildComplaints() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        wrapper.add(buildViewComplaintsCard());
        return wrapper;
    }

    private Div buildViewComplaintsCard() {
        Div card = actionCard("View Complaints",
                "Browse and respond to submitted complaints from members.");

        Button loadBtn = actionButton("Load Complaints", "#026cdf");
        Div resultArea = new Div();
        resultArea.getStyle().set("margin-top", "16px").set("display", "flex")
                .set("flex-direction", "column").set("gap", "12px");

        loadBtn.addClickListener(e -> {
            resultArea.removeAll();
            try {
                var complaints = systemAdminService.getAllSystemComplaintst(token);
                if (complaints.isEmpty()) {
                    resultArea.add(new Paragraph("No complaints found."));
                    return;
                }

                for (var c : complaints) {
                    Div complaintCard = new Div();
                    complaintCard.getStyle()
                            .set("background", "white").set("border-radius", "8px")
                            .set("padding", "16px").set("border", "1px solid #e5e7eb")
                            .set("border-left", "4px solid #026cdf");

                    // שורה עליונה — subject + status + תאריך
                    Div topRow = new Div();
                    topRow.getStyle().set("display", "flex").set("justify-content",
                            "space-between")
                            .set("align-items", "center").set("margin-bottom", "6px");

                    Span subject = new Span("📋 " + c.getSubject());
                    subject.getStyle().set("font-weight", "700").set("font-size", "14px");

                    Span statusSpan = new Span(c.getStatus().toString());
                    statusSpan.getStyle()
                            .set("font-size", "12px").set("font-weight", "600").set("padding", "2px 8px")
                            .set("border-radius", "999px")
                            .set("background", "RESOLVED".equals(c.getStatus().toString()) ? "#dcfce7" : "#fef9c3")
                            .set("color", "RESOLVED".equals(c.getStatus().toString()) ? "#16a34a" : "#854d0e");

                    topRow.add(subject, statusSpan);

                    // reporter + תאריך
                    Paragraph meta = new Paragraph(
                            "👤 " + c.getReporterMemberId() +
                                    (c.getCreatedAt() != null ? " • " + c.getCreatedAt().toLocalDate() : ""));
                    meta.getStyle().set("margin", "0 0 8px 0").set("font-size",
                            "13px").set("color", "#6b7280");

                    // description
                    Paragraph desc = new Paragraph("📝 " + c.getDescription());
                    desc.getStyle().set("margin", "0 0 12px 0").set("font-size",
                            "13px").set("color", "#374151");

                    // response + resolve
                    TextArea response = new TextArea("Admin Response");
                    response.setWidthFull();
                    response.setPlaceholder("Type your response...");
                    if (c.getAdminResponse() != null && !c.getAdminResponse().isBlank()) {
                        response.setValue(c.getAdminResponse());
                        response.setReadOnly(true);
                    }

                    Button resolveBtn = actionButton("Resolve", "#16a34a");
                    resolveBtn.setEnabled(c.getAdminResponse() == null ||
                            c.getAdminResponse().isBlank());
                    resolveBtn.addClickListener(re -> {
                        if (response.isEmpty()) {
                            showError("Please enter a response.");
                            return;
                        }
                        try {
                            systemAdminService.resolveComplaint(token, c.getComplaintId(), response.getValue());
                            showSuccess("Complaint resolved.");
                            resolveBtn.setEnabled(false);
                            response.setReadOnly(true);
                            statusSpan.setText("RESOLVED");
                            statusSpan.getStyle().set("background", "#dcfce7").set("color", "#16a34a");
                        } catch (Exception ex) {
                            showError(ex.getMessage());
                        }
                    });

                    complaintCard.add(topRow, meta, desc, response, resolveBtn);
                    resultArea.add(complaintCard);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(loadBtn, resultArea);
        return card;
    }

    // TAB: MESSAGES — II.6.3 Send System Message
    private Div buildMessages() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        wrapper.add(buildSendSystemMessageCard());
        return wrapper;
    }

    private Div buildSendSystemMessageCard() {
        Div card = actionCard("Send System Message",
                "Send an official system message to a member. Use this for important platform notifications.");

        TextField recipient = styledField("Recipient Username");
        TextArea message = new TextArea("Message");
        message.setWidthFull();
        message.setPlaceholder("Type your message here...");
        message.getStyle().set("margin-top", "12px");

        Button sendBtn = actionButton("Send Message", "#026cdf");
        sendBtn.addClickListener(e -> {
            if (recipient.isEmpty() || message.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }
            try {
                String memberId = systemAdminService.getMemberIdByUsername(token, recipient.getValue());
                notificationService.createNotification(
                        memberId,
                        message.getValue(),
                        NotificationType.SYSTEM_ANNOUNCEMENT);
                showSuccess("Message sent to '" + recipient.getValue() + "'.");
                recipient.clear();
                message.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(recipient, message, sendBtn);
        return card;
    }

    // TAB: ANALYTICS — II.6.5
    private Div buildAnalytics() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr 1fr")
                .set("gap", "24px");
        int totalUsers = systemAdminService.getAllUsers(token).size();
        long loggedInNow = systemAdminService.getLoggedInUsersCount(token);
        int suspendedUsers = systemAdminService.getSuspensions(token).size();
        int activeOrders = activeOrderService.getActiveOrdersCount();
        int purchasesToday = activeOrderService.getPurchasesTodayCount();
        int reservationRate = activeOrderService.getReservationRate();
        try {
            wrapper.add(
                    analyticsCard("Total Users", "👥", "All registered members", String.valueOf(totalUsers)),
                    analyticsCard("Active Sessions", "🟢", "Live visitors right now", String.valueOf(loggedInNow)),
                    analyticsCard("Suspended Users", "🔴", "Currently suspended members",
                            String.valueOf(suspendedUsers)),
                    analyticsCard("Purchases Today", "💳", "Completed transactions", String.valueOf(purchasesToday)),
                    analyticsCard("Active Orders", "🎟️", "Orders pending payment", String.valueOf(activeOrders)),
                    analyticsCard("Reservation Rate", "⏱️", "Reservations per minute", reservationRate + "/min"));
        } catch (Exception ex) {
            showError("Failed to load analytics: " + ex.getMessage());
        }
        return wrapper;
    }

    private Div analyticsCard(String title, String icon, String desc, String count) {
        Div c = new Div();
        c.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("padding", "24px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.07)")
                .set("border-top", "4px solid #026cdf");

        H3 val = new H3(icon);
        val.getStyle().set("margin", "0 0 8px 0").set("font-size", "30px")
                .set("font-weight", "900").set("color", "#026cdf");

        Paragraph t = new Paragraph(title);
        t.getStyle().set("margin", "0 0 4px 0").set("font-weight", "700")
                .set("font-size", "14px").set("color", "#111");

        Paragraph countP = new Paragraph(count);
        countP.getStyle().set("margin", "0 0 4px 0").set("font-size", "22px")
                .set("font-weight", "900").set("color", "#026cdf");

        Paragraph d = new Paragraph(desc);
        d.getStyle().set("margin", "0").set("font-size", "13px").set("color", "#9ca3af");

        c.add(val, t, countP, d);
        return c;
    }

    // TAB: QUEUES — II.6.6
    private Div buildQueues() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        wrapper.add(buildQueuesListCard());
        wrapper.add(buildQueueControlCard());
        return wrapper;
    }

    private Div buildQueuesListCard() {
        Div card = actionCard("View Active Queues",
                "Monitor all active virtual queues across the platform. Use this during high-demand ticket releases.");

        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        Button loadBtn = actionButton("Load Queues", "#026cdf");
        loadBtn.addClickListener(e -> {
            tableArea.removeAll();
            try {
                var queues = systemAdminService.getAllQueues(token);
                if (queues.isEmpty()) {
                    tableArea.add(new Paragraph("No active queues found."));
                    return;
                }

                Div headerRow = tableRow("#026cdf", "white");
                headerRow.add(tableCell("Show ID", true), tableCell("Waiting", true),
                        tableCell("Flow Rate/min", true));
                tableArea.add(headerRow);

                for (int i = 0; i < queues.size(); i++) {
                    var q = queues.get(i);
                    Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                    row.add(tableCell(String.valueOf(q.getShowId()), false),
                            tableCell(String.valueOf(q.getTotalWaiting()), false),
                            tableCell(String.valueOf(q.getCheckoutCapacityPerMinute()), false));
                    tableArea.add(row);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(loadBtn, tableArea);
        return card;
    }

    private Div buildQueueControlCard() {
        Div card = actionCard("Control Queue",
                "Adjust the user flow rate or clear a queue entirely if a technical issue occurs.");

        TextField queueId = styledField("Show ID");

        NumberField flowAmount = new NumberField("Flow change amount");
        flowAmount.setMin(1);
        flowAmount.setWidthFull();
        flowAmount.getStyle().set("margin-top", "8px");

        Button increaseBtn = actionButton("Increase Flow", "#026cdf");
        Button decreaseBtn = actionButton("Decrease Flow", "#026cdf");
        Button clearBtn = actionButton("Clear Queue", "#026cdf");

        increaseBtn.setEnabled(false);
        decreaseBtn.setEnabled(false);
        clearBtn.setEnabled(false);

        Runnable updateButtons = () -> {
            boolean hasQueueId = !queueId.isEmpty();
            boolean hasAmount = flowAmount.getValue() != null && flowAmount.getValue() >= 1;
            increaseBtn.setEnabled(hasQueueId && hasAmount);
            decreaseBtn.setEnabled(hasQueueId && hasAmount);
            clearBtn.setEnabled(hasQueueId);
        };

        queueId.addValueChangeListener(e -> updateButtons.run());
        flowAmount.addValueChangeListener(e -> updateButtons.run());

        increaseBtn.addClickListener(e -> {
            try {
                int amount = flowAmount.getValue().intValue();
                systemAdminService.increaseQueueFlow(token,
                        Long.parseLong(queueId.getValue()), amount);
                showSuccess("Flow rate increased by " + amount + " for queue '" +
                        queueId.getValue() + "'.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        decreaseBtn.addClickListener(e -> {
            try {
                int amount = flowAmount.getValue().intValue();
                systemAdminService.decreaseQueueFlow(token,
                        Long.parseLong(queueId.getValue()), amount);
                showSuccess("Flow rate decreased by " + amount + " for queue '" +
                        queueId.getValue() + "'.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        clearBtn.addClickListener(e -> {
            try {
                systemAdminService.clearQueue(token, Long.parseLong(queueId.getValue()));
                showSuccess("Queue '" + queueId.getValue() + "' cleared.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(queueId, flowAmount, increaseBtn, decreaseBtn, clearBtn);
        return card;
    }

    // TAB: PURCHASE HISTORY — II.6.4

    private Div buildPurchaseHistory() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        wrapper.add(buildPurchasesByBuyerCard());
        wrapper.add(buildPurchasesByEventCard());
        return wrapper;
    }

    private Div buildPurchasesByBuyerCard() {
        Div card = actionCard("Purchases by Buyer",
                "View full purchase history of a specific member by their Member ID.");

        TextField buyerId = styledField("Username or Member ID");
        Button searchBtn = actionButton("Load Purchases", "#026cdf");

        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        searchBtn.addClickListener(e -> {
            if (buyerId.isEmpty()) {
                showError("Please enter a Member ID.");
                return;
            }
            tableArea.removeAll();
            try {
                var purchases = systemAdminService.getPurchasesByBuyer(token, buyerId.getValue());
                if (purchases.isEmpty()) {
                    tableArea.add(new Paragraph("No purchases found."));
                    return;
                }

                Div headerRow = tableRow("#026cdf", "white");
                headerRow.add(tableCell("Purchase ID", true), tableCell("Order ID", true),
                        tableCell("Price", true), tableCell("Date", true));
                tableArea.add(headerRow);

                for (int i = 0; i < purchases.size(); i++) {
                    var p = purchases.get(i);
                    Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                    row.add(
                            tableCell(p.getPurchaseId().toString(), false),
                            tableCell(p.getOrderId().toString(), false),
                            tableCell(p.getFinalPrice() != null ? p.getFinalPrice().toString() : "—",
                                    false),
                            tableCell(p.getPurchasedAt() != null
                                    ? p.getPurchasedAt().toLocalDate().toString()
                                    : "—", false));
                    tableArea.add(row);
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(buyerId, searchBtn, tableArea);
        return card;
    }

    private Div buildPurchasesByEventCard() {
        Div card = actionCard("Purchases by Event",
                "View all purchases made for a specific event by its Event ID.");

        TextField eventId = styledField("Event ID ");
        Button searchBtn = actionButton("Load Purchases", "#026cdf");

        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        searchBtn.addClickListener(e -> {
            if (eventId.isEmpty()) {
                showError("Please enter an Event ID.");
                return;
            }
            tableArea.removeAll();
            try {
                var purchases = systemAdminService.getPurchasesByEvent(token, UUID.fromString(eventId.getValue()));
                if (purchases.isEmpty()) {
                    tableArea.add(new Paragraph("No purchases found."));
                    return;
                }

                Div headerRow = tableRow("#026cdf", "white");
                headerRow.add(tableCell("Purchase ID", true), tableCell("Order ID", true),
                        tableCell("Price", true), tableCell("Date", true));
                tableArea.add(headerRow);

                for (int i = 0; i < purchases.size(); i++) {
                    var p = purchases.get(i);
                    Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                    row.add(
                            tableCell(p.getPurchaseId().toString(), false),
                            tableCell(p.getOrderId().toString(), false),
                            tableCell(p.getFinalPrice() != null ? p.getFinalPrice().toString() : "—", false),
                            tableCell(p.getPurchasedAt() != null
                                    ? p.getPurchasedAt().toLocalDate().toString()
                                    : "—", false));
                    tableArea.add(row);
                }
            } catch (IllegalArgumentException ex) {
                showError("Invalid UUID format.");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        card.add(eventId, searchBtn, tableArea);
        return card;
    }

    // HELPERS
    private Div actionCard(String title, String description) {
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("padding", "28px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.07)")
                .set("border-top", "4px solid #026cdf")
                .set("flex", "1")
                .set("min-width", "320px")
                .set("box-sizing", "border-box");

        H3 t = new H3(title);
        t.getStyle().set("margin", "0 0 8px 0").set("font-size", "18px").set("color", "#111").set("font-weight", "700");

        Paragraph desc = new Paragraph(description);
        desc.getStyle().set("color", "#6b7280").set("font-size", "14px").set("line-height", "1.5").set("margin",
                "0 0 20px 0");

        card.add(t, desc);
        return card;
    }

    private TextField styledField(String placeholder) {
        TextField field = new TextField(placeholder);
        field.setWidthFull();
        field.getStyle().set("margin-bottom", "12px");
        return field;
    }

    private Div tableRow(String bg, String color) {
        Div row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "2fr 2fr 2fr 1fr")
                .set("background", bg).set("color", color)
                .set("border-radius", "6px").set("padding", "10px 12px")
                .set("font-size", "13px").set("margin-bottom", "2px");
        return row;
    }

    private Span tableCell(String text, boolean bold) {
        Span s = new Span(text);
        if (bold)
            s.getStyle().set("font-weight", "700");
        s.getStyle().set("overflow", "hidden").set("text-overflow", "ellipsis");
        return s;
    }

    private Button actionButton(String text, String color) {
        Button b = new Button(text);
        b.setWidthFull();
        b.getStyle()
                .set("height", "44px")
                .set("background", color)
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "16px");
        return b;
    }

    private void showSuccess(String msg) {
        Notification n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.setPosition(Notification.Position.TOP_CENTER);
    }

    private void showError(String msg) {
        Notification n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setPosition(Notification.Position.TOP_CENTER);
    }

}
