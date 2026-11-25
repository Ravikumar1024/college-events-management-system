package com.cems.service;

import java.time.Instant;
import java.util.List;

public interface AdminStudentService {
	List<StudentResponse> students();

	void deactivate(String studentId);

	record StudentResponse(String id, String fullName, String email, String studentNumber, boolean active,
			boolean emailVerified, Instant createdAt) {
	}
}
