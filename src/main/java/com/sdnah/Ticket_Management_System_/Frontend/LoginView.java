package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("login")
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle()
                .set("background", "#f5f5f5")
                .set("font-family", "Arial, sans-serif");

        Div page = new Div();
        page.getStyle()
                .set("display", "flex")
                .set("width", "950px")
                .set("height", "570px")
                .set("background", "white")
                .set("box-shadow", "0 8px 30px rgba(0,0,0,0.12)")
                .set("border-radius", "4px")
                .set("overflow", "hidden");

        Div left = createLeftSide();
        Div right = createRightSide();

        page.add(left, right);
        add(page);
    }

    private Div createLeftSide() {
        Div left = new Div();
        left.getStyle()
                .set("width", "50%")
                .set("background", "#050505")
                .set("color", "white")
                .set("padding", "50px")
                .set("box-sizing", "border-box");

        H2 logo = new H2("VibePass");
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "26px")
                .set("font-weight", "800");

        H1 welcome = new H1("WELCOME");
        welcome.getStyle()
                .set("margin-top", "150px")
                .set("margin-bottom", "12px")
                .set("font-size", "52px")
                .set("font-weight", "900")
                .set("letter-spacing", "1px");

        Div blueLine = new Div();
        blueLine.getStyle()
                .set("width", "85px")
                .set("height", "5px")
                .set("background", "#0057ff")
                .set("margin-bottom", "28px");

        Paragraph text = new Paragraph(
                "Discover millions of events, get alerts about your favorite artists, teams, plays and more — plus always-secure, effortless ticketing."
        );
        text.getStyle()
                .set("font-size", "16px")
                .set("line-height", "1.7")
                .set("color", "#eeeeee")
                .set("max-width", "340px");

        left.add(logo, welcome, blueLine, text);
        return left;
    }

    private Div createRightSide() {
        Div right = new Div();
        right.getStyle()
                .set("width", "50%")
                .set("padding", "70px 55px")
                .set("box-sizing", "border-box");

        H1 title = new H1("SIGN IN");
        title.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "900")
                .set("margin", "0 0 18px 0")
                .set("color", "#111111");

        Paragraph subtitle = new Paragraph(
                "If you don't have an account you will be prompted to create one."
        );
        subtitle.getStyle()
                .set("font-size", "15px")
                .set("color", "#555555")
                .set("line-height", "1.5")
                .set("margin-bottom", "35px");

        TextField email = new TextField("Email Address");
        email.setWidthFull();

        PasswordField password = new PasswordField("Password");
        password.setWidthFull();

        Button continueButton = new Button("Continue");
        continueButton.setWidthFull();
        continueButton.getStyle()
                .set("height", "48px")
                .set("background", "#0057ff")
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("border-radius", "2px")
                .set("cursor", "pointer")
                .set("margin-top", "18px");

        continueButton.addClickListener(event -> {
            if (email.isEmpty() || password.isEmpty()) {
                Notification.show("Please enter email and password");
                return;
            }

            Notification.show("Login clicked: " + email.getValue());
        });

        Paragraph createAccount = new Paragraph("New to the show? Create an account");
        createAccount.getStyle()
                .set("text-align", "center")
                .set("font-size", "14px")
                .set("color", "#0057ff")
                .set("font-weight", "700")
                .set("margin-top", "35px");

        right.add(title, subtitle, email, password, continueButton, createAccount);
        return right;
    }
}