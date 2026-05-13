package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ProfileResponse;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.UpdateProfileRequest;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements IrepresnteUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    VerificationEmail verificationService;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthTokenService authTokenService;
    private final KeyedLock keyedLock;

    private static final String LOCK_NS_USERNAME = "user:username";
    private static final String LOCK_NS_MEMBER = "user:member";

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher,
            AuthTokenService authTokenService, VerificationEmail verificationService,
            KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.authTokenService = authTokenService;
        this.verificationService = verificationService;
        this.keyedLock = keyedLock;
    }


    public String login(String username, String password) {
        logger.info("Login attempt for username={}", username);

        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Login failed: username not found, username={}", username);
                    return new RuntimeException("Invalid username or password");
                });

        // Check if account is suspended
        if (member.isSuspended()) 
        {
            logger.warn("Login failed: account is suspended until {}, username={}, memberId={}", member.getSuspendedUntil(), username, member.getMemberId());
            throw new RuntimeException("Your account is suspended.");
        }
        if (!member.isActive()) {
            logger.error("Login failed: member is inactive, username={}, memberId={}", username, member.getMemberId());
            throw new RuntimeException("Member is inactive");
        }

        if (!member.isVerified()) {
            logger.error("Login failed: account is not verified, username={}, memberId={}", username, member.getMemberId());
            throw new RuntimeException("Account is not verified");
        }

        if (!passwordHasher.matches(password, member.getPasswordHash())) {
            logger.error("Login failed: invalid password, username={}, memberId={}", username, member.getMemberId());
            throw new RuntimeException("Invalid username or password");
        }

        member.login();
        userRepository.save(member);

        return authTokenService.generateToken(member.getUsername());
    }

    @Transactional
    public void logout(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.error("Logout failed: token is empty");
            throw new RuntimeException("Token cannot be empty");
        }

        Member member = getMemberByToken(tokenValue);
        member.logout();
        userRepository.save(member);

    }

    // ===================================================================================================================================
    // HELPER METHODS
    // ===================================================================================================================================
    public Member getMemberById(String targetMemberId) {
        logger.debug("Fetching member by id={}", targetMemberId);
        return userRepository.findByMemberId(targetMemberId);
    }

    public void validateCompanyRoleRequest(CompanyRoleAssignment assignment) {
        logger.debug("Validating company role request for companyId={}", assignment.getCompanyId());
        if (assignment.getCompanyId() <= 0) {
            logger.error("Company role validation failed: missing company id");
            throw new RuntimeException("Company id cannot be empty");
        }

        if (assignment.getRoleType() == null) {
            logger.error("Company role validation failed: missing role type");
            throw new RuntimeException("Role type is required");
        }

        if ((assignment.getRoleType() == CompanyRoleType.OWNER
                || assignment.getRoleType() == CompanyRoleType.MANAGER)
                && (assignment.getAppointedByMemberId() == null
                        || assignment.getAppointedByMemberId().isBlank())) {
            logger.error("Company role validation failed: appointed by member id is required for roleType={}",
                    assignment.getRoleType());
            throw new RuntimeException("Appointed by member id is required");
        }

        logger.debug("Company role request validation passed for companyId={}", assignment.getCompanyId());
    }

    public void forgotPassword(String email) {
        logger.info("Forgot password requested for email={}", email);
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Forgot password failed: no member found for email={}", email);
                    return new RuntimeException("No member found with this email");
                });

        verificationService.createAndSendPasswordResetCode(member);
        if (member != null) {
            userRepository.save(member);
            logger.info("Password reset code generated and sent, memberId={}", member.getMemberId());
        } else {
            logger.warn("Password reset failed: member not found for email={}", email);
        }
    }

    public void resetPassword(String email, String code, String newPassword) {
        logger.info("Reset password request received for email={}", email);
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Reset password failed: no member found for email={}", email);
                    return new RuntimeException("No member found with this email");
                });

        verificationService.resetPassword(member, code, newPassword, passwordHasher);
        if (member != null) {
            userRepository.save(member);
            logger.info("Password reset completed successfully, memberId={}", member.getMemberId());
        } else {
            logger.warn("Password reset failed: member not found for email={}", email);
        }
    }

    public Member getMemberByToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            logger.error("Token validation failed: token is empty");
            throw new RuntimeException("Invalid token");
        }

        if (!authTokenService.validateToken(tokenValue)) {
            logger.error("Token validation failed: invalid or expired token");
            throw new RuntimeException("Invalid or expired token");
        }

        String username = authTokenService.extractUsername(tokenValue);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public boolean isTokenValid(String tokenValue) {
        return authTokenService.validateToken(tokenValue);
    }

    private String get_new_member_id() {
        logger.debug("Generating new member id");
        String newId;
        do {
            newId = UUID.randomUUID().toString();
        } while (userRepository.findByMemberId(newId) != null);
        logger.debug("Generated member id={}", newId);
        return newId;
    }

    private boolean validatePassword(String password) {
        if (password == null || password.length() < 6) {
            logger.error("Password validation failed: below minimum length");
            throw new RuntimeException("Password must contain at least 6 characters");
        }
        logger.debug("Password validation passed");
        return true;
    }

    private boolean validateUsername(String username) {
        if (username == null || username.isBlank()) {
            logger.error("Username validation failed: empty username");
            throw new RuntimeException("Username cannot be empty");
        }
        if (username.length() < 3) {
            logger.error("Username validation failed: username too short, username={}", username);
            throw new RuntimeException("Username must contain at least 3 characters");
        }
        logger.debug("Username validation passed for username={}", username);
        return true;

    }

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
            logger.error("Register with verification rejected: invalid input data for username={}", username);
            throw new RuntimeException("Invalid input data");
        }

        return keyedLock.callLocked(LOCK_NS_USERNAME, username, () -> {
            if (userRepository.existsByUsername(username)) {
                logger.error("Register with verification rejected: username already exists, username={}", username);
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
            logger.error("Account verification rejected: missing username");
            throw new RuntimeException("Username is required");
        }

        if (code == null || code.isBlank()) {
            logger.error("Account verification rejected: missing code for username={}", username);
            throw new RuntimeException("Verification code is required");
        }

        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Account verification failed: member not found, username={}", username);
                    return new RuntimeException("Member not found");
                });

        verificationService.verifyCode(member, code);
        member.setVerified(true);
        userRepository.save(member);
        logger.info("Account verification completed for memberId={}, username={}", member.getMemberId(), username);
    }

    private boolean validateVerificationTarget(String email,
            String phone,
            VerificationMethod method) {
        if (method == null) {
            logger.error("Verification target validation failed: method is required");
            throw new RuntimeException("Verification method is required");
        }

        if (method == VerificationMethod.EMAIL) {
            if (email == null || email.isBlank()) {
                logger.error("Verification target validation failed: missing email for EMAIL method");
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

    // private boolean validatePhone(String phone) {
    // // Simple regex for phone number validation (10 digits)
    // if (phone == null) {
    // logger.warn("Phone validation failed: phone is null");
    // return false;
    // }
    // Pattern ISRAEL_PHONE_PATTERN = Pattern.compile(
    // "^(?:0(?:2|3|4|8|9)\\d{7}|0(?:5\\d|7[2-9])\\d{7}|(?:\\+972|972)(?:2|3|4|8|9)\\d{7}|(?:\\+972|972)(?:5\\d|7[2-9])\\d{7})$");

    // String normalized = phone.replaceAll("[\\s-]", "");
    // boolean valid = ISRAEL_PHONE_PATTERN.matcher(normalized).matches();
    // if (!valid) {
    // logger.warn("Phone validation failed for value={}", normalized);
    // } else {
    // logger.debug("Phone validation passed");
    // }

    // return valid;
    // }

    public Member getMemberByUsername(String username) {
        logger.debug("Fetching member by username={}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Member lookup failed: member not found, username={}", username);
                    return new RuntimeException("Member not found");
                });
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
            Member fresh = userRepository.findByMemberId(resolved.getMemberId());
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

    public Member requireOwner(String tokenValue, int companyId) {
        Member member = getMemberByToken(tokenValue);

        if (!member.isOwnerInCompany(companyId)) {
            logger.error("Owner permission required for companyId={}, memberId={}", companyId, member.getMemberId());
            throw new RuntimeException("Owner permission required");
        }

        return member;
    }

    public Member requireManager(String tokenValue, int companyId) {
        Member member = getMemberByToken(tokenValue);

        if (!member.isManagerInCompany(companyId) && !member.isOwnerInCompany(companyId)) {
            logger.error("Manager permission required for companyId={}, memberId={}", companyId, member.getMemberId());
            throw new RuntimeException("Manager permission required");
        }

        return member;
    }

    public Member requireAdmin(String tokenValue) {
        Member member = getMemberByToken(tokenValue);

        if (!member.isSystemAdmin()) {
            logger.error("Admin permission required for memberId={}", member.getMemberId());
            throw new RuntimeException("Admin permission required");
        }

        return member;
    }

    public String getMemberIdByToken(String tokenValue) {
        return getMemberByToken(tokenValue).getMemberId();
    }

    @Override
    public Member requireMember(String token) {
        return getMemberByToken(token);
    }

    @Override
    public String requireMemberId(String token) {
        return getMemberByToken(token).getMemberId();
    }

    @Override
    public boolean validateToken(String token) {
        return authTokenService.validateToken(token);
    }
}
