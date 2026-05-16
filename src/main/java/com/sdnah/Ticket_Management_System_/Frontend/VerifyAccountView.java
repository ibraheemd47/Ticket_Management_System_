package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("verify-account")
public class VerifyAccountView extends VerticalLayout {

    private final UserService userService;

    public VerifyAccountView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        Div card = new Div();
        card.getStyle()
                .set("width", "430px")
                .set("background", "white")
                .set("padding", "40px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 30px rgba(0,0,0,0.12)")
                .set("box-sizing", "border-box");

        H1 title = new H1("Verify Account");
        title.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "900")
                .set("margin", "0 0 12px 0")
                .set("color", "#111111");

        Paragraph subtitle = new Paragraph("Enter the verification code sent to your email.");
        subtitle.getStyle()
                .set("font-size", "15px")
                .set("color", "#555555")
                .set("line-height", "1.5")
                .set("margin-bottom", "25px");

        TextField username = new TextField("Username");
        username.setWidthFull();

        Object pendingUsername = getUI()
                .map(ui -> ui.getSession().getAttribute("pendingUsername"))
                .orElse(null);

        if (pendingUsername != null) {
            username.setValue(pendingUsername.toString());
        }

        TextField code = new TextField("Verification Code");
        code.setWidthFull();

        Button verifyButton = new Button("Verify Account");
        verifyButton.setWidthFull();
        verifyButton.getStyle()
                .set("height", "48px")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "20px");

        verifyButton.addClickListener(event -> {
            if (username.isEmpty() || code.isEmpty()) {
                Notification.show("Please enter username and verification code");
                return;
            }

            try {
                userService.verifyAccount(username.getValue(), code.getValue());

                Notification.show("Account verified successfully");

                getUI().ifPresent(ui -> {
                    ui.getSession().setAttribute("pendingUsername", null);
                    ui.navigate("login");
                });

            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Paragraph backToLogin = new Paragraph("Back to login");
        backToLogin.getStyle()
                .set("text-align", "center")
                .set("font-size", "14px")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("margin-top", "25px")
                .set("cursor", "pointer");

        backToLogin.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));

        card.add(title, subtitle, username, code, verifyButton, backToLogin);
        add(card);
    }
}