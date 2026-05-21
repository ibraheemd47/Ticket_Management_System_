package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ProfileResponse;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.UpdateProfileRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("profile-details")
public class ProfileDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private final UserService userService;

    public ProfileDetailsView(UserService userService) {
        this.userService = userService;

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

        Div nav = new Div();
        nav.getStyle()
                .set("display", "flex")
                .set("gap", "40px")
                .set("align-items", "center");

        Span profile = createNavItem("Back to Profile", "profile");
        Span home = createNavItem("Home", "main");

        nav.add(profile, home);
        header.add(logo, nav);

        return header;
    }

    private Div createContent() {
        String token = (String) UI.getCurrent().getSession().getAttribute("token");

        ProfileResponse profile = null;
        try {
            profile = userService.getMyProfile(token);
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }

        Div wrapper = new Div();
        wrapper.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "40px 60px");

        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.08)")
                .set("padding", "42px 52px")
                .set("box-sizing", "border-box")
                .set("max-width", "750px")
                .set("margin", "0 auto");

        Div titleRow = new Div();
        titleRow.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "22px")
                .set("margin-bottom", "35px");

        Span icon = new Span("🪪");
        icon.getStyle().set("font-size", "42px");

        String fullName = "";

        if (profile != null) {
            fullName =
                    (profile.getFirstName() != null ? profile.getFirstName() : "")
                            + " "
                            + (profile.getLastName() != null ? profile.getLastName() : "");
        }

        H1 title = new H1(fullName.isBlank() ? "My Profile" : fullName);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "38px")
                .set("font-weight", "900");

        titleRow.add(icon, title);

        H2 detailsTitle = new H2("Profile Details");
        detailsTitle.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "400")
                .set("margin", "0 0 35px 70px");

        TextField firstName = createField("First Name", profile != null ? profile.getFirstName() : "");
        TextField lastName = createField("Last Name", profile != null ? profile.getLastName() : "");
        TextField email = createField("Email", profile != null ? profile.getEmail() : "");
        TextField phone = createField("Phone Number", profile != null ? profile.getPhone() : "");

        Button updateProfile = createBlueButton("Update Profile");
        updateProfile.addClickListener(e -> saveProfile(
                token,
                firstName,
                lastName,
                email,
                phone
        ));

        card.add(
                titleRow,
                detailsTitle,
                createSection("My Info", firstName, lastName, email, phone, updateProfile)
        );

        wrapper.add(card);
        return wrapper;
    }

    private void saveProfile(
            String token,
            TextField firstName,
            TextField lastName,
            TextField email,
            TextField phone) {

        try {
            UpdateProfileRequest request = new UpdateProfileRequest();

            request.setFirstName(firstName.getValue());
            request.setLastName(lastName.getValue());
            request.setEmail(email.getValue());
            request.setPhone(phone.getValue());

            userService.updateMyProfile(token, request);

            Notification.show("Profile updated successfully");

        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
    }

    private Div createSection(String title, com.vaadin.flow.component.Component... components) {
        Div section = new Div();
        section.getStyle().set("margin-bottom", "45px");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "900")
                .set("margin", "0 0 28px 0");

        section.add(sectionTitle);
        section.add(components);

        return section;
    }

    private TextField createField(String label, String value) {
        TextField field = new TextField(label);
        field.setWidthFull();

        if (value != null) {
            field.setValue(value);
        }

        field.getStyle()
                .set("font-size", "20px")
                .set("margin-bottom", "22px");

        return field;
    }

    private Button createBlueButton(String text) {
        Button button = new Button(text);

        button.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "800")
                .set("font-size", "20px")
                .set("padding", "18px 32px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "8px");

        return button;
    }

    private Span createNavItem(String text, String route) {
        Span item = new Span(text);

        item.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "700");

        item.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        return item;
    }
}