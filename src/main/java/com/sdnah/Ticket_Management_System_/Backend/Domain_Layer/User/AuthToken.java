package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    private  String tokenValue;
    private  String memberId;
    private  LocalDateTime expiresAt;
    protected AuthToken() {
        // required by JPA
    }

    public AuthToken(String tokenValue, String memberId, LocalDateTime expiresAt) {
        this.tokenValue = Objects.requireNonNull(tokenValue);
        this.memberId = Objects.requireNonNull(memberId);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
