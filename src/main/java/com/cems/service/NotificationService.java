package com.cems.service;

import java.util.List;

import com.cems.entity.Event;
import com.cems.entity.User;

public interface NotificationService {
	void create(User recipient, Event event, String type, String title, String message, String referenceId);

	List<NotificationResponse> mine(String email);

	void markRead(String notificationId, String email);

	void markReadForReference(String referenceId, String email, String type);

	record NotificationResponse(String id, String type, String title, String message, String eventId, String eventTitle,
			String referenceId, java.time.Instant createdAt, java.time.Instant readAt) {
	}
}
