package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;


import com.sdnah.Ticket_Management_System_.DTOs.ProfileResponse;
import com.sdnah.Ticket_Management_System_.DTOs.UpdateProfileRequest;

@RestController
@RequestMapping("/api/ProfilePage")
public class ProfilePage {

    private final UserService userService;

    public ProfilePage(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = extractToken(authorizationHeader);
        return ResponseEntity.ok(userService.getMyProfile(token));
    }

    @PutMapping("/update")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody UpdateProfileRequest request) {

        String token = extractToken(authorizationHeader);
        return ResponseEntity.ok(userService.updateMyProfile(token, request));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new RuntimeException("Missing Authorization header");
        }

        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return authorizationHeader;
    }
}