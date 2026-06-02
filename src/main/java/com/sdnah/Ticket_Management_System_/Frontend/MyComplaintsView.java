package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("my-complaints")
public class MyComplaintsView extends VerticalLayout implements BeforeEnterObserver {

    private final ComplaintService complaintService;

    public MyComplaintsView(ComplaintService complaintService) {
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

        logo.addClickListener(e -> UI.getCurrent().navigate("main"));

        Span back = new Span("← Back to Profile");
        back.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "700");

        back.addClickListener(e -> UI.getCurrent().navigate("profile"));

        header.add(logo, back);
        return header;
    }

    private Div createContent() {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "50px 70px");

        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "14px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.08)")
                .set("padding", "40px")
                .set("max-width", "900px")
                .set("margin", "0 auto");

        H1 title = new H1("My Complaints");
        title.getStyle()
                .set("margin", "0 0 25px 0")
                .set("font-size", "36px");

        Button fileComplaint = new Button("File New Complaint",
                e -> UI.getCurrent().navigate("complaints"));

        fileComplaint.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-bottom", "25px");

        card.add(title, fileComplaint);

        try {
            String token = (String) UI.getCurrent()
                    .getSession()
                    .getAttribute("token");

            List<ComplaintDTO> complaints = complaintService.getUserComplaints(token);

            if (complaints == null || complaints.isEmpty()) {
                Paragraph empty = new Paragraph("You have not submitted any complaints yet.");
                empty.getStyle().set("color", "#6b7280");
                card.add(empty);
            } else {
                for (ComplaintDTO c : complaints) {
                    card.add(createComplaintCard(c));
                }
            }

        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }

        wrapper.add(card);
        return wrapper;
    }

    private Div createComplaintCard(ComplaintDTO c) {
        Div box = new Div();
        box.getStyle()
                .set("border", "1px solid #e5e7eb")
                .set("border-left", "5px solid #026cdf")
                .set("border-radius", "10px")
                .set("padding", "18px")
                .set("margin-bottom", "18px")
                .set("background", "#f9fafb");

        H2 subject = new H2(c.getSubject());
        subject.getStyle()
                .set("font-size", "20px")
                .set("margin", "0 0 8px 0");

        Paragraph status = new Paragraph("Status: " + c.getStatus());
        status.getStyle()
                .set("font-weight", "700")
                .set("margin", "0 0 8px 0");

        Paragraph description = new Paragraph("Description: " + c.getDescription());
        description.getStyle().set("margin", "0 0 8px 0");

        String responseText = c.getAdminResponse() == null || c.getAdminResponse().isBlank()
                ? "No response yet"
                : c.getAdminResponse();

        Paragraph response = new Paragraph("Admin Response: " + responseText);
        response.getStyle()
                .set("margin", "0")
                .set("color", "#374151");

        box.add(subject, status, description, response);
        return box;
    }
}