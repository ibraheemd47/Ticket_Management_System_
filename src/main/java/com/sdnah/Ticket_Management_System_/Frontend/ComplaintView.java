package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CreateComplaintDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("complaints")
public class ComplaintView extends VerticalLayout implements BeforeEnterObserver {

    private final ComplaintService complaintService;

    public ComplaintView(ComplaintService complaintService) {
        this.complaintService = complaintService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(createHeader());
        add(createContent());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object token = event.getUI().getSession().getAttribute("token");
        if (token == null) {
            event.rerouteTo("login");
        }
    }

    private Div createHeader() {
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

        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("main")));

        Button back = new Button("Back to Profile", e ->
                getUI().ifPresent(ui -> ui.navigate("profile")));

        back.getStyle()
                .set("background", "white")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("border-radius", "8px")
                .set("cursor", "pointer");

        header.add(logo, back);
        return header;
    }

    private Div createContent() {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "70px 20px");

        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("width", "520px")
                .set("padding", "42px")
                .set("border-radius", "14px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.08)");

        H1 title = new H1("File Complaint");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "36px")
                .set("color", "#111827");

        Paragraph subtitle = new Paragraph("Tell us what happened. An admin will review your complaint.");
        subtitle.getStyle()
                .set("color", "#6b7280")
                .set("font-size", "16px")
                .set("margin", "14px 0 28px 0");

        TextField subject = new TextField("Subject");
        subject.setWidthFull();

        TextArea description = new TextArea("Description");
        description.setWidthFull();
        description.setMinHeight("160px");

        TextField targetType = new TextField("Target Type");
        targetType.setPlaceholder("Example: EVENT, USER, ORDER, COMPANY");
        targetType.setWidthFull();

        TextField targetId = new TextField("Target ID");
        targetId.setPlaceholder("Optional ID related to the complaint");
        targetId.setWidthFull();

        Button submit = new Button("Submit Complaint");
        submit.setWidthFull();
        submit.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "14px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "18px");

        submit.addClickListener(e -> {
            if (subject.isEmpty() || description.isEmpty() || targetType.isEmpty()) {
                Notification.show("Please fill subject, description, and target type");
                return;
            }

            try {
                String token = (String) UI.getCurrent()
                        .getSession()
                        .getAttribute("token");

            CreateComplaintDTO request = new CreateComplaintDTO(
                    subject.getValue(),
                    description.getValue(),
                    targetType.getValue(),
                    targetId.getValue()
            );

                complaintService.createComplaint(token, request);

                Notification.show("Complaint submitted successfully");

                subject.clear();
                description.clear();
                targetType.clear();
                targetId.clear();

            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        card.add(title, subtitle, subject, description, targetType, targetId, submit);
        wrapper.add(card);

        return wrapper;
    }
}