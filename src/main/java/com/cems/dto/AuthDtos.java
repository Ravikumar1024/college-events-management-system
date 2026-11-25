package com.cems.dto;

import java.util.List;

import jakarta.validation.constraints.*;

public final class AuthDtos {
	private AuthDtos() {
	}

	public record RegisterRequest(@NotBlank @Size(max = 150) String fullName,
			@NotBlank @Email @Size(max = 180) String email,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotBlank @Size(max = 40) String studentNumber) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record EmailRequest(@NotBlank @Email @Size(max = 180) String email) {
	}

	public record OtpVerificationRequest(@NotBlank @Email @Size(max = 180) String email,
			@NotBlank @Pattern(regexp = "\\d{6}") String otp) {
	}

	public record ActivationLinkRequest(@NotBlank @Size(max = 128) String token) {
	}

	public record RegistrationResponse(String email, String message) {
	}

	public record VerificationResponse(boolean verified, String message) {
	}

	public record AuthResponse(String token, String tokenType, long expiresInSeconds, String userId, String fullName,
			String email, List<String> roles) {
	}
}
