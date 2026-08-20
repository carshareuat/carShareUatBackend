package com.carpool.security;

import com.carpool.config.AppProperties;
import com.carpool.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private SecretKey secretKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() {
        String secret = appProperties.getJwt().getSecret().trim();
        byte[] keyBytes;
        try {
            // Support either Base64 secrets or plain-text secrets.
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException ex) {
            keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        secretKey = Keys.hmacShaKeyFor(keyBytes.length >= 32 ? keyBytes : java.util.Arrays.copyOf(keyBytes, 32));
    }

    public String generateAccessToken(UUID userId, Role role, String mobile, UUID ownerId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(userId.toString())
            .claims(Map.of(
                "role", role.name(),
                "mobile", mobile,
                "ownerId", ownerId == null ? "" : ownerId.toString()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS);
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
