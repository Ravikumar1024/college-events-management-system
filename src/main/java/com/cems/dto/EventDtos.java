package com.cems.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class EventDtos {
	private EventDtos() {
	}

	public record CreateRequest(@NotBlank @Size(max = 200) String title, @NotBlank String description,
			@NotNull String categoryId, @NotBlank String venue, @NotNull Instant startsAt, @NotNull Instant endsAt,
			@NotNull Instant registrationDeadline, @Positive Integer capacity, @NotBlank String status) {
	}

	public record Response(String id, String title, String description, String categoryId, String category,
			String organizerId, String organizer, String venue, Instant startsAt, Instant endsAt,
			Instant registrationDeadline, Integer capacity, String status) {
	}
}
