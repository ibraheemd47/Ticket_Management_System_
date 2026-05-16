package com.sdnah.Ticket_Management_System_.Frontend;

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

@Route("admin")
public class SystemAdminView extends VerticalLayout implements BeforeEnterObserver {
    private String selectedTab = "users";

    public SystemAdminView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
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
    wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap","wrap");

    Div usersListCard = actionCard("Registered Users",
    "Browse all registered platform users. The list will appear here after backend connection.");
    //systemAdminService.getAllUsers()
    Div usersPlaceholder = new Div();
    usersPlaceholder.getStyle()
    .set("background", "#f9fafb")
    .set("border", "1px dashed #d1d5db")
    .set("border-radius", "8px")
    .set("padding", "40px 24px")
    .set("text-align", "center");

    Paragraph usersText = new Paragraph(
    "Registered users list will appear here after connecting the backend.");

    usersText.getStyle()
    .set("color", "#9ca3af")
    .set("font-size", "14px")
    .set("margin", "0");

    usersPlaceholder.add(usersText);
    usersListCard.add(usersPlaceholder);

    wrapper.add(usersListCard);

    // Suspend user card — II.6.7
    Div suspendCard = actionCard("Suspend User","Enter the Member ID of the member you wish to suspend. Suspended members can browse the platform but cannot perform any actions.");

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

    type.addValueChangeListener(e ->
    hours.setVisible(e.getValue().equals("Temporary")));

    Button suspendBtn = actionButton("Suspend User", "#026cdf");

    suspendBtn.addClickListener(e -> {
    if (suspendMemberID.isEmpty()) {
    showError("Please enter a Member ID.");
    return;
    }
    // TsystemAdminService.suspendUser(token, memberId, hours)
    showSuccess("User '" + suspendMemberID.getValue() + "' has been suspended.");
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
    // systemAdminService.removeMember(token, memberId)
    showSuccess("Member '" + removeMemberID.getValue() + "' has been removed.");
    removeMemberID.clear();
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

        TextField MemberID = styledField("Member ID to unsuspend");
        Button unsuspendBtn = actionButton("Unsuspend User", "#026cdf");
        unsuspendBtn.addClickListener(e -> {
            if (MemberID.isEmpty()) {
                showError("Please enter a MemberID.");
                return;
            }
            // systemAdminService.unsuspendUser(token, memberId)
            showSuccess("User '" + MemberID.getValue() + "' has been unsuspended.");
            MemberID.clear();
        });

        unsuspendCard.add(MemberID, unsuspendBtn);
        wrapper.add(unsuspendCard);

        // View suspensions card — II.6.9
        Div viewCard = actionCard("View Suspensions",
                "Load a list of all currently suspended members, including suspension start date and expiry.");

        Button viewBtn = actionButton("Load Suspended Users", "#026cdf");
        Div resultArea = new Div();
        resultArea.getStyle().set("margin-top", "16px");

        viewBtn.addClickListener(e -> {
            // systemAdminService.getSuspensions(token)
            resultArea.removeAll();
            Paragraph p = new Paragraph("📋 charlie — suspended until 2026-05-31 (30 days)");
            p.getStyle().set("background", "#f3f4f6").set("padding", "12px").set("border-radius", "8px");
            resultArea.add(p);
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
        Div companiesList = new Div();
        companiesList.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("padding", "28px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.07)")
                .set("border-top", "4px solid #026cdf")
                .set("width", "340px")
                .set("min-height", "420px")
                .set("box-sizing", "border-box");

        H3 listTitle = new H3("Production Companies");
        listTitle.getStyle()
                .set("margin", "0 0 8px 0")
                .set("font-size", "18px")
                .set("font-weight", "700");

        Paragraph listDesc = new Paragraph(
                "All production companies will appear here after connecting the backend.");
        listDesc.getStyle()
                .set("color", "#6b7280")
                .set("font-size", "14px")
                .set("line-height", "1.5")
                .set("margin", "0 0 20px 0");

        Div placeholder = new Div();
        placeholder.getStyle()
                .set("background", "#f9fafb")
                .set("border", "1px dashed #d1d5db")
                .set("border-radius", "8px")
                .set("padding", "40px 24px")
                .set("text-align", "center");

        Paragraph p = new Paragraph(
                "Production companies list will appear here after connecting the backend.");
        p.getStyle()
                .set("color", "#9ca3af")
                .set("font-size", "14px")
                .set("margin", "0");

        placeholder.add(p);

        companiesList.add(listTitle, listDesc, placeholder);

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
            // systemAdminService.closeCompany(token, companyId)
            showSuccess("Company '" + companyId.getValue() + "' has been closed.");

            companyId.clear();
            reason.clear();
        });
        card.add(companyId, reason, closeBtn);
        wrapper.add(companiesList, card);

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
            // notificationService.sendSystemMessage(token, recipient, message)
            showSuccess("Message sent to '" + recipient.getValue() + "'.");
            recipient.clear();
            message.clear();
        });

