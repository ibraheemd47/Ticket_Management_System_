package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ProfileResponse;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.UpdateProfileRequest;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Acceptance Tests")
class UserServiceAcceptanceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private VerificationEmail verificationService;

    private UserService userService;

    private static final String VALID_USERNAME = "alice";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_PHONE = "0501234567";
    private static final String MEMBER_ID = "m-001";
    private static final String TOKEN_VALUE = "jwt-token";
    private static final int COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordHasher,
                authTokenService,
                verificationService,
                new KeyedLock());
    }

    @Test
    @DisplayName("Given valid details, when user registers, then member is saved")
    void givenValidDetails_WhenUserRegisters_ThenMemberIsSaved() {
        when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
        when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("hashed-pw");

        String memberId = userService.register(
                VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

        assertNotNull(memberId);
        verify(userRepository).save(any(Member.class));
        verify(verificationService).createAndSendCode(any(Member.class), eq(VerificationMethod.EMAIL));
    }

    @Test
    @DisplayName("Given existing username, when user registers, then exception is thrown")
    void givenExistingUsername_WhenUserRegisters_ThenExceptionIsThrown() {
        when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(
                        VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given short password, when user registers, then exception is thrown")
    void givenShortPassword_WhenUserRegisters_ThenExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(
                        VALID_USERNAME, "123", VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL));

        assertEquals("Password must contain at least 6 characters", ex.getMessage());
    }

    @Test
    @DisplayName("Given invalid email, when user registers, then invalid input exception is thrown")
    void givenInvalidEmail_WhenUserRegisters_ThenInvalidInputExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(
                        VALID_USERNAME, VALID_PASSWORD, "bad-email", VALID_PHONE, VerificationMethod.EMAIL));

        assertEquals("Invalid input data", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid verification code, when verifying account, then member is verified")
    void givenValidVerificationCode_WhenVerifyingAccount_ThenMemberIsVerified() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        userService.verifyAccount(VALID_USERNAME, "123456");

        assertTrue(member.isVerified());
        verify(verificationService).verifyCode(member, "123456");
        verify(userRepository).save(member);
    }

    @Test
    @DisplayName("Given verified member and valid credentials, when logging in, then JWT token is returned")
    void givenVerifiedMemberAndValidCredentials_WhenLoggingIn_ThenJwtTokenIsReturned() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.setVerified(true);

        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
        when(passwordHasher.matches(VALID_PASSWORD, "hashed-pw")).thenReturn(true);
        when(authTokenService.generateToken(VALID_USERNAME)).thenReturn(TOKEN_VALUE);

        String result = userService.login(VALID_USERNAME, VALID_PASSWORD);

        assertEquals(TOKEN_VALUE, result);
        assertTrue(member.isLoggedin());
        verify(userRepository).save(member);
        verify(authTokenService).generateToken(VALID_USERNAME);
    }

    @Test
    @DisplayName("Given unverified member, when logging in, then account not verified exception is thrown")
    void givenUnverifiedMember_WhenLoggingIn_ThenAccountNotVerifiedExceptionIsThrown() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.setVerified(false);

        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login(VALID_USERNAME, VALID_PASSWORD));

        assertEquals("Account is not verified", ex.getMessage());
        verify(authTokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Given wrong password, when logging in, then invalid credentials exception is thrown")
    void givenWrongPassword_WhenLoggingIn_ThenInvalidCredentialsExceptionIsThrown() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.setVerified(true);

        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
        when(passwordHasher.matches("wrong", "hashed-pw")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login(VALID_USERNAME, "wrong"));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid token, when getting member by token, then member is returned")
    void givenValidToken_WhenGettingMemberByToken_ThenMemberIsReturned() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        Member result = userService.getMemberByToken(TOKEN_VALUE);

        assertEquals(member, result);
    }

    @Test
    @DisplayName("Given invalid token, when getting member by token, then exception is thrown")
    void givenInvalidToken_WhenGettingMemberByToken_ThenExceptionIsThrown() {
        when(authTokenService.validateToken("bad-token")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getMemberByToken("bad-token"));

        assertEquals("Invalid or expired token", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid token, when user logs out, then member is saved as logged out")
    void givenValidToken_WhenUserLogsOut_ThenMemberIsSavedAsLoggedOut() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.setVerified(true);
        member.login();

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        userService.logout(TOKEN_VALUE);

        assertFalse(member.isLoggedin());
        verify(userRepository).save(member);
    }

    @Test
    @DisplayName("Given valid token, when checking token validity, then true is returned")
    void givenValidToken_WhenCheckingTokenValidity_ThenTrueIsReturned() {
        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);

        assertTrue(userService.isTokenValid(TOKEN_VALUE));
    }

    @Test
    @DisplayName("Given registered email, when forgot password, then reset code is sent")
    void givenRegisteredEmail_WhenForgotPassword_ThenResetCodeIsSent() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

        userService.forgotPassword(VALID_EMAIL);

        verify(verificationService).createAndSendPasswordResetCode(member);
        verify(userRepository).save(member);
    }

    @Test
    @DisplayName("Given valid reset code, when resetting password, then member is saved")
    void givenValidResetCode_WhenResettingPassword_ThenMemberIsSaved() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

        userService.resetPassword(VALID_EMAIL, "123456", "newPassword123");

        verify(verificationService).resetPassword(member, "123456", "newPassword123", passwordHasher);
        verify(userRepository).save(member);
    }

    @Test
    @DisplayName("Given valid token, when getting profile, then profile response is returned")
    void givenValidToken_WhenGettingProfile_ThenProfileResponseIsReturned() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.setFirstName("Alice");
        member.setLastName("Cohen");
        member.setEmail(VALID_EMAIL);
        member.setPhone(VALID_PHONE);
        member.setVerified(true);

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        ProfileResponse response = userService.getMyProfile(TOKEN_VALUE);

        assertEquals(MEMBER_ID, response.getMemberId());
        assertEquals(VALID_USERNAME, response.getUsername());
        assertEquals("Alice", response.getFirstName());
    }

    @Test
    @DisplayName("Given valid token and update request, when updating profile, then profile is updated")
    void givenValidTokenAndUpdateRequest_WhenUpdatingProfile_ThenProfileIsUpdated() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Alice");
        request.setLastName("Levi");
        request.setEmail("new@example.com");
        request.setPhone("0507654321");
        request.setAddress("Street 5");
        request.setCity("Haifa");
        request.setCountry("Israel");
        request.setBirthDate(LocalDate.of(2000, 1, 1));

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
        when(userRepository.findByMemberId(MEMBER_ID)).thenReturn(member);

        ProfileResponse response = userService.updateMyProfile(TOKEN_VALUE, request);

        assertEquals("Alice", response.getFirstName());
        assertEquals("Levi", response.getLastName());
        assertEquals("new@example.com", response.getEmail());
        verify(userRepository).save(member);
    }

    @Test
    @DisplayName("Given owner member, when requireOwner is called, then member is returned")
    void givenOwnerMember_WhenRequireOwnerCalled_ThenMemberIsReturned() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, MEMBER_ID, CompanyRoleType.OWNER, Set.of()));

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        Member result = userService.requireOwner(TOKEN_VALUE, COMPANY_ID);

        assertEquals(member, result);
    }

    @Test
    @DisplayName("Given manager member, when requireManager is called, then member is returned")
    void givenManagerMember_WhenRequireManagerCalled_ThenMemberIsReturned() {
        Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
        member.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, MEMBER_ID, CompanyRoleType.MANAGER, Set.of()));

        when(authTokenService.validateToken(TOKEN_VALUE)).thenReturn(true);
        when(authTokenService.extractUsername(TOKEN_VALUE)).thenReturn(VALID_USERNAME);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

        Member result = userService.requireManager(TOKEN_VALUE, COMPANY_ID);

        assertEquals(member, result);
    }
}