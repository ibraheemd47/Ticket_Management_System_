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
        // if (token == null || token.isBlank()) {
        // event.rerouteTo("login");
        // return;
        // }
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
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));

        Span back = new Span("← Back to Home");
        back.getStyle().set("cursor", "pointer").set("font-size", "14px").set("font-weight", "700");
        back.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));

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
                tab("Analytics", "analytics"),
                tab("Queues", "queues"));

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
            case "analytics" -> content.add(buildAnalytics());
            case "queues" -> content.add(buildQueues());
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
        // systemAdminService.getAllUsers()
        Div tableArea = new Div();
        tableArea.getStyle().set("margin-top", "16px").set("width", "100%");

        Button loadBtn = actionButton("Load Users", "#026cdf");
        loadBtn.addClickListener(e -> {
            tableArea.removeAll();
            try {
                // var users = systemAdminService.getAllUsers(token);
                // if (users.isEmpty()) {
                // tableArea.add(new Paragraph("No users found."));
                // return;
                // }

                // Div headerRow = tableRow("#026cdf", "white");
                // headerRow.add(
                // tableCell("Member ID", true),
                // tableCell("Username", true),
                // tableCell("Email", true),
                // tableCell("Status", true));
                // tableArea.add(headerRow);

                // for (int i = 0; i < users.size(); i++) {
                // var m = users.get(i);
                // Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
                // String status = m.isSuspended() ? " Suspended"
                // : m.isActive() ? " Active"
                // : " Inactive";
                // row.add(
                // tableCell(m.getMemberId(), false),
                // tableCell(m.getUsername(), false),
                // tableCell(m.getEmail() != null ? m.getEmail() : "—", false),
                // tableCell(status, false));
                // tableArea.add(row);
                // }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        usersListCard.add(loadBtn, tableArea);
        wrapper.add(usersListCard);

        // Suspend user card — II.6.7
        Div suspendCard = actionCard("Suspend User",
                "Enter the Member ID of the member you wish to suspend. Suspended members can browse the platform but cannot perform any actions.");

        TextField suspendMemberID = styledField("Member ID to suspend");

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
            if (suspendMemberID.isEmpty()) {
                showError("Please enter a Member ID.");
                return;
            }
            try {
                if (type.getValue().equals("Permanent")) {
                    systemAdminService.suspendPermanently(token, suspendMemberID.getValue());
                    showSuccess("User '" + suspendMemberID.getValue() + "' suspended permanently.");
                } else {
                    long h = hours.getValue() == null ? 24 : hours.getValue().longValue();
                    systemAdminService.suspendUser(token, suspendMemberID.getValue(), h);
                    showSuccess("User '" + suspendMemberID.getValue() + "' suspended for " + h + " hours.");
                }
                suspendMemberID.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
            suspendMemberID.clear();
        });

        suspendCard.add(suspendMemberID, type, hours, suspendBtn);
        wrapper.add(suspendCard);

        // Remove member card — II.6.2
        Div removeCard = actionCard("Remove Member",
                "Enter the Member ID of the member you wish to remove. This action is irreversible and will revoke all their roles and permissions.");

        TextField removeMemberID = styledField("Member ID to remove");
        Button removeBtn = actionButton("Remove Member", "#026cdf");

        removeBtn.addClickListener(e -> {
            if (removeMemberID.isEmpty()) {
                showError("Please enter a MemberID.");
                return;
            }
            try {
                // systemAdminService.removeMember(token, removeMemberID.getValue());
                showSuccess("Member '" + removeMemberID.getValue() + "' has been removed.");
                removeMemberID.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        removeCard.add(removeMemberID, removeBtn);
        wrapper.add(removeCard);
        return wrapper;
    }

    // TAB: SUSPENDED — II.6.8 Unsuspend + II.6.9 View
    private Div buildSuspended() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        // Unsuspend card — II.6.8
        Div unsuspendCard = actionCard("Unsuspend User",
                "Enter the Member ID of the suspended member you wish to reinstate. They will regain full access to the platform immediately.");

        TextField unsuspendId = styledField("Member ID to unsuspend");
        Button unsuspendBtn = actionButton("Unsuspend User", "#026cdf");
        unsuspendBtn.addClickListener(e -> {
            if (unsuspendId.isEmpty()) {
                showError("Please enter a MemberID.");
                return;
            }
            try {
                systemAdminService.unsuspendUser(token, unsuspendId.getValue());
                showSuccess("User '" + unsuspendId.getValue() + "' unsuspended.");
                unsuspendId.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        unsuspendCard.add(unsuspendId, unsuspendBtn);
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
                .set("width", "100%");
        // LEFT SIDE — companies list
        Div companiesListCard = actionCard("Active Production Companies",
                "All active production companies on the platform.");
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
                companyManagmentService.adminCloseCompany(token, Integer.parseInt(companyId.getValue()));
                showSuccess("Company '" + companyId.getValue() + "' has been closed.");
                companyId.clear();
                reason.clear();
            } catch (NumberFormatException ex) {
                showError("Company ID must be a number.");
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
        // View complaints
        Div viewCard = actionCard("View Complaints",
                "Browse submitted complaints from members regarding fake tickets, event issues or platform abuse.");

        Div placeholder2 = new Div();
        placeholder2.getStyle()
                .set("background", "#f9fafb")
                .set("border", "1px dashed #d1d5db")
                .set("border-radius", "8px")
                .set("padding", "40px 24px")
                .set("text-align", "center")
                .set("margin-bottom", "16px");
        Paragraph pt2 = new Paragraph("Complaint list will appear here after connecting the backend.");
        pt2.getStyle().set("color", "#9ca3af").set("font-size", "14px").set("margin", "0");
        placeholder2.add(pt2);
        viewCard.add(placeholder2);
        wrapper.add(viewCard);
        // Send system message
        Div msgCard = actionCard("Send System Message",
                "Send an official system message to a member or production company. Use this for important platform notifications.");

        TextField recipient = styledField("Member ID or Company ID");
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
                notificationService.createNotification(recipient.getValue(), message.getValue(),
                        NotificationType.SYSTEM_ANNOUNCEMENT);
                showSuccess("Message sent to '" + recipient.getValue() + "'.");
                recipient.clear();
                message.clear();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        msgCard.add(recipient, message, sendBtn);
        wrapper.add(msgCard);
        return wrapper;
    }

    // TAB: ANALYTICS — II.6.5
    private Div buildAnalytics() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr 1fr")
                .set("gap", "24px");
        // int totalUsers = systemAdminService.getAllUsers(token).size();
        // long loggedInNow = systemAdminService.getLoggedInUsersCount(token);
        // int suspendedUsers = systemAdminService.getSuspensions(token).size();
        int activeOrders = activeOrderService.getActiveOrdersCount();
        int purchasesToday = activeOrderService.getPurchasesTodayCount();
        int reservationRate = activeOrderService.getReservationRate();
        try {
            wrapper.add(
                    // analyticsCard("Total Users", "👥", "All registered members",
                    // String.valueOf(totalUsers)),
                    // analyticsCard("Active Sessions", "🟢", "Live visitors right now",
                    // String.valueOf(loggedInNow)),
                    // analyticsCard("Suspended Users", "🔴", "Currently suspended
                    // members",String.valueOf(suspendedUsers)),
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
        // loadBtn.addClickListener(e -> {
        //     tableArea.removeAll();
        //     try {
        //         var queues = systemAdminService.getAllQueues(token);
        //         if (queues.isEmpty()) { tableArea.add(new Paragraph("No active queues found.")); return; }
 
        //         Div headerRow = tableRow("#026cdf", "white");
        //         headerRow.add(tableCell("Show ID", true), tableCell("Waiting", true),
        //                 tableCell("Flow Rate/min", true));
        //         tableArea.add(headerRow);
 
        //         for (int i = 0; i < queues.size(); i++) {
        //             var q = queues.get(i);
        //             Div row = tableRow(i % 2 == 0 ? "#f9fafb" : "white", "#111");
        //             row.add(tableCell(String.valueOf(q.getShowId()), false),
        //                     tableCell(String.valueOf(q.getTotalWaiting()), false),
        //                     tableCell(String.valueOf(q.getCheckoutCapacityPerMinute()), false));
        //             tableArea.add(row);
        //         }
        //     } catch (Exception ex) { showError(ex.getMessage()); }
        // });
 
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
        Button clearBtn    = actionButton("Clear Queue", "#026cdf");
 
        increaseBtn.setEnabled(false);
        decreaseBtn.setEnabled(false);
        clearBtn.setEnabled(false);
 
        Runnable updateButtons = () -> {
            boolean hasQueueId = !queueId.isEmpty();
            boolean hasAmount  = flowAmount.getValue() != null && flowAmount.getValue() >= 1;
            increaseBtn.setEnabled(hasQueueId && hasAmount);
            decreaseBtn.setEnabled(hasQueueId && hasAmount);
            clearBtn.setEnabled(hasQueueId);
        };
 
        queueId.addValueChangeListener(e -> updateButtons.run());
        flowAmount.addValueChangeListener(e -> updateButtons.run());
 
        // increaseBtn.addClickListener(e -> {
        //     try {
        //         int amount = flowAmount.getValue().intValue();
        //         systemAdminService.increaseQueueFlow(token, Long.parseLong(queueId.getValue()), amount);
        //         showSuccess("Flow rate increased by " + amount + " for queue '" + queueId.getValue() + "'.");
        //     } catch (Exception ex) { showError(ex.getMessage()); }
        // });
 
        // decreaseBtn.addClickListener(e -> {
        //     try {
        //         int amount = flowAmount.getValue().intValue();
        //         systemAdminService.decreaseQueueFlow(token, Long.parseLong(queueId.getValue()), amount);
        //         showSuccess("Flow rate decreased by " + amount + " for queue '" + queueId.getValue() + "'.");
        //     } catch (Exception ex) { showError(ex.getMessage()); }
        // });
 
        // clearBtn.addClickListener(e -> {
        //     try {
        //         systemAdminService.clearQueue(token, Long.parseLong(queueId.getValue()));
        //         showSuccess("Queue '" + queueId.getValue() + "' cleared.");
        //     } catch (Exception ex) { showError(ex.getMessage()); }
        // });
 
        card.add(queueId, flowAmount, increaseBtn, decreaseBtn, clearBtn);
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
                .set("grid-template-columns", "2fr 1fr 2fr 1fr")
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
