package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;

@Service
public class VerificationEmail {

    private static final int CODE_EXPIRE_MINUTES = 10;

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public VerificationEmail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void createAndSendCode(Member member, VerificationMethod verificationMethod) {
        if (member == null) {
            throw new RuntimeException("Member cannot be null");
        }

        if (verificationMethod == null) {
            throw new RuntimeException("Verification method is required");
        }

        if (verificationMethod != VerificationMethod.EMAIL) {
            throw new RuntimeException("Only EMAIL verification is supported");
        }

        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new RuntimeException("Member email is missing");
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES);

        member.setVerificationCode(code);
        member.setVerificationCodeExpiresAt(expiresAt);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(member.getEmail());
        message.setSubject("Verify your account");
        message.setText(
                "Hello " + member.getUsername() + ",\n\n" +
                "Your verification code is: " + code + "\n" +
                "This code will expire in " + CODE_EXPIRE_MINUTES + " minutes.\n\n" +
                "If you did not request this, please ignore this email.");

        mailSender.send(message);
    }

    public void verifyCode(Member member, String code) {
        if (member == null) {
            throw new RuntimeException("Member cannot be null");
        }

        if (code == null || code.isBlank()) {
            throw new RuntimeException("Verification code cannot be empty");
        }

        if (member.getVerificationCode() == null || member.getVerificationCode().isBlank()) {
            throw new RuntimeException("No verification code was generated for this member");
        }

        if (member.getVerificationCodeExpiresAt() == null) {
            throw new RuntimeException("Verification code expiration is missing");
        }

        if (LocalDateTime.now().isAfter(member.getVerificationCodeExpiresAt())) {
            throw new RuntimeException("Verification code has expired");
        }

        if (!member.getVerificationCode().equals(code.trim())) {
            throw new RuntimeException("Invalid verification code");
        }

        member.setVerified(true);
        member.setVerificationCode(null);
        member.setVerificationCodeExpiresAt(null);
    }

    private String generateCode() {
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }
}