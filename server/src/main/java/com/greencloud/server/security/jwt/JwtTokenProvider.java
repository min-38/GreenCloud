package com.greencloud.server.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.Base64;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class        JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.access-token-exp-seconds}")
    private long accessExpSeconds;

    @Getter
    @Value("${jwt.refresh-token-exp-seconds}")
    private long refreshExpSeconds;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpSeconds)))
                .claims(Map.of("email", email, "role", role))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpSeconds)))
                .claim("typ", "refresh")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}