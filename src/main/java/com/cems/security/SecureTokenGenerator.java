package com.cems.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class SecureTokenGenerator {
	private static final SecureRandom RANDOM = new SecureRandom();

	private SecureTokenGenerator() {
	}

	public static String generateToken(int byteLength) {
		if (byteLength < 1) {
			throw new IllegalArgumentException("Token length must be positive.");
		}
		byte[] value = new byte[byteLength];
		RANDOM.nextBytes(value);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	public static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}
}
