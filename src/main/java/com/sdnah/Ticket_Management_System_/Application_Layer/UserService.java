package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }


    public boolean register(String username, String password) {
        validateUsername(username);
        validatePassword(password);

        //check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        String memberId = get_new_member_id();
        String passwordHash = passwordHasher.hash(password);

        Member member = new Member(memberId, username, passwordHash);
        userRepository.save(member);

        return true;
    }








    //===================================================================================================================================
    //                                                      HELPER METHODS
    //===================================================================================================================================

    private String get_new_member_id() {
        String newId;
        do {
            newId = UUID.randomUUID().toString();
        } while (userRepository.findById(newId).isPresent());
        return newId;
    }


    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new RuntimeException("Password must contain at least 6 characters");
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username cannot be empty");
        }
        if (username.length() < 3) {
            throw new RuntimeException("Username must contain at least 3 characters");
        }
    }
}
