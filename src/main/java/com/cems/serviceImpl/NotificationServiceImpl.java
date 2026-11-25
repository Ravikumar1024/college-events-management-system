package com.cems.serviceImpl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.entity.AppNotification;
import com.cems.entity.Event;
import com.cems.entity.User;
import com.cems.repository.AppNotificationRepository;
import com.cems.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {
	private final AppNotificationRepository notifications;

	public NotificationServiceImpl(AppNotificationRepository notifications) {
		this.notifications = notifications;
	}

	@Override
	@Transactional
	public void create(User recipient, Event event, String type, String title, String message, String referenceId) {
		notifications.save(new AppNotification(recipient, event, type, title, message, referenceId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationResponse> mine(String email) {
		return notifications.findForRecipient(email).stream().map(this::response).toList();
	}

	@Override
	@Transactional
	public void markRead(String notificationId, String email) {
		AppNotification notification = notifications.findByIdAndRecipientEmail(notificationId, email)
				.orElseThrow(() -> new AccessDeniedException("Notification not found for this account."));
		notification.markRead();
	}

	@Override
	@Transactional
	public void markReadForReference(String referenceId, String email, String type) {
		notifications.findForReference(referenceId, email, type).forEach(AppNotification::markRead);
	}

	private NotificationResponse response(AppNotification notification) {
		Event event = notification.getEvent();
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
				notification.getMessage(), event == null ? null : event.getId(),
				event == null ? null : event.getTitle(), notification.getReferenceId(),
				notification.getCreatedAt(), notification.getReadAt());
	}
}