        msgCard.add(recipient, message, sendBtn);
        wrapper.add(msgCard);

        return wrapper;
    }

    // TAB: ANALYTICS — II.6.5
    private Div buildAnalytics() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr 1fr").set("gap", "24px");

        wrapper.add(
                analyticsCard("Active Sessions", "👥", "Live visitors right now"),
                analyticsCard("New Registrations", "🆕", "Members registered today"),
                analyticsCard("Tickets Reserved", "🎟️", "Active reservations"),
                analyticsCard("Purchases Today", "💳", "Completed transactions"),
                analyticsCard("Peak Load", "📈", "Max concurrent users today"),
                analyticsCard("Reservation Rate", "⏱️", "Reservations per minute"));

        // TODO: connect AnalyticsService to display live data (II.6.5)
        return wrapper;
    }

    private Div analyticsCard(String title, String value, String desc) {
        Div c = new Div();
        c.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("padding", "24px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.07)")
                .set("border-top", "4px solid #026cdf");

        H3 val = new H3(value);
        val.getStyle().set("margin", "0 0 8px 0").set("font-size", "30px").set("font-weight", "900").set("color",
                "#026cdf");

        Paragraph t = new Paragraph(title);
        t.getStyle().set("margin", "0 0 4px 0").set("font-weight", "700").set("font-size", "14px").set("color", "#111");

        Paragraph d = new Paragraph(desc);
        d.getStyle().set("margin", "0").set("font-size", "13px").set("color", "#9ca3af");

        c.add(val, t, d);
        return c;
    }

    // TAB: QUEUES — II.6.6
    private Div buildQueues() {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex").set("gap", "24px").set("flex-wrap", "wrap");
        // View queues
        Div viewCard = actionCard("View Active Queues",
                "Monitor all active virtual queues across the platform. Use this during high-demand ticket releases to track queue status.");

        Div placeholderQ = new Div();
        placeholderQ.getStyle()
                .set("background", "#f9fafb")
                .set("border", "1px dashed #d1d5db")
                .set("border-radius", "8px")
                .set("padding", "40px 24px")
                .set("text-align", "center");
        Paragraph ptQ = new Paragraph("Active queue list will appear here after connecting the backend.");
        ptQ.getStyle().set("color", "#9ca3af").set("font-size", "14px").set("margin", "0");
        placeholderQ.add(ptQ);
        viewCard.add(placeholderQ);
        wrapper.add(viewCard);
        // Control queue
        Div controlCard = actionCard("Control Queue",
                "Select a queue by its event ID and adjust the user flow rate, or clear the queue entirely if a technical issue occurs.");

        TextField queueId = styledField("Queue / Event ID");

        Button increaseBtn = actionButton("Increase Flow", "#026cdf");
        increaseBtn.addClickListener(e -> {
            if (queueId.isEmpty()) {
                showError("Please enter a Queue ID.");
                return;
            }
            // queueService.increaseFlowRate(token, queueId)
            showSuccess("Flow rate increased for queue '" + queueId.getValue() + "'.");
        });

        Button decreaseBtn = actionButton("Decrease Flow", "#026cdf");
        decreaseBtn.addClickListener(e -> {
            if (queueId.isEmpty()) {
                showError("Please enter a Queue ID.");
                return;
            }
            // queueService.decreaseFlowRate(token, queueId)
            showSuccess("Flow rate decreased for queue '" + queueId.getValue() + "'.");
        });

        Button clearBtn = actionButton("Clear Queue", "#026cdf");
        clearBtn.addClickListener(e -> {
            if (queueId.isEmpty()) {
                showError("Please enter a Queue ID.");
                return;
            }
            // queueService.clearQueue(token, queueId)
            showSuccess("Queue '" + queueId.getValue() + "' cleared.");
        });

        controlCard.add(queueId, increaseBtn, decreaseBtn, clearBtn);
        wrapper.add(controlCard);

        return wrapper;
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
