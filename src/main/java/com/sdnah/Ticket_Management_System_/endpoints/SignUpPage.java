package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/SignUpPage")
public class SignUpPage {
 @PostMapping("/SignUpByDetails") // sign up by email then check if the email is associated with a member or not if not then will be redirected to signup page 
    public ResponseEntity<String> SignUp(String email, String password, String firstname, String lastname, String phoneNumber, String address,String birthDate) {
        return ResponseEntity.ok("sign up endpoint!");
    }
 @GetMapping("/signupButton")
    public ResponseEntity<String> signupButton() {
        return ResponseEntity.ok("signup button endpoint!");
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
