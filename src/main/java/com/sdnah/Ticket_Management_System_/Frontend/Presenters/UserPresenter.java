package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Frontend.LoginView;
import com.sdnah.Ticket_Management_System_.Frontend.SignUpView;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class UserPresenter {

    private final UserService userService;
    private LoginView view;
    private SignUpView signUpView;

    public UserPresenter(UserService userService) {
        this.userService = userService;
    }

    public void setView(LoginView view) {
        this.view = view;
    }
     public void setSignUpView(SignUpView signUpView) {
        this.signUpView = signUpView;
    }

    public void handleLogin(String username, String password) {
        // 1. Validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            view.showNotification("Please enter username and password", true);
            return;
        }

        try {
            // 2. Authenticate and fetch data
            String token = userService.login(username, password);
            String memberId = userService.getMemberIdByToken(token);

            // 3. Tell the view to store the session data
            view.storeSessionData(token, memberId);
            view.showNotification("Login successful", false);

            // 4. Decide where to navigate
            if (userService.isSystemAdmin(token)) {
                view.navigateTo("admin");
            } else {
                view.navigateTo("main");
            }

        } catch (Exception ex) {
            // Catch invalid credentials or other errors
            view.showNotification(ex.getMessage(), true);
        }
    }
   

    public void handleSignUp(String username, String email, String ageStr, String phone, String password, String confirmPassword) {
        // 1. Check for empty fields
        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            signUpView.showNotification("Please fill in all fields.", true);
            return;
        }

        // 2. Validate Phone Number
        String phoneRegex = "^05[0-8]\\d{7}$";
        if (!phone.matches(phoneRegex)) {
            signUpView.showNotification("Invalid phone number. It must be exactly 10 digits and start with 050-058.", true);
            return;
        }

        // 3. Validate Age
        String ageRegex = "^(1[89]|[2-9]\\d)$"; // Validates age between 18 and 99
        if (!ageStr.matches(ageRegex)) {
            signUpView.showNotification("Invalid age. You must be at least 18 years old.", true);
            return;
        }

        // 4. Validate Passwords match
        if (!password.equals(confirmPassword)) {
            signUpView.showNotification("Passwords do not match!", true);
            return;
        }

        // 5. Proceed with Registration
        try {
            userService.register(
                    username,
                    password,
                    email,
                    phone,
                    Integer.parseInt(ageStr),
                VerificationMethod.EMAIL
            );

            signUpView.showNotification("Account created. Please enter the verification code.", false);
            signUpView.storePendingSessionData(username, password);
            signUpView.navigateTo("verify-account");

        } catch (Exception ex) {
            signUpView.showNotification(ex.getMessage(), true);
        }
    }
    public void verifyAccount(String username, String code) {
        try {
            userService.verifyAccount(username, code);
            view.showNotification("Account verified successfully! Please log in.", false);
            view.navigateTo("login");
        } catch (Exception ex) {
            view.showNotification(ex.getMessage(), true);
        }
    }

    public String loginAndGetToken(String value, String tempPassword) {
        try {
            return userService.login(value, tempPassword);
        } catch (Exception ex) {
            view.showNotification("Login failed: " + ex.getMessage(), true);
            return null;
        }
    }
   

 
}