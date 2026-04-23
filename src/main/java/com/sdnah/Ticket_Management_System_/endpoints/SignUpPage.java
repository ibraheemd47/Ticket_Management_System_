package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.RequestRegisterDTO;
import com.sdnah.Ticket_Management_System_.DTOs.VerifyAccountDTO;

@RestController
@RequestMapping("/api/SignUpPage")
public class SignUpPage {

    private final UserService userService;

    public SignUpPage(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RequestRegisterDTO request) {
        String membre_Id;
        try {
            membre_Id = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getVerificationMethod());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (membre_Id == null) {
            return ResponseEntity.badRequest().body("Registration failed. Please try again.");
        }
        return ResponseEntity.ok("User registered successfully. Verification code sent.");
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerifyAccountDTO verifyAccountDTO) {
        userService.verifyAccount(verifyAccountDTO.getUsername(), verifyAccountDTO.getCode());

        return ResponseEntity.ok(
                "Account verified successfully");
    }
}
