package com.cems.security;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cems.entity.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final Key key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
		byte[] bytes = Base64.getDecoder().decode(secret);
		if (bytes.length < 32)
			throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes");
		key = Keys.hmacShaKeyFor(bytes);
		this.expirationMs = expirationMs;
	}

	public String generate(User user) {
		Date now = new Date();
		String roles = user.getRoles().stream().map(Role -> Role.getName()).collect(Collectors.joining(","));
		return Jwts.builder().subject(user.getEmail()).claim("roles", roles).issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
	}

	public String subject(String token) {
		return Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(token).getPayload()
				.getSubject();
	}

	public long getExpirationSeconds() {
		return expirationMs / 1000;
	}
}
