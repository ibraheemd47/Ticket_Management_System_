package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;

@Service
public class AuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    /**
     * Issues a token using the default expiration window.
     */
    public AuthToken issueToken(String memberId) {
        logger.debug("Issue token requested with default TTL, memberId={}", memberId);
        return issueToken(memberId, DEFAULT_TTL);
    }

    /**
     * Issues a token for a member with the provided time-to-live.
     */
    public AuthToken issueToken(String memberId, Duration ttl) {
        logger.info("Issue token request received, memberId={}, ttl={}", memberId, ttl);

        if (memberId == null || memberId.isBlank()) {
            logger.warn("Issue token rejected: memberId is empty");
            throw new RuntimeException("Member ID is required");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            logger.warn("Issue token rejected: invalid TTL, memberId={}, ttl={}", memberId, ttl);
            throw new RuntimeException("TTL must be a positive duration");
        }

        LocalDateTime now = LocalDateTime.now();
        String tokenValue = UUID.randomUUID().toString();
        AuthToken token = new AuthToken(tokenValue, memberId, now.plus(ttl));

        logger.info("Token issued successfully, memberId={}, expiresAt={}", memberId, token.getExpiresAt());
        return token;
    }

    /**
     * Checks token validity against the current system time.
     */
    public boolean isTokenValid(AuthToken token) {
        logger.debug("Token validity check requested against current time");
        return isTokenValid(token, LocalDateTime.now());
    }

    /**
     * Checks token validity against a specific time value.
     */
    private boolean isTokenValid(AuthToken token, LocalDateTime now) {
        if (token == null || now == null) {
            logger.warn("Token validity check result=false: token or now is null");
            return false;
        }

        boolean valid = !token.isExpired(now);
        logger.debug("Token validity check result={}, memberId={}, expiresAt={}",
                valid, token.getMemberId(), token.getExpiresAt());
        return valid;
    }
}