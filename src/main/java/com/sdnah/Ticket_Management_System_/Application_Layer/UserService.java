package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@Service
public class UserService {

    VerificationEmail verificationService;//TODO: need to be inited 
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenRepository tokenRepository;
    private final AuthTokenService authTokenService;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenRepository tokenRepository,
            AuthTokenService authTokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenRepository = tokenRepository;
        this.authTokenService = authTokenService;
    }

    public boolean register(String username, String password) {
        validateUsername(username);
        validatePassword(password);

        // check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        String memberId = get_new_member_id();
        String passwordHash = passwordHasher.hash(password);

        Member member = new Member(memberId, username, passwordHash);
        userRepository.save(member);

        return true;
    }

    public String login(String username, String password) {
        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!member.isActive()) {
            throw new RuntimeException("Member is inactive");
        }

        if (!passwordHasher.matches(password, member.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        member.login(); // Mark the member as logged in
        userRepository.save(member);

        AuthToken token = authTokenService.issueToken(member.getMemberId());
        tokenRepository.save(token);

        return token.getTokenValue();
    }

    public void logout(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new RuntimeException("Token cannot be empty");
        }

        AuthToken to_logout = tokenRepository.findById(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        Member member = userRepository.findById(to_logout.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.logout(); // Mark the member as logged out
        userRepository.save(member);
        tokenRepository.deleteByToken(tokenValue);
    }

    // ===================================================================================================================================
    //                                             HELPER METHODS
    // ===================================================================================================================================
        private Member getMemberById(String targetMemberId) {
        return userRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }
    private void validateCompanyRoleRequest(CompanyRoleAssignment assignment) {
        if (assignment.getCompanyId() == null || assignment.getCompanyId().isBlank()) {
            throw new RuntimeException("Company id cannot be empty");
        }

        if (assignment.getRoleType() == null) {
            throw new RuntimeException("Role type is required");
        }

        if (assignment.getRoleType() == CompanyRoleType.OWNER ||
                assignment.getRoleType() == CompanyRoleType.MANAGER) {
            throw new RuntimeException("Appointed by member id is required");
        }
    }

    public Member getMemberByToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new RuntimeException("Invalid token");
        }

        AuthToken token = tokenRepository.findById(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isExpired(java.time.LocalDateTime.now())) {
            tokenRepository.deleteByToken(tokenValue);
            throw new RuntimeException("Token expired");
        }

        return userRepository.findById(token.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public boolean isTokenValid(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return false;
        }

        AuthToken token = tokenRepository.findById(tokenValue).orElse(null);
        if (token == null) {
            return false;
        }

        if (token.isExpired(java.time.LocalDateTime.now())) {
            tokenRepository.deleteByToken(tokenValue);
            return false;
        }

        return true;
    }

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


    //TODO: need to test 
     public String register(String username,
                           String password,
                           String email,
                           String phone,
                           VerificationMethod verificationMethod) {

        validateUsername(username);
        validatePassword(password);
        validateVerificationTarget(email, phone, verificationMethod);

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        String memberId = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(password);

        Member member = new Member(memberId, username, passwordHash);
        member.setEmail(email);
        member.setPhone(phone);
        member.setVerified(false);
        member.logout();

        userRepository.save(member);

        verificationService.createAndSendCode(member, verificationMethod);

        return memberId;
    }

    public void verifyAccount(String memberId, String code) {
        if (memberId == null || memberId.isBlank()) {
            throw new RuntimeException("Member id is required");
        }

        if (code == null || code.isBlank()) {
            throw new RuntimeException("Verification code is required");
        }

        Member member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        verificationService.verifyCode(member, code);
        member.setVerified(true);
        userRepository.save(member);
    }

    private void validateVerificationTarget(String email,
                                            String phone,
                                            VerificationMethod method) {
        if (method == null) {
            throw new RuntimeException("Verification method is required");
        }

        if (method == VerificationMethod.EMAIL) {
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email is required for email verification");
            }
        }

        if (method == VerificationMethod.PHONE) {
            if (phone == null || phone.isBlank()) {
                throw new RuntimeException("Phone is required for phone verification");
            }
        }
    }

}
