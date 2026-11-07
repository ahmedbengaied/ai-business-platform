package com.ahmed.ai_business_platform.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ahmed.ai_business_platform.user.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationSeconds;

	public JwtService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration}") long expirationSeconds) {
		if (secret.length() < 32) {
			throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
		}
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationSeconds = expirationSeconds;
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + expirationSeconds * 1000);
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("role", user.getRole().name())
				.claim("authVersion", user.getAuthVersion())
				.issuedAt(now)
				.expiration(expiration)
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, String username, long authVersion) {
		Claims claims = parseClaims(token);
		Object tokenVersion = claims.get("authVersion");
		boolean versionMatches = tokenVersion instanceof Number number
				? number.longValue() == authVersion
				: tokenVersion == null && authVersion == 0;
		return username.equalsIgnoreCase(claims.getSubject())
				&& claims.getExpiration().after(new Date())
				&& versionMatches;
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}
}
