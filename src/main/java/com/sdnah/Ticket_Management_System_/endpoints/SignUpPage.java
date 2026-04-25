package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.RequestRegisterDTO;
import com.sdnah.Ticket_Management_System_.DTOs.SignUpResponse;
import com.sdnah.Ticket_Management_System_.DTOs.VerifyAccountDTO;

@RestController
@RequestMapping("/api/SignUpPage")
public class SignUpPage {

    private final UserService userService;

    public SignUpPage(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<SignUpResponse> register(@RequestBody RequestRegisterDTO request) {
        try {
            String memberId = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getVerificationMethod());

            return ResponseEntity.ok(
                    new SignUpResponse(
                            true,
                            "User registered successfully. Verification code sent.",
                            memberId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new SignUpResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerifyAccountDTO verifyAccountDTO) {
        userService.verifyAccount(verifyAccountDTO.getUsername(), verifyAccountDTO.getCode());
        return ResponseEntity.ok("Account verified successfully");
    }
}