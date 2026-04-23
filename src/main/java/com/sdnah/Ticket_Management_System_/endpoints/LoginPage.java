package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.UserDTO;

@RestController
@RequestMapping("/api/LoginPage")
public class LoginPage {

    private final UserService userService;

    public LoginPage(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO userDTO) {
        try {
            String token = userService.login(userDTO.getUsername(), userDTO.getPassword());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody UserDTO userDTO) {
        try {
            userService.logout(userDTO.getToken());
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
