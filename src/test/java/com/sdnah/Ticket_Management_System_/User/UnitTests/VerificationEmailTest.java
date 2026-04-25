package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;

class VerificationEmailTest {

    private JavaMailSender mailSender;
    private VerificationEmail verificationEmail;
    private Member member;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        verificationEmail = new VerificationEmail(mailSender);

        member = new Member("member-1", "mostafa", "hashed-password");
        member.setEmail("mostafa@example.com");
    }

    @Test
    @DisplayName("Given valid member and email verification method, when creating verification code, then code and expiration are saved")
    void givenValidMemberAndEmailVerificationMethod_WhenCreatingVerificationCode_ThenCodeAndExpirationAreSaved() {
        // Arrange

        // Act
        verificationEmail.createAndSendCode(member, VerificationMethod.EMAIL);

        // Assert
        assertNotNull(member.getVerificationCode());
        assertFalse(member.getVerificationCode().isBlank());
        assertEquals(6, member.getVerificationCode().length());
        assertNotNull(member.getVerificationCodeExpiresAt());
        assertTrue(member.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now()));
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Given null member, when creating verification code, then member cannot be null exception is thrown")
    void givenNullMember_WhenCreatingVerificationCode_ThenMemberCannotBeNullExceptionIsThrown() {
        // Arrange + Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.createAndSendCode(null, VerificationMethod.EMAIL));

        // Assert
        assertEquals("Member cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Given null verification method, when creating verification code, then verification method required exception is thrown")
    void givenNullVerificationMethod_WhenCreatingVerificationCode_ThenVerificationMethodRequiredExceptionIsThrown() {
        // Arrange + Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.createAndSendCode(member, null));

        // Assert
        assertEquals("Verification method is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given member without email, when creating verification code, then member email missing exception is thrown")
    void givenMemberWithoutEmail_WhenCreatingVerificationCode_ThenMemberEmailMissingExceptionIsThrown() {
        // Arrange
        member.setEmail("   ");

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.createAndSendCode(member, VerificationMethod.EMAIL));

        // Assert
        assertEquals("Member email is missing", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid verification code, when verifying code, then member becomes verified and code is cleared")
    void givenValidVerificationCode_WhenVerifyingCode_ThenMemberBecomesVerifiedAndCodeIsCleared() {
        // Arrange
        member.setVerificationCode("123456");
        member.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        // Act
        verificationEmail.verifyCode(member, "123456");

        // Assert
        assertTrue(member.isVerified());
        assertNull(member.getVerificationCode());
        assertNull(member.getVerificationCodeExpiresAt());
    }

    @Test
    @DisplayName("Given null member, when verifying code, then member cannot be null exception is thrown")
    void givenNullMember_WhenVerifyingCode_ThenMemberCannotBeNullExceptionIsThrown() {
        // Arrange + Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyCode(null, "123456"));

        // Assert
        assertEquals("Member cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank code, when verifying code, then verification code empty exception is thrown")
    void givenBlankCode_WhenVerifyingCode_ThenVerificationCodeEmptyExceptionIsThrown() {
        // Arrange

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyCode(member, "   "));

        // Assert
        assertEquals("Verification code cannot be empty", ex.getMessage());
    }

    @Test
    @DisplayName("Given no generated verification code, when verifying code, then no code generated exception is thrown")
    void givenNoGeneratedVerificationCode_WhenVerifyingCode_ThenNoCodeGeneratedExceptionIsThrown() {
        // Arrange
        member.setVerificationCode(null);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyCode(member, "123456"));

        // Assert
        assertEquals("No verification code was generated for this member", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing verification code expiration, when verifying code, then expiration missing exception is thrown")
    void givenMissingVerificationCodeExpiration_WhenVerifyingCode_ThenExpirationMissingExceptionIsThrown() {
        // Arrange
        member.setVerificationCode("123456");
        member.setVerificationCodeExpiresAt(null);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyCode(member, "123456"));

        // Assert
        assertEquals("Verification code expiration is missing", ex.getMessage());
    }

    @Test
    @DisplayName("Given expired verification code, when verifying code, then code expired exception is thrown")
    void givenExpiredVerificationCode_WhenVerifyingCode_ThenCodeExpiredExceptionIsThrown() {
        // Arrange
        member.setVerificationCode("123456");
        member.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyCode(member, "123456"));

        // Assert
        assertEquals("Verification code has expired", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid member email, when creating password reset code, then reset code and expiration are saved")
    void givenValidMemberEmail_WhenCreatingPasswordResetCode_ThenResetCodeAndExpirationAreSaved() {
        // Arrange

        // Act
        verificationEmail.createAndSendPasswordResetCode(member);

        // Assert
        assertNotNull(member.getPasswordResetCode());
        assertEquals(6, member.getPasswordResetCode().length());
        assertNotNull(member.getPasswordResetCodeExpiresAt());
        assertTrue(member.getPasswordResetCodeExpiresAt().isAfter(LocalDateTime.now()));
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Given null member, when creating password reset code, then member cannot be null exception is thrown")
    void givenNullMember_WhenCreatingPasswordResetCode_ThenMemberCannotBeNullExceptionIsThrown() {
        // Arrange + Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.createAndSendPasswordResetCode(null));

        // Assert
        assertEquals("Member cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Given member without email, when creating password reset code, then member email missing exception is thrown")
    void givenMemberWithoutEmail_WhenCreatingPasswordResetCode_ThenMemberEmailMissingExceptionIsThrown() {
        // Arrange
        member.setEmail("");

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.createAndSendPasswordResetCode(member));

        // Assert
        assertEquals("Member email is missing", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid password reset code, when verifying reset code, then no exception is thrown")
    void givenValidPasswordResetCode_WhenVerifyingResetCode_ThenNoExceptionIsThrown() {
        // Arrange
        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        // Act + Assert
        assertDoesNotThrow(() -> verificationEmail.verifyPasswordResetCode(member, "123456"));
    }

    @Test
    @DisplayName("Given invalid password reset code, when verifying reset code, then invalid reset code exception is thrown")
    void givenInvalidPasswordResetCode_WhenVerifyingResetCode_ThenInvalidResetCodeExceptionIsThrown() {
        // Arrange
        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyPasswordResetCode(member, "000000"));

        // Assert
        assertEquals("Invalid password reset code", ex.getMessage());
    }

    @Test
    @DisplayName("Given expired password reset code, when verifying reset code, then reset code expired exception is thrown")
    void givenExpiredPasswordResetCode_WhenVerifyingResetCode_ThenResetCodeExpiredExceptionIsThrown() {
        // Arrange
        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.verifyPasswordResetCode(member, "123456"));

        // Assert
        assertEquals("Password reset code has expired", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid reset code and new password, when resetting password, then password hash is updated and reset code is cleared")
    void givenValidResetCodeAndNewPassword_WhenResettingPassword_ThenPasswordHashIsUpdatedAndResetCodeIsCleared() {
        // Arrange
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordHasher.hash("newPassword123")).thenReturn("new-hashed-password");

        // Act
        verificationEmail.resetPassword(member, "123456", "newPassword123", passwordHasher);

        // Assert
        assertEquals("new-hashed-password", member.getPasswordHash());
        assertNull(member.getPasswordResetCode());
        assertNull(member.getPasswordResetCodeExpiresAt());
        verify(passwordHasher, times(1)).hash("newPassword123");
    }

    @Test
    @DisplayName("Given null password hasher, when resetting password, then password hasher cannot be null exception is thrown")
    void givenNullPasswordHasher_WhenResettingPassword_ThenPasswordHasherCannotBeNullExceptionIsThrown() {
        // Arrange
        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.resetPassword(member, "123456", "newPassword123", null));

        // Assert
        assertEquals("PasswordHasher cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Given short new password, when resetting password, then password length exception is thrown")
    void givenShortNewPassword_WhenResettingPassword_ThenPasswordLengthExceptionIsThrown() {
        // Arrange
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> verificationEmail.resetPassword(member, "123456", "123", passwordHasher));

        // Assert
        assertEquals("Password must contain at least 6 characters", ex.getMessage());
        verify(passwordHasher, never()).hash(anyString());
    }
}
