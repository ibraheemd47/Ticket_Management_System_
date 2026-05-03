package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.ProfileResponse;
import com.sdnah.Ticket_Management_System_.DTOs.UpdateProfileRequest;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    VerificationEmail verificationService;// TODO: need to be inited
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenRepository tokenRepository;
    private final AuthTokenService authTokenService;
    private final SystemAdminService systemAdminService;
    private final KeyedLock keyedLock;

    private static final String LOCK_NS_USERNAME = "user:username";
    private static final String LOCK_NS_MEMBER = "user:member";

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenRepository tokenRepository,
            AuthTokenService authTokenService, VerificationEmail verificationService,
            SystemAdminService systemAdminService, KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenRepository = tokenRepository;
        this.authTokenService = authTokenService;
        this.verificationService = verificationService;
        this.systemAdminService = systemAdminService;
        this.keyedLock = keyedLock;
    }

    // TODO: ZAKI DELETE
    public boolean register(String username, String password) {
        logger.info("Register request received for username={}", username);
        validateUsername(username);
        validatePassword(password);

        return keyedLock.callLocked(LOCK_NS_USERNAME, username, () -> {
            // check if username already exists (inside lock to prevent race)
            if (userRepository.existsByUsername(username)) {
                logger.warn("Register rejected: username already exists, username={}", username);
                throw new RuntimeException("Username already exists");
            }

            String memberId = get_new_member_id();
            String passwordHash = passwordHasher.hash(password);

            Member member = new Member(memberId, username, passwordHash);
            userRepository.save(member);

            logger.info("Member registered successfully, memberId={}, username={}", memberId, username);

            return true;
        });
    }

    public String login(String username, String password) {
        logger.info("Login attempt for username={}", username);
        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!member.isActive()) {
            logger.warn("Login rejected: inactive member, memberId={}, username={}", member.getMemberId(), username);
            throw new RuntimeException("Member is inactive");
        }

        if (!passwordHasher.matches(password, member.getPasswordHash())) {
            logger.warn("Login rejected: password mismatch for username={}", username);
            throw new RuntimeException("Invalid username or password");
        }

        member.login(); // Mark the member as logged in
        userRepository.save(member);

        AuthToken token = authTokenService.issueToken(member.getMemberId());
        tokenRepository.save(token);

        logger.info("Login successful, memberId={}, username={}", member.getMemberId(), username);

        return token.getTokenValue();
    }

    @Transactional
    public void logout(String tokenValue) {
        logger.info("Logout request received");
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("Logout rejected: token is empty");
            throw new RuntimeException("Token cannot be empty");
        }

        AuthToken to_logout = tokenRepository.findById(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        Member member = userRepository.findById(to_logout.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (getAllTokensForMember(member.getMemberId()).size() == 1) {
            member.logout(); // Mark the member as logged out
        }
        userRepository.save(member);
        tokenRepository.deleteByTokenValue(tokenValue);
        logger.info("Logout successful, memberId={}", member.getMemberId());
    }

    // ===================================================================================================================================
    // HELPER METHODS
    // ===================================================================================================================================
    public Member getMemberById(String targetMemberId) {
        logger.debug("Fetching member by id={}", targetMemberId);
        return userRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public void validateCompanyRoleRequest(CompanyRoleAssignment assignment) {
        logger.debug("Validating company role request for companyId={}", assignment.getCompanyId());
        if (assignment.getCompanyId() <=0) {
            logger.warn("Company role validation failed: missing company id");
            throw new RuntimeException("Company id cannot be empty");
        }

        if (assignment.getRoleType() == null) {
            logger.warn("Company role validation failed: missing role type");
            throw new RuntimeException("Role type is required");
        }

        if ((assignment.getRoleType() == CompanyRoleType.OWNER
                || assignment.getRoleType() == CompanyRoleType.MANAGER)
                && (assignment.getAppointedByMemberId() == null
                        || assignment.getAppointedByMemberId().isBlank())) {
            logger.warn("Company role validation failed: appointed by member id is required for roleType={}",
                    assignment.getRoleType());
            throw new RuntimeException("Appointed by member id is required");
        }

        logger.debug("Company role request validation passed for companyId={}", assignment.getCompanyId());
    }

    public void forgotPassword(String email) {
        logger.info("Forgot password requested for email={}", email);
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No member found with this email"));

        verificationService.createAndSendPasswordResetCode(member);
        userRepository.save(member);
        logger.info("Password reset code generated and sent, memberId={}", member.getMemberId());
    }

    public void resetPassword(String email, String code, String newPassword) {
        logger.info("Reset password request received for email={}", email);
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No member found with this email"));

        verificationService.resetPassword(member, code, newPassword, passwordHasher);
        userRepository.save(member);
        logger.info("Password reset completed successfully, memberId={}", member.getMemberId());
    }

    public Member getMemberByToken(String tokenValue) {
        logger.debug("Get member by token requested");
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.warn("Get member by token rejected: token is empty");
            throw new RuntimeException("Invalid token");
        }

        AuthToken token = tokenRepository.findById(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isExpired(java.time.LocalDateTime.now())) {
            tokenRepository.deleteByTokenValue(tokenValue);
            logger.warn("Token lookup failed: token expired for memberId={}", token.getMemberId());
            throw new RuntimeException("Token expired");
        }

        logger.debug("Token resolved to memberId={}", token.getMemberId());
        return userRepository.findById(token.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public boolean isTokenValid(String tokenValue) {
        logger.debug("Token validation requested");
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.debug("Token validation result=false because token is empty");
            return false;
        }

        AuthToken token = tokenRepository.findById(tokenValue).orElse(null);
        if (token == null) {
            logger.debug("Token validation result=false because token was not found");
            return false;
        }

        if (token.isExpired(java.time.LocalDateTime.now())) {
            tokenRepository.deleteByTokenValue(tokenValue);
            logger.debug("Token validation result=false because token expired, memberId={}", token.getMemberId());
            return false;
        }

        logger.debug("Token validation result=true for memberId={}", token.getMemberId());
        return true;
    }

    private String get_new_member_id() {
        logger.debug("Generating new member id");
        String newId;
        do {
            newId = UUID.randomUUID().toString();
        } while (userRepository.findById(newId).isPresent());
        logger.debug("Generated member id={}", newId);
        return newId;
    }

    private boolean validatePassword(String password) {
        if (password == null || password.length() < 6) {
            logger.warn("Password validation failed: below minimum length");
            throw new RuntimeException("Password must contain at least 6 characters");
        }
        logger.debug("Password validation passed");
        return true;
    }

    private boolean validateUsername(String username) {
        if (username == null || username.isBlank()) {
            logger.warn("Username validation failed: empty username");
            throw new RuntimeException("Username cannot be empty");
        }
        if (username.length() < 3) {
            logger.warn("Username validation failed: username too short, username={}", username);
            throw new RuntimeException("Username must contain at least 3 characters");
        }
        logger.debug("Username validation passed for username={}", username);
        return true;

    }

    // TODO: need to test
    public String register(String username,
            String password,
            String email,
            String phone,
            VerificationMethod verificationMethod) {
        logger.info("Register with verification requested for username={}, method={}", username, verificationMethod);
        //////// validation/////////////////////////
        if (!validateUsername(username) ||
                !validatePassword(password) ||
                !validateEmail(email) ||
                !validateVerificationTarget(email, phone, verificationMethod)) {
            logger.warn("Register with verification rejected: invalid input data for username={}", username);
            throw new RuntimeException("Invalid input data");
        }

        return keyedLock.callLocked(LOCK_NS_USERNAME, username, () -> {
            if (userRepository.existsByUsername(username)) {
                logger.warn("Register with verification rejected: username already exists, username={}", username);
                throw new RuntimeException("Username already exists");
            }

            String memberId = UUID.randomUUID().toString();
            String passwordHash = passwordHasher.hash(password);

            Member member = new Member(memberId, username, passwordHash);
            member.setEmail(email);
            member.setPhone(phone);
            member.setVerified(false);
            member.logout();

            verificationService.createAndSendCode(member, verificationMethod);
            userRepository.save(member);

            logger.info("Member registered pending verification, memberId={}, username={}", memberId, username);

            return memberId;
        });
    }

    public void verifyAccount(String username, String code) {
        logger.info("Account verification requested for username={}", username);
        if (username == null || username.isBlank()) {
            logger.warn("Account verification rejected: missing username");
            throw new RuntimeException("Username is required");
        }

        if (code == null || code.isBlank()) {
            logger.warn("Account verification rejected: missing code for username={}", username);
            throw new RuntimeException("Verification code is required");
        }

        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        verificationService.verifyCode(member, code);
        member.setVerified(true);
        userRepository.save(member);
        logger.info("Account verification completed for memberId={}, username={}", member.getMemberId(), username);
    }

    private boolean validateVerificationTarget(String email,
            String phone,
            VerificationMethod method) {
        if (method == null) {
            logger.warn("Verification target validation failed: method is required");
            throw new RuntimeException("Verification method is required");
        }

        if (method == VerificationMethod.EMAIL) {
            if (email == null || email.isBlank()) {
                logger.warn("Verification target validation failed: missing email for EMAIL method");
                throw new RuntimeException("Email is required for email verification");
            }
        }

        // if (method == VerificationMethod.EMAIL) {
        // if (phone == null || phone.isBlank()) {
        // throw new RuntimeException("Phone is required for phone verification");
        // }
        // }
        logger.debug("Verification target validation passed for method={}", method);
        return true;
    }

    private boolean validateEmail(String email) {
        // Simple regex for email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        boolean valid = email != null && email.matches(emailRegex);
        if (!valid) {
            logger.warn("Email validation failed");
        } else {
            logger.debug("Email validation passed");
        }
        return valid;
    }

    private boolean validatePhone(String phone) {
        // Simple regex for phone number validation (10 digits)
        if (phone == null) {
            logger.warn("Phone validation failed: phone is null");
            return false;
        }
        Pattern ISRAEL_PHONE_PATTERN = Pattern.compile(
                "^(?:0(?:2|3|4|8|9)\\d{7}|0(?:5\\d|7[2-9])\\d{7}|(?:\\+972|972)(?:2|3|4|8|9)\\d{7}|(?:\\+972|972)(?:5\\d|7[2-9])\\d{7})$");

        String normalized = phone.replaceAll("[\\s-]", "");
        boolean valid = ISRAEL_PHONE_PATTERN.matcher(normalized).matches();
        if (!valid) {
            logger.warn("Phone validation failed for value={}", normalized);
        } else {
            logger.debug("Phone validation passed");
        }

        return valid;
    }

    public Member getMemberByUsername(String username) {
        logger.debug("Fetching member by username={}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    private List<AuthToken> getAllTokensForMember(String memberId) {
        return tokenRepository.findAllByMemberId(memberId);
    }

    public ProfileResponse getMyProfile(String tokenValue) {
        Member member = getMemberByToken(tokenValue);

        return new ProfileResponse(
                member.getMemberId(),
                member.getUsername(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhone(),
                member.getAddress(),
                member.getCity(),
                member.getCountry(),
                member.getBirthDate(),
                member.getRole().name(),
                member.isActive(),
                member.isLoggedin(),
                member.isVerified());
    }

    public ProfileResponse updateMyProfile(String tokenValue, UpdateProfileRequest request) {
        Member resolved = getMemberByToken(tokenValue);

        Member member = keyedLock.callLocked(LOCK_NS_MEMBER, resolved.getMemberId(), () -> {
            // Re-read inside the lock so concurrent updates don't overwrite each other
            Member fresh = userRepository.findById(resolved.getMemberId())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            fresh.setFirstName(request.getFirstName());
            fresh.setLastName(request.getLastName());
            fresh.setEmail(request.getEmail());
            fresh.setPhone(request.getPhone());
            fresh.setAddress(request.getAddress());
            fresh.setCity(request.getCity());
            fresh.setCountry(request.getCountry());
            fresh.setBirthDate(request.getBirthDate());
            userRepository.save(fresh);
            return fresh;
        });

        return new ProfileResponse(
                member.getMemberId(),
                member.getUsername(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhone(),
                member.getAddress(),
                member.getCity(),
                member.getCountry(),
                member.getBirthDate(),
                member.getRole().name(),
                member.isActive(),
                member.isLoggedin(),
                member.isVerified());
    }

    public Member requireOwner(String tokenValue, String companyId) {
        Member member = getMemberByToken(tokenValue);

        if (!member.isOwnerInCompany(companyId)) {
            throw new RuntimeException("Owner permission required");
        }

        return member;
    }

    public Member requireManager(String tokenValue, String companyId) {
        Member member = getMemberByToken(tokenValue);

        if (!member.isManagerInCompany(companyId) && !member.isOwnerInCompany(companyId)) {
            throw new RuntimeException("Manager permission required");
        }

        return member;
    }

    public Member requireAdmin(String tokenValue) {
        return systemAdminService.requireAdmin(tokenValue);
    }
}
