package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Frontend.Presenters.UserPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("login")
public class LoginView extends VerticalLayout {

    private final UserPresenter presenter;

    // We inject the Presenter instead of the UserService
    public LoginView(UserPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setLoginView(this); // Link the view to the presenter

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(createHeader());

        Div cardWrapper = new Div();
        cardWrapper.getStyle()
                .set("display", "flex")
                .set("width", "100%")
                .set("flex-grow", "1")
                .set("align-items", "center")
                .set("justify-content", "center");

        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("width", "950px")
                .set("height", "570px")
                .set("background", "white")
                .set("box-shadow", "0 8px 30px rgba(0,0,0,0.12)")
                .set("border-radius", "8px")
                .set("overflow", "hidden");

        card.add(createLeftSide(), createRightSide());
        cardWrapper.add(card);
        add(cardWrapper);
    }

    private Div createHeader() {
        Div header = new Div();
        header.getStyle()
                .set("width", "100%")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "20px 52px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("box-sizing", "border-box");

        H1 logo = new H1("TICKET MANAGEMENT");
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900")
                .set("cursor", "pointer");

        logo.addClickListener(event -> navigateTo("main"));

        header.add(logo);
        return header;
    }

    private Div createLeftSide() {
        Div left = new Div();
        left.getStyle()
                .set("width", "50%")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "50px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center");

        H2 logo = new H2("VibePass");
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "26px")
                .set("font-weight", "800");

        H1 welcome = new H1("WELCOME");
        welcome.getStyle()
                .set("margin-top", "40px")
                .set("margin-bottom", "12px")
                .set("font-size", "52px")
                .set("font-weight", "900")
                .set("letter-spacing", "1px");

        Div whiteLine = new Div();
        whiteLine.getStyle()
                .set("width", "85px")
                .set("height", "5px")
                .set("background", "white")
                .set("margin-bottom", "28px")
                .set("border-radius", "2px");

        Paragraph text = new Paragraph(
                "Discover millions of events, get alerts about your favorite artists, teams, plays and more — plus always-secure, effortless ticketing."
        );
        text.getStyle()
                .set("font-size", "16px")
                .set("line-height", "1.7")
                .set("color", "#eeeeee")
                .set("max-width", "340px");

        left.add(logo, welcome, whiteLine, text);
        return left;
    }

    private Div createRightSide() {
        Div right = new Div();
        right.getStyle()
                .set("width", "50%")
                .set("padding", "70px 55px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center");

        H1 title = new H1("SIGN IN");
        title.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "900")
                .set("margin", "0 0 18px 0")
                .set("color", "#111111");

        Paragraph subtitle = new Paragraph("Enter your username and password to continue.");
        subtitle.getStyle()
                .set("font-size", "15px")
                .set("color", "#555555")
                .set("line-height", "1.5")
                .set("margin-bottom", "35px");

        TextField username = new TextField("Username");
        username.setWidthFull();

        PasswordField password = new PasswordField("Password");
        password.setWidthFull();

        Button continueButton = new Button("Continue");
        continueButton.setWidthFull();
        continueButton.getStyle()
                .set("height", "48px")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "18px");

        // --------------------------------------------------------
        // DELEGATE LOGIN LOGIC TO PRESENTER
        // --------------------------------------------------------
        continueButton.addClickListener(event -> {
            presenter.handleLogin(username.getValue(), password.getValue());
        });

        Paragraph createAccount = new Paragraph("New to the show? Create an account");
        createAccount.getStyle()
                .set("text-align", "center")
                .set("font-size", "14px")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("margin-top", "35px")
                .set("cursor", "pointer");

        createAccount.addClickListener(e -> navigateTo("signup"));

        right.add(title, subtitle, username, password, continueButton, createAccount);
        return right;
    }

    // =========================================================================
    // PRESENTER CALLBACK METHODS (These let the presenter command the UI safely)
    // =========================================================================

    public void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    public void navigateTo(String route) {
        getUI().ifPresent(ui -> ui.navigate(route));
    }

    public void storeSessionData(String token, String memberId) {
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("token", token);
            ui.getSession().setAttribute("userId", memberId);
        });
    }
}