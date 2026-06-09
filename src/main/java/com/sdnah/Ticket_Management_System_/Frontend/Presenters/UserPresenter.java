package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.endpoints.LoginPage;
import com.sdnah.Ticket_Management_System_.Frontend.LoginView;
import com.sdnah.Ticket_Management_System_.Frontend.SignUpView;
import com.sdnah.Ticket_Management_System_.Frontend.VerifyAccountView;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class UserPresenter {

   
    private final UserService userService;
    private LoginView Loginview;
    private SignUpView signUpView;
    private VerifyAccountView verifyAccountView;

    public UserPresenter(UserService userService) {
        this.userService = userService;
    }

    public void setLoginView(LoginView view) {
        this.Loginview = view;
    }
     public void setSignUpView(SignUpView signUpView) {
        this.signUpView = signUpView;
    }
    public void setVerifyAccountView(VerifyAccountView verifyAccountView) {
            this.verifyAccountView = verifyAccountView;
        }
    public void handleLogin(String username, String password) {
        // 1. Validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            Loginview.showNotification("Please enter username and password", true);
            return;
        }

        try {
            // 2. Authenticate and fetch data
            String token = userService.login(username, password);
            String memberId = userService.getMemberIdByToken(token);

            // 3. Tell the view to store the session data
            Loginview.storeSessionData(token, memberId);
            Loginview.showNotification("Login successful", false);

            // 4. Decide where to navigate
            if (userService.isSystemAdmin(token)) {
                Loginview.navigateTo("admin");
            } else {
                Loginview.navigateTo("main");
            }

        } catch (Exception ex) {
            // Catch invalid credentials or other errors
            Loginview.showNotification(ex.getMessage(), true);
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
   public void verifyAccount(String username, String password, String code) {
        try {
            // 1. Verify the account using the code
            userService.verifyAccount(username, code);

            // 2. Automatically log them in using the credentials we passed in
            String token = userService.login(username, password);
            String memberId = userService.getMemberIdByToken(token);

            // 3. Store the session data so the app knows they are logged in
            verifyAccountView.storeSessionData(token, memberId);

            // 4. Show success and send them to the main page!
            verifyAccountView.showNotification("Account verified! Logging you in...", false);
            
            if (userService.isSystemAdmin(token)) {
                verifyAccountView.navigateTo("admin");
            } else {
                verifyAccountView.navigateTo("main");
            }

        } catch (Exception ex) {
            verifyAccountView.showNotification(ex.getMessage(), true);
        }
    }

    public String loginAndGetToken(String value, String tempPassword) {
        try {
            return userService.login(value, tempPassword);
        } catch (Exception ex) {
            Loginview.showNotification("Login failed: " + ex.getMessage(), true);
            return null;
        }
    }
   

 
}