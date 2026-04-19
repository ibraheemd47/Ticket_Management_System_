package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;

@Service
public class AuthTokenService {
    private static final Duration DEFAULT_TTL = Duration.ofHours(2);
    /**
     * Issues a token using the default expiration window.
     */
    public AuthToken issueToken(String memberId) {
        return issueToken(memberId, DEFAULT_TTL);
    }

    /**
     * Issues a token for a member with the provided time-to-live.
     */
    public AuthToken issueToken(String memberId, Duration ttl) {
        if (memberId == null || memberId.isBlank()) {
            throw new RuntimeException("Member ID is required");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new RuntimeException("TTL must be a positive duration");
        }

        LocalDateTime now = LocalDateTime.now();
        String tokenValue = UUID.randomUUID().toString();
        return new AuthToken(tokenValue, memberId, now.plus(ttl));
    }

    /**
     * Checks token validity against the current system time.
     */
    public boolean isTokenValid(AuthToken token) {
        return isTokenValid(token, LocalDateTime.now());
    }

    /**
     * Checks token validity against a specific time value.
     */
    private boolean isTokenValid(AuthToken token, LocalDateTime now) {
        if (token == null || now == null) {
            return false;
        }
        return !token.isExpired(now);
    }

}
