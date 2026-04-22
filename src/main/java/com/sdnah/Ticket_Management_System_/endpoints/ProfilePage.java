package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ProfilePage")
public class ProfilePage {
    //Show User Details By User ID
    // add the ability to edit user details (name, email, phone number , password , location etc.)
    @GetMapping("/ViewProfile")
    public ResponseEntity<String> ViewProfile() {
        return ResponseEntity.ok("view profile endpoint!");
    }
}
