package com.cems.service;

import java.time.Instant;
import java.util.List;

public interface InvitationService {
	List<StudentOption> candidates(String eventId, String organizerEmail);

	InviteResult invite(String eventId, List<String> studentIds, String organizerEmail);

	List<InvitationResponse> mine(String studentEmail);

	InvitationResponse respond(String invitationId, String studentEmail, boolean accept);

	record StudentOption(String id, String fullName, String email, String studentNumber) {
	}

	record InviteResult(int invited, int unchanged) {
	}

	record InvitationResponse(String id, String eventId, String eventTitle, String status, Instant createdAt,
			Instant respondedAt) {
	}
}
