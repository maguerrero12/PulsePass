package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final String expectedIssuer;
    private final Duration ttl; //

    public JwtService(
            @Value("${pulsepass.security.jwt.secret-base64}") String secretBase64,
            @Value("${pulsepass.security.jwt.issuer}") String issuer,
            @Value("${pulsepass.security.jwt.access-token-ttl}") Duration ttl) {

        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expectedIssuer = issuer;
        this.ttl = ttl;
    }

    public String generateToken(SecurityPrincipal principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getId().toString())
                .issuer(expectedIssuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid or expired JWT token", e);
        }
    }
}