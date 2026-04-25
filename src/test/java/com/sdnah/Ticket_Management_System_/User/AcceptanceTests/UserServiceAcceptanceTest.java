package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Acceptance Tests")
class UserServiceAcceptanceTest {

    // -------------------------------------------------------------------------
    // Mocked ports (infrastructure + external services)
    // -------------------------------------------------------------------------
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private VerificationEmail verificationService;
    @Mock
    private SystemAdminService systemAdminService;

    private UserService userService;

    // -------------------------------------------------------------------------
    // Shared test fixtures
    // -------------------------------------------------------------------------
    private static final String VALID_USERNAME = "alice";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_PHONE = "0501234567";
    private static final String MEMBER_ID = "m-001";
    private static final String TOKEN_VALUE = "token-abc-123";

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordHasher,
                tokenRepository,
                authTokenService,
                verificationService,
                systemAdminService);
    }

    // =========================================================================
    // UC II.1.3 — Registration with email/phone verification
    // =========================================================================
    @Nested
    @DisplayName("UC II.1.3 — Registration with verification")
    class RegistrationWithVerificationTests {

        // --- Main scenario ---

        @Test
        @DisplayName("Given valid details and EMAIL method, when user registers, then a memberId is returned")
        void givenValidDetailsAndEmailMethod_WhenUserRegisters_ThenMemberIdIsReturned() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("hashed-pw");

            // Act
            String memberId = userService.register(
                    VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

            // Assert
            assertNotNull(memberId);
            assertFalse(memberId.isBlank());
        }

        @Test
        @DisplayName("Given valid details, when user registers, then member is saved to the system")
        void givenValidDetails_WhenUserRegisters_ThenMemberIsSavedToSystem() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("hashed-pw");

            // Act
            userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

            // Assert
            verify(userRepository, times(1)).save(any(Member.class));
        }

        @Test
        @DisplayName("Given valid details, when user registers, then a verification code is sent")
        void givenValidDetails_WhenUserRegisters_ThenVerificationCodeIsSent() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("hashed-pw");

            // Act
            userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

            // Assert — the system sent a verification code to the user
            verify(verificationService, times(1)).createAndSendCode(any(Member.class), eq(VerificationMethod.EMAIL));
        }

        @Test
        @DisplayName("Given valid details, when user registers, then the account is NOT yet verified")
        void givenValidDetails_WhenUserRegisters_ThenAccountIsNotYetVerified() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("hashed-pw");

            // Act
            userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

            // Assert — capture the saved member and confirm they are not verified
            var captor = org.mockito.ArgumentCaptor.forClass(Member.class);
            verify(userRepository).save(captor.capture());
            assertFalse(captor.getValue().isVerified());
        }

        @Test
        @DisplayName("Given valid details, when user registers, then the stored password is not plain text")
        void givenValidDetails_WhenUserRegisters_ThenStoredPasswordIsNotPlainText() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("$2a$hashed");

            // Act
            userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, VerificationMethod.EMAIL);

            // Assert
            var captor = org.mockito.ArgumentCaptor.forClass(Member.class);
            verify(userRepository).save(captor.capture());
            assertNotEquals(VALID_PASSWORD, captor.getValue().getPasswordHash());
        }

        // --- Alternative flow: username already taken ---

        @Test
        @DisplayName("Given an already-taken username, when user tries to register, then an exception is thrown")
        void givenAlreadyTakenUsername_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE,
                            VerificationMethod.EMAIL));

            assertEquals("Username already exists", ex.getMessage());
        }

        @Test
        @DisplayName("Given an already-taken username, when registration fails, then no member is saved")
        void givenAlreadyTakenUsername_WhenRegistrationFails_ThenNoMemberIsSaved() {
            // Arrange
            when(userRepository.existsByUsername(VALID_USERNAME)).thenReturn(true);

            // Act
            try {
                userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE,
                        VerificationMethod.EMAIL);
            } catch (RuntimeException ignored) {
            }

            // Assert
            verify(userRepository, never()).save(any());
        }

        // --- Alternative flow: invalid username ---

        @Test
        @DisplayName("Given a blank username, when user tries to register, then an exception is thrown")
        void givenBlankUsername_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            assertThrows(RuntimeException.class,
                    () -> userService.register("   ", VALID_PASSWORD, VALID_EMAIL, VALID_PHONE,
                            VerificationMethod.EMAIL));
        }

        @Test
        @DisplayName("Given a username shorter than 3 characters, when user tries to register, then an exception is thrown")
        void givenShortUsername_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.register("ab", VALID_PASSWORD, VALID_EMAIL, VALID_PHONE,
                            VerificationMethod.EMAIL));

            assertEquals("Username must contain at least 3 characters", ex.getMessage());
        }

        // --- Alternative flow: invalid password ---

        @Test
        @DisplayName("Given a password shorter than 6 characters, when user tries to register, then an exception is thrown")
        void givenShortPassword_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.register(VALID_USERNAME, "123", VALID_EMAIL, VALID_PHONE,
                            VerificationMethod.EMAIL));

            assertEquals("Password must contain at least 6 characters", ex.getMessage());
        }

        // --- Alternative flow: invalid email ---

        @Test
        @DisplayName("Given an email with no @ symbol, when user tries to register, then an exception is thrown")
        void givenEmailWithoutAtSymbol_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.register(VALID_USERNAME, VALID_PASSWORD, "notAnEmail", VALID_PHONE,
                            VerificationMethod.EMAIL));

            assertEquals("Invalid input data", ex.getMessage());
        }

        @Test
        @DisplayName("Given a null email, when user tries to register, then an exception is thrown")
        void givenNullEmail_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            assertThrows(RuntimeException.class,
                    () -> userService.register(VALID_USERNAME, VALID_PASSWORD, null, VALID_PHONE,
                            VerificationMethod.EMAIL));
        }

        // --- Alternative flow: invalid verification method ---

        @Test
        @DisplayName("Given a null verification method, when user tries to register, then an exception is thrown")
        void givenNullVerificationMethod_WhenUserTriesToRegister_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.register(VALID_USERNAME, VALID_PASSWORD, VALID_EMAIL, VALID_PHONE, null));

            assertEquals("Verification method is required", ex.getMessage());
        }
    }

    // =========================================================================
    // UC II.1.3b — Account verification (verifyAccount)
    // =========================================================================
    @Nested
    @DisplayName("UC II.1.3b — Account verification")
    class AccountVerificationTests {

        // --- Main scenario ---

        @Test
        @DisplayName("Given a registered user and a correct code, when they verify their account, then the account becomes verified")
        void givenRegisteredUserAndCorrectCode_WhenTheyVerifyAccount_ThenAccountBecomesVerified() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            // verifyCode succeeds (no exception thrown)

            // Act
            userService.verifyAccount(VALID_USERNAME, "CODE-123");

            // Assert — member is marked verified and saved
            assertTrue(member.isVerified());
            verify(userRepository).save(member);
        }

        @Test
        @DisplayName("Given a registered user and a correct code, when they verify, then verifyCode is called on the verification service")
        void givenRegisteredUserAndCorrectCode_WhenTheyVerify_ThenVerifyCodeIsCalledOnVerificationService() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));

            // Act
            userService.verifyAccount(VALID_USERNAME, "CODE-123");

            // Assert
            verify(verificationService, times(1)).verifyCode(eq(member), eq("CODE-123"));
        }

        // --- Alternative flow: invalid input ---

        @Test
        @DisplayName("Given a null username, when verifyAccount is called, then an exception is thrown")
        void givenNullUsername_WhenVerifyAccountCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount(null, "CODE-123"));

            assertEquals("Username is required", ex.getMessage());
        }

        @Test
        @DisplayName("Given a blank username, when verifyAccount is called, then an exception is thrown")
        void givenBlankUsername_WhenVerifyAccountCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount("   ", "CODE-123"));

            assertEquals("Username is required", ex.getMessage());
        }

        @Test
        @DisplayName("Given a null code, when verifyAccount is called, then an exception is thrown")
        void givenNullCode_WhenVerifyAccountCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount(VALID_USERNAME, null));

            assertEquals("Verification code is required", ex.getMessage());
        }

        @Test
        @DisplayName("Given a blank code, when verifyAccount is called, then an exception is thrown")
        void givenBlankCode_WhenVerifyAccountCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount(VALID_USERNAME, "   "));

            assertEquals("Verification code is required", ex.getMessage());
        }

        @Test
        @DisplayName("Given a username that does not exist, when verifyAccount is called, then an exception is thrown")
        void givenUsernameNotInSystem_WhenVerifyAccountCalled_ThenExceptionIsThrown() {
            // Arrange
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount("ghost", "CODE-123"));

            assertEquals("Member not found", ex.getMessage());
        }

        @Test
        @DisplayName("Given a wrong verification code, when verifyAccount is called, then an exception propagates from the verification service")
        void givenWrongCode_WhenVerifyAccountCalled_ThenExceptionPropagates() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            doThrow(new RuntimeException("Invalid verification code"))
                    .when(verificationService).verifyCode(any(), eq("WRONG-CODE"));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.verifyAccount(VALID_USERNAME, "WRONG-CODE"));

            assertEquals("Invalid verification code", ex.getMessage());
        }

        @Test
        @DisplayName("Given a wrong code, when verification fails, then the member is NOT marked as verified")
        void givenWrongCode_WhenVerificationFails_ThenMemberIsNotMarkedVerified() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            doThrow(new RuntimeException("Invalid verification code"))
                    .when(verificationService).verifyCode(any(), eq("WRONG-CODE"));

            // Act
            try {
                userService.verifyAccount(VALID_USERNAME, "WRONG-CODE");
            } catch (RuntimeException ignored) {
            }

            // Assert
            assertFalse(member.isVerified());
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC II.1.4 — Login
    // =========================================================================
    @Nested
    @DisplayName("UC II.1.4 — Login")
    class LoginTests {

        @Test
        @DisplayName("Given valid credentials, when user logs in, then a non-null token is returned")
        void givenValidCredentials_WhenUserLogsIn_ThenNonNullTokenIsReturned() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            when(passwordHasher.matches(VALID_PASSWORD, "hashed-pw")).thenReturn(true);
            when(authTokenService.issueToken(MEMBER_ID)).thenReturn(token);

            // Act
            String result = userService.login(VALID_USERNAME, VALID_PASSWORD);

            // Assert
            assertNotNull(result);
            assertEquals(TOKEN_VALUE, result);
        }

        @Test
        @DisplayName("Given valid credentials, when user logs in, then the token is persisted in the system")
        void givenValidCredentials_WhenUserLogsIn_ThenTokenIsPersistedInSystem() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            when(passwordHasher.matches(VALID_PASSWORD, "hashed-pw")).thenReturn(true);
            when(authTokenService.issueToken(MEMBER_ID)).thenReturn(token);

            // Act
            userService.login(VALID_USERNAME, VALID_PASSWORD);

            // Assert
            verify(tokenRepository, times(1)).save(token);
        }

        @Test
        @DisplayName("Given valid credentials, when user logs in, then they are marked as logged in")
        void givenValidCredentials_WhenUserLogsIn_ThenMemberIsMarkedLoggedIn() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            member.logout(); // start logged out
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            when(passwordHasher.matches(VALID_PASSWORD, "hashed-pw")).thenReturn(true);
            when(authTokenService.issueToken(MEMBER_ID)).thenReturn(token);

            // Act
            userService.login(VALID_USERNAME, VALID_PASSWORD);

            // Assert
            assertTrue(member.isLoggedin());
        }

        // --- Alternative flow: incorrect credentials ---

        @Test
        @DisplayName("Given an unknown username, when user tries to log in, then an exception is thrown")
        void givenUnknownUsername_WhenUserTriesToLogIn_ThenExceptionIsThrown() {
            // Arrange
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.login("ghost", VALID_PASSWORD));

            assertEquals("Invalid username or password", ex.getMessage());
        }

        @Test
        @DisplayName("Given a wrong password, when user tries to log in, then an exception is thrown")
        void givenWrongPassword_WhenUserTriesToLogIn_ThenExceptionIsThrown() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(member));
            when(passwordHasher.matches("wrong", "hashed-pw")).thenReturn(false);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.login(VALID_USERNAME, "wrong"));

            assertEquals("Invalid username or password", ex.getMessage());
        }

        @Test
        @DisplayName("Given wrong credentials, when login fails, then no token is issued or stored")
        void givenWrongCredentials_WhenLoginFails_ThenNoTokenIsIssuedOrStored() {
            // Arrange
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.empty());

            // Act
            try {
                userService.login(VALID_USERNAME, VALID_PASSWORD);
            } catch (RuntimeException ignored) {
            }

            // Assert
            verify(tokenRepository, never()).save(any());
            verify(authTokenService, never()).issueToken(any());
        }

        // --- Alternative flow: account cancelled by System Administrator ---

        @Test
        @DisplayName("Given an account deactivated by a system admin, when user tries to log in, then an exception is thrown")
        void givenDeactivatedAccount_WhenUserTriesToLogIn_ThenExceptionIsThrown() {
            // Arrange — simulate an account that a system admin has cancelled
            Member inactiveMember = mock(Member.class);
            when(inactiveMember.isActive()).thenReturn(false);
            when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(inactiveMember));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.login(VALID_USERNAME, VALID_PASSWORD));

            assertEquals("Member is inactive", ex.getMessage());
            verify(tokenRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC II.3.1 — Logout
    // =========================================================================
    @Nested
    @DisplayName("UC II.3.1 — Logout")
    class LogoutTests {

        @Test
        @DisplayName("Given a logged-in user with one active session, when they log out, then they are marked as logged out")
        void givenLoggedInUserWithOneSession_WhenTheyLogOut_ThenMarkedAsLoggedOut() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            // Only one session active — returning this one token signals it's the last
            when(tokenRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(token));

            // Act
            userService.logout(TOKEN_VALUE);

            // Assert
            assertFalse(member.isLoggedin());
        }

        @Test
        @DisplayName("Given a user with two active sessions, when they log out of one, then they remain logged in")
        void givenUserWithTwoSessions_WhenTheyLogOutOfOne_ThenTheyRemainLoggedIn() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token1 = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            AuthToken token2 = new AuthToken("token-second", MEMBER_ID, LocalDateTime.now().plusHours(2));
            member.setLoggedin(true); // user is logged in
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token1));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            // Two sessions still active
            when(tokenRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(token1, token2));

            // Act
            userService.logout(TOKEN_VALUE);

            // Assert — member still logged in because another session exists
            assertTrue(member.isLoggedin());
        }

        @Test
        @DisplayName("Given a logged-in user, when they log out, then the token is deleted from the system")
        void givenLoggedInUser_WhenTheyLogOut_ThenTokenIsDeletedFromSystem() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(tokenRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(token));

            // Act
            userService.logout(TOKEN_VALUE);

            // Assert
            verify(tokenRepository, times(1)).deleteByTokenValue(TOKEN_VALUE);
        }

        // --- Alternative flow: invalid token ---

        @Test
        @DisplayName("Given a blank token, when logout is called, then an exception is thrown")
        void givenBlankToken_WhenLogoutCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.logout("   "));

            assertEquals("Token cannot be empty", ex.getMessage());
        }

        @Test
        @DisplayName("Given a null token, when logout is called, then an exception is thrown")
        void givenNullToken_WhenLogoutCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            assertThrows(RuntimeException.class, () -> userService.logout(null));
        }

        @Test
        @DisplayName("Given a token that does not exist, when logout is called, then an exception is thrown")
        void givenNonExistentToken_WhenLogoutCalled_ThenExceptionIsThrown() {
            // Arrange
            when(tokenRepository.findById("bad-token")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.logout("bad-token"));

            assertEquals("Invalid token", ex.getMessage());
        }
    }

    // =========================================================================
    // UC — Forgot password / Reset password
    // =========================================================================
    @Nested
    @DisplayName("UC — Forgot password and Reset password")
    class ForgotAndResetPasswordTests {

        // --- forgotPassword: main scenario ---

        @Test
        @DisplayName("Given a registered email, when forgotPassword is called, then a reset code is sent")
        void givenRegisteredEmail_WhenForgotPasswordCalled_ThenResetCodeIsSent() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

            // Act
            userService.forgotPassword(VALID_EMAIL);

            // Assert
            verify(verificationService, times(1)).createAndSendPasswordResetCode(member);
        }

        @Test
        @DisplayName("Given a registered email, when forgotPassword is called, then the member is saved after code generation")
        void givenRegisteredEmail_WhenForgotPasswordCalled_ThenMemberIsSaved() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

            // Act
            userService.forgotPassword(VALID_EMAIL);

            // Assert
            verify(userRepository).save(member);
        }

        // --- forgotPassword: alternative flow ---

        @Test
        @DisplayName("Given an email not registered in the system, when forgotPassword is called, then an exception is thrown")
        void givenUnregisteredEmail_WhenForgotPasswordCalled_ThenExceptionIsThrown() {
            // Arrange
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.forgotPassword("nobody@example.com"));

            assertEquals("No member found with this email", ex.getMessage());
        }

        @Test
        @DisplayName("Given an unregistered email, when forgotPassword fails, then no code is sent")
        void givenUnregisteredEmail_WhenForgotPasswordFails_ThenNoCodeIsSent() {
            // Arrange
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            // Act
            try {
                userService.forgotPassword("nobody@example.com");
            } catch (RuntimeException ignored) {
            }

            // Assert
            verify(verificationService, never()).createAndSendPasswordResetCode(any());
        }

        // --- resetPassword: main scenario ---

        @Test
        @DisplayName("Given a valid email and correct code, when resetPassword is called, then password is reset successfully")
        void givenValidEmailAndCorrectCode_WhenResetPasswordCalled_ThenPasswordIsResetSuccessfully() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

            // Act — no exception means success
            assertDoesNotThrow(() -> userService.resetPassword(VALID_EMAIL, "RESET-CODE", "newPassword123"));

            // Assert — member saved after reset
            verify(userRepository).save(member);
        }

        @Test
        @DisplayName("Given a valid email and correct code, when resetPassword is called, then the verification service handles the reset")
        void givenValidEmailAndCorrectCode_WhenResetPasswordCalled_ThenVerificationServiceHandlesReset() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));

            // Act
            userService.resetPassword(VALID_EMAIL, "RESET-CODE", "newPassword123");

            // Assert
            verify(verificationService, times(1))
                    .resetPassword(eq(member), eq("RESET-CODE"), eq("newPassword123"), any(PasswordHasher.class));
        }

        // --- resetPassword: alternative flow ---

        @Test
        @DisplayName("Given an email not in the system, when resetPassword is called, then an exception is thrown")
        void givenUnregisteredEmail_WhenResetPasswordCalled_ThenExceptionIsThrown() {
            // Arrange
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.resetPassword("nobody@example.com", "CODE", "newPass"));

            assertEquals("No member found with this email", ex.getMessage());
        }

        @Test
        @DisplayName("Given a correct email but a wrong code, when resetPassword is called, then the exception propagates")
        void givenCorrectEmailButWrongCode_WhenResetPasswordCalled_ThenExceptionPropagates() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(member));
            doThrow(new RuntimeException("Invalid reset code"))
                    .when(verificationService).resetPassword(any(), eq("BAD-CODE"), any(), any());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.resetPassword(VALID_EMAIL, "BAD-CODE", "newPass123"));

            assertEquals("Invalid reset code", ex.getMessage());
        }
    }

    // =========================================================================
    // UC — Get profile / Update profile
    // =========================================================================
    @Nested
    @DisplayName("UC — Get and Update profile")
    class ProfileTests {

        // --- getMyProfile: main scenario ---

        @Test
        @DisplayName("Given a valid session token, when user gets their profile, then a ProfileResponse is returned")
        void givenValidToken_WhenUserGetsTheirProfile_ThenProfileResponseIsReturned() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            member.setEmail(VALID_EMAIL);
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            // Act
            ProfileResponse profile = userService.getMyProfile(TOKEN_VALUE);

            // Assert
            assertNotNull(profile);
            assertEquals(MEMBER_ID, profile.getMemberId());
            assertEquals(VALID_USERNAME, profile.getUsername());
            assertEquals(VALID_EMAIL, profile.getEmail());
        }

        // --- getMyProfile: alternative flow ---

        @Test
        @DisplayName("Given an invalid token, when user tries to get their profile, then an exception is thrown")
        void givenInvalidToken_WhenUserTriesToGetProfile_ThenExceptionIsThrown() {
            // Arrange
            when(tokenRepository.findById("bad-token")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> userService.getMyProfile("bad-token"));
        }

        @Test
        @DisplayName("Given an expired token, when user tries to get their profile, then an exception is thrown and token is deleted")
        void givenExpiredToken_WhenUserTriesToGetProfile_ThenExceptionIsThrownAndTokenDeleted() {
            // Arrange
            AuthToken expired = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(expired));

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> userService.getMyProfile(TOKEN_VALUE));

            verify(tokenRepository).deleteByTokenValue(TOKEN_VALUE);
        }

        // --- updateMyProfile: main scenario ---

        @Test
        @DisplayName("Given a valid token and profile data, when user updates their profile, then updated ProfileResponse is returned")
        void givenValidTokenAndProfileData_WhenUserUpdatesProfile_ThenUpdatedProfileResponseIsReturned() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Alice");
            request.setLastName("Smith");
            request.setEmail("newalice@example.com");
            request.setPhone("0521234567");
            request.setAddress("123 Main St");
            request.setCity("Tel Aviv");
            request.setCountry("Israel");
            request.setBirthDate(LocalDate.of(1995, 6, 15));

            // Act
            ProfileResponse updated = userService.updateMyProfile(TOKEN_VALUE, request);

            // Assert
            assertNotNull(updated);
            assertEquals("Alice", updated.getFirstName());
            assertEquals("Smith", updated.getLastName());
            assertEquals("newalice@example.com", updated.getEmail());
            assertEquals("Tel Aviv", updated.getCity());
        }

        @Test
        @DisplayName("Given a valid token and profile data, when user updates their profile, then changes are persisted")
        void givenValidTokenAndProfileData_WhenUserUpdatesProfile_ThenChangesArePersisted() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Alice");
            request.setLastName("Smith");
            request.setEmail(VALID_EMAIL);
            request.setPhone(VALID_PHONE);
            request.setAddress("123 Main St");
            request.setCity("Tel Aviv");
            request.setCountry("Israel");
            request.setBirthDate(LocalDate.of(1995, 6, 15));

            // Act
            userService.updateMyProfile(TOKEN_VALUE, request);

            // Assert
            verify(userRepository).save(member);
            assertEquals("Alice", member.getFirstName());
            assertEquals("Smith", member.getLastName());
        }

        // --- updateMyProfile: alternative flow ---

        @Test
        @DisplayName("Given an invalid token, when user tries to update their profile, then an exception is thrown")
        void givenInvalidToken_WhenUserTriesToUpdateProfile_ThenExceptionIsThrown() {
            // Arrange
            when(tokenRepository.findById("bad-token")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> userService.updateMyProfile("bad-token", new UpdateProfileRequest()));
        }
    }

    // =========================================================================
    // UC — requireOwner / requireManager / requireAdmin (access guards)
    // =========================================================================
    @Nested
    @DisplayName("UC — Access control guards")
    class AccessControlTests {

        // --- requireOwner ---

        @Test
        @DisplayName("Given a token of a company owner, when requireOwner is called, then the member is returned")
        void givenOwnerToken_WhenRequireOwnerCalled_ThenMemberIsReturned() {
            // Arrange
            Member owner = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            owner.addCompanyRole(new CompanyRoleAssignment("company-1", MEMBER_ID, CompanyRoleType.OWNER, null));

            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(owner));

            // Act
            Member result = userService.requireOwner(TOKEN_VALUE, "company-1");

            // Assert
            assertEquals(owner, result);
        }

        @Test
        @DisplayName("Given a token of a non-owner member, when requireOwner is called, then an exception is thrown")
        void givenNonOwnerToken_WhenRequireOwnerCalled_ThenExceptionIsThrown() {
            // Arrange — member with no company role
            Member regularMember = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");

            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.requireOwner(TOKEN_VALUE, "company-1"));

            assertEquals("Owner permission required", ex.getMessage());
        }

        // --- requireManager ---

        @Test
        @DisplayName("Given a token of a company manager, when requireManager is called, then the member is returned")
        void givenManagerToken_WhenRequireManagerCalled_ThenMemberIsReturned() {
            // Arrange
            Member manager = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            manager.addCompanyRole(new CompanyRoleAssignment("company-1", "owner-id", CompanyRoleType.MANAGER, null));

            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(manager));

            // Act
            Member result = userService.requireManager(TOKEN_VALUE, "company-1");

            // Assert
            assertEquals(manager, result);
        }

        @Test
        @DisplayName("Given a token of a company owner, when requireManager is called, then the owner also qualifies")
        void givenOwnerToken_WhenRequireManagerCalled_ThenOwnerAlsoQualifies() {
            // Arrange — owners have at least manager-level access
            Member owner = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            owner.addCompanyRole(new CompanyRoleAssignment("company-1", MEMBER_ID, CompanyRoleType.OWNER, null));

            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(owner));

            // Act & Assert — no exception
            assertDoesNotThrow(() -> userService.requireManager(TOKEN_VALUE, "company-1"));
        }

        @Test
        @DisplayName("Given a token of a regular member, when requireManager is called, then an exception is thrown")
        void givenRegularMemberToken_WhenRequireManagerCalled_ThenExceptionIsThrown() {
            // Arrange
            Member regularMember = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");

            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.requireManager(TOKEN_VALUE, "company-1"));

            assertEquals("Manager permission required", ex.getMessage());
        }

        // --- requireAdmin ---

        @Test
        @DisplayName("Given a token of a system admin, when requireAdmin is called, then the admin member is returned")
        void givenAdminToken_WhenRequireAdminCalled_ThenAdminMemberIsReturned() {
            // Arrange
            Member adminMember = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            when(systemAdminService.requireAdmin(TOKEN_VALUE)).thenReturn(adminMember);

            // Act
            Member result = userService.requireAdmin(TOKEN_VALUE);

            // Assert
            assertEquals(adminMember, result);
        }

        @Test
        @DisplayName("Given a token of a non-admin member, when requireAdmin is called, then an exception propagates from the admin service")
        void givenNonAdminToken_WhenRequireAdminCalled_ThenExceptionPropagates() {
            // Arrange
            when(systemAdminService.requireAdmin(TOKEN_VALUE))
                    .thenThrow(new RuntimeException("Admin permission required"));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.requireAdmin(TOKEN_VALUE));

            assertEquals("Admin permission required", ex.getMessage());
        }
    }

    // =========================================================================
    // UC — getMemberByToken / isTokenValid (session helpers)
    // =========================================================================
    @Nested
    @DisplayName("UC — Session helpers (getMemberByToken, isTokenValid)")
    class SessionHelperTests {

        @Test
        @DisplayName("Given a valid non-expired token, when getMemberByToken is called, then the correct member is returned")
        void givenValidToken_WhenGetMemberByTokenCalled_ThenCorrectMemberIsReturned() {
            // Arrange
            Member member = new Member(MEMBER_ID, VALID_USERNAME, "hashed-pw");
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));

            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            // Act
            Member result = userService.getMemberByToken(TOKEN_VALUE);

            // Assert
            assertEquals(member, result);
        }

        @Test
        @DisplayName("Given an expired token, when getMemberByToken is called, then token is deleted and exception is thrown")
        void givenExpiredToken_WhenGetMemberByTokenCalled_ThenTokenDeletedAndExceptionThrown() {
            // Arrange
            AuthToken expired = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(expired));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.getMemberByToken(TOKEN_VALUE));

            assertEquals("Token expired", ex.getMessage());
            verify(tokenRepository).deleteByTokenValue(TOKEN_VALUE);
        }

        @Test
        @DisplayName("Given a blank token, when getMemberByToken is called, then an exception is thrown")
        void givenBlankToken_WhenGetMemberByTokenCalled_ThenExceptionIsThrown() {
            // Arrange + Act & Assert
            assertThrows(RuntimeException.class, () -> userService.getMemberByToken("   "));
        }

        @Test
        @DisplayName("Given a valid non-expired token, when isTokenValid is called, then returns true")
        void givenValidToken_WhenIsTokenValidCalled_ThenReturnsTrue() {
            // Arrange
            AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().plusHours(2));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(token));

            // Act
            boolean result = userService.isTokenValid(TOKEN_VALUE);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Given a blank token, when isTokenValid is called, then returns false without touching the repository")
        void givenBlankToken_WhenIsTokenValidCalled_ThenReturnsFalseWithoutTouchingRepository() {
            // Arrange + Act
            boolean result = userService.isTokenValid("   ");

            // Assert
            assertFalse(result);
            verify(tokenRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Given a token not stored in the system, when isTokenValid is called, then returns false")
        void givenTokenNotInSystem_WhenIsTokenValidCalled_ThenReturnsFalse() {
            // Arrange
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.empty());

            // Act
            boolean result = userService.isTokenValid(TOKEN_VALUE);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Given an expired token, when isTokenValid is called, then returns false and token is cleaned up")
        void givenExpiredToken_WhenIsTokenValidCalled_ThenReturnsFalseAndTokenCleanedUp() {
            // Arrange
            AuthToken expired = new AuthToken(TOKEN_VALUE, MEMBER_ID, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findById(TOKEN_VALUE)).thenReturn(Optional.of(expired));

            // Act
            boolean result = userService.isTokenValid(TOKEN_VALUE);

            // Assert
            assertFalse(result);
            verify(tokenRepository).deleteByTokenValue(TOKEN_VALUE);
        }
    }
}
