package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.RequestRegisterDTO;

@RestController
@RequestMapping("/api/SignUpPage")
public class SignUpPage {

    private final UserService userService;

    public SignUpPage(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RequestRegisterDTO request) {
        String memberId = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getPhone(),
                request.getVerificationMethod());
        return ResponseEntity.ok("User registered successfully. Verification code sent.");
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody String username, @RequestBody String code) {
        userService.verifyAccount(username, code);

        return ResponseEntity.ok(
                "Account verified successfully");
    }
}
