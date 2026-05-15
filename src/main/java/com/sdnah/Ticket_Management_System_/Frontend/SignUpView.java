package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("signup")
public class SignUpView extends VerticalLayout {

    public SignUpView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // 1. Match the global page background
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        // 2. Add the Header to the top
        Div header = createHeader();
        add(header);

        // 3. Create a wrapper to center the card
        Div cardWrapper = new Div();
        cardWrapper.getStyle()
                .set("display", "flex")
                .set("width", "100%")
                .set("flex-grow", "1")
                .set("align-items", "center")
                .set("justify-content", "center");

        // 4. The main Sign Up Card (Slightly taller to fit extra fields)
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("width", "950px")
                .set("min-height", "620px") // Increased slightly from 570px for the extra fields
                .set("background", "white")
                .set("box-shadow", "0 8px 30px rgba(0,0,0,0.12)")
                .set("border-radius", "8px")
                .set("overflow", "hidden");

        Div left = createLeftSide();
        Div right = createRightSide();

        card.add(left, right);
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
                
        logo.addClickListener(event -> {
            getUI().ifPresent(ui -> ui.navigate("")); 
        });
        
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

        H1 welcome = new H1("JOIN US"); // Changed from WELCOME
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
                "Create an account to unlock exclusive presales, track your favorite artists, and manage your tickets securely."
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
                .set("padding", "40px 55px") // Reduced top/bottom padding slightly to fit fields
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center");

        H1 title = new H1("SIGN UP");
        title.getStyle()
                .set("font-size", "30px")
                .set("font-weight", "900")
                .set("margin", "0 0 10px 0")
                .set("color", "#111111");

        Paragraph subtitle = new Paragraph("Enter your details to create a new account.");
        subtitle.getStyle()
                .set("font-size", "15px")
                .set("color", "#555555")
                .set("line-height", "1.5")
                .set("margin-bottom", "20px");

        // First and Last Name side-by-side
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setWidthFull();
        
        TextField firstName = new TextField("First Name");
        firstName.setWidthFull();
        
        TextField lastName = new TextField("Last Name");
        lastName.setWidthFull();
        
        nameLayout.add(firstName, lastName);

        // Email
        TextField email = new TextField("Email Address");
        email.setWidthFull();

        // Passwords
        PasswordField password = new PasswordField("Password");
        password.setWidthFull();

        PasswordField confirmPassword = new PasswordField("Confirm Password");
        confirmPassword.setWidthFull();

        // Sign Up Button
        Button signupButton = new Button("Create Account");
        signupButton.setWidthFull();
        signupButton.getStyle()
                .set("height", "48px")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("margin-top", "20px");

        signupButton.addClickListener(event -> {
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Notification.show("Please fill in all fields.");
                return;
            }
            if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Passwords do not match!");
                return;
            }
            Notification.show("Account creation initiated for: " + email.getValue());
            // Uncomment backend logic when ready
        });

        // Redirect to Login Link
        Paragraph alreadyAccount = new Paragraph("Already have an account? Sign in");
        alreadyAccount.getStyle()
                .set("text-align", "center")
                .set("font-size", "14px")
                .set("color", "#026cdf")
                .set("font-weight", "700")
                .set("margin-top", "25px")
                .set("cursor", "pointer"); 
        
        alreadyAccount.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));

        right.add(title, subtitle, nameLayout, email, password, confirmPassword, signupButton, alreadyAccount);
        return right;
    }
}