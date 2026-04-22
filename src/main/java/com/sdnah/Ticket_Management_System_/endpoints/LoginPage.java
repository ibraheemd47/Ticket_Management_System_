package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/LoginPage")
public class LoginPage {
    @PostMapping("/LoginbyEmail") // loginby email then check if the email is associated with a member or not if not then will be redirected to signup page 
    public ResponseEntity<String> Login(String email) {
        return ResponseEntity.ok("login endpoint!");
    }
    @GetMapping("/loginButton")
    public ResponseEntity<String> loginButton() {
        return ResponseEntity.ok("login button endpoint!");
    }
    @PostMapping("/VerifyLoginByPhone") // verify login by phone then check if the phone is associated with a member or not if not then will be redirected to signup page
    public ResponseEntity<String> VerifyLoginByPhone(String Code) {
        return ResponseEntity.ok("verify login by phone endpoint!");
    }
    @GetMapping("/resendCodeButton")
    public ResponseEntity<String> resendCodeButton() {
        return ResponseEntity.ok("resend code button endpoint!");
    }
    @PostMapping("/VerifyLoginByEmail") // verify login by email then check if the email is associated with a member or not if not then will be redirected to signup page
    public ResponseEntity<String> VerifyLoginByEmail(String Code) {
        return ResponseEntity.ok("verify login by email endpoint!");
    }
    @GetMapping("/resendCodeEmailButton")
    public ResponseEntity<String> resendCodeEmailButton() {
        return ResponseEntity.ok("resend code email button endpoint!");
    }
}
