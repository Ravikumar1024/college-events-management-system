package com.cems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminUserDtos {
	private AdminUserDtos() {
	}

	public record CreateAdminRequest(@Size(max = 150) String fullName,
			@NotBlank @Email @Size(max = 180) String email) {
	}

	public record CreateAdminResponse(String email, String message) {
	}

	public record CompleteInvitationRequest(@NotBlank @Size(max = 128) String token,
			@NotBlank @Size(min = 8, max = 72) String password) {
	}
}
