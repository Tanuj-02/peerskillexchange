package com.tanuj.peerskillexchange.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;

@Service
public class jwtService {
    private static final String secret = "MySecretKey02022005tmX45rosuperhitman";

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 *7))
                .claim("role",role)
                .signWith(getsignedKey())
                .compact();
    }

    private SecretKey getsignedKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims verifyTokenandExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getsignedKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token){
        return verifyTokenandExtractClaims(token).getSubject();
    }

    public Date extractExpiration(String token){
        return verifyTokenandExtractClaims(token).getExpiration();
    }

    public boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}