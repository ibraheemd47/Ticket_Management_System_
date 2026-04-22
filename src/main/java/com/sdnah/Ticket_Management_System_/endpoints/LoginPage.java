package com.sdnah.Ticket_Management_System_.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/LoginPage")
public class LoginPage {
    @PostMapping("/login")
    public ResponseEntity<String> login(){ // should take username and password as input and return a token if successful
        return ResponseEntity.ok("login endpoint!");
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(){ // should take username password and email as input and return a token if successful
        return ResponseEntity.ok("register endpoint!");
    }

}
