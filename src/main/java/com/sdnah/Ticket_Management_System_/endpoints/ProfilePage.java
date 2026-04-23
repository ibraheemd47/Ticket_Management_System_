package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.ProfileDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

@RestController
@RequestMapping("/api/ProfilePage")
public class ProfilePage {

    private final UserService userService;

    public ProfilePage(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestParam String token) {
        try {
            Member member = userService.getMemberByToken(token);

            ProfileDTO dto = new ProfileDTO();
            dto.setMemberId(member.getMemberId());
            dto.setUsername(member.getUsername());
            dto.setFirstName(member.getFirstName());
            dto.setLastName(member.getLastName());
            dto.setFullName(member.getFullName());
            dto.setEmail(member.getEmail());
            dto.setPhone(member.getPhone());
            dto.setAddress(member.getAddress());
            dto.setCity(member.getCity());
            dto.setCountry(member.getCountry());
            dto.setBirthDate(member.getBirthDate());
            dto.setActive(member.isActive());
            dto.setLoggedin(member.isLoggedin());
            dto.setVerified(member.isVerified());
            dto.setRole(member.getRole() == null ? null : member.getRole().name());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}