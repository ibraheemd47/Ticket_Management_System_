package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.time.LocalDateTime;
import java.util.Objects;

public class AuthToken {

        private final String tokenValue;
    private final String memberId;
    private final LocalDateTime expiresAt;

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
