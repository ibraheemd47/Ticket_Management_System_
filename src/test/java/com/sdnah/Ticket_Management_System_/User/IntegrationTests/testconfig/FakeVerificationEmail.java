package com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;

import java.time.LocalDateTime;

@Primary
@Component
public class FakeVerificationEmail extends VerificationEmail {

    public FakeVerificationEmail() {
        super(null); // no mail sender
    }

    @Override
    public void createAndSendCode(Member member, VerificationMethod method) {
        member.setVerificationCode("123456");
        member.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    @Override
    public void createAndSendPasswordResetCode(Member member) {
        member.setPasswordResetCode("123456");
        member.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
    }
}
