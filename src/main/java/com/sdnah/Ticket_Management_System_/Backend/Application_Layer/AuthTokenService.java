package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.AuthToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Service
public class AuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

    private final SecretKey key;
    private final long expirationMs;

    public AuthTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username is required");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        logger.info("JWT generated successfully for username={}, expiresAt={}", username, expiryDate);
        return token;
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            logger.debug("JWT validation success");
            return true;
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Token is required");
        }

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}