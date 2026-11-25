package com.cems.serviceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.repository.EventRegistrationRepository;
import com.cems.service.EmailNotificationService;

@Component
public class RegistrationReminderScheduler {
	private final EventRegistrationRepository registrations;
	private final EmailNotificationService notifications;

	public RegistrationReminderScheduler(EventRegistrationRepository registrations,
			EmailNotificationService notifications) {
		this.registrations = registrations;
		this.notifications = notifications;
	}

	@Scheduled(fixedDelayString = "${app.mail.reminder-scan-ms:60000}")
	@Transactional
	public void sendUpcomingEventReminders() {
		Instant now = Instant.now();
		List<EventRegistration> due = registrations.findRemindersDue(now, now.plus(30, ChronoUnit.MINUTES));
		Map<String, List<EventRegistration>> byEvent = new LinkedHashMap<>();
		for (EventRegistration registration : due) {
			byEvent.computeIfAbsent(registration.getEvent().getId(), ignored -> new ArrayList<>())
					.add(registration);
		}

		for (List<EventRegistration> eventRegistrations : byEvent.values()) {
			Event event = eventRegistrations.getFirst().getEvent();
			for (EventRegistration registration : eventRegistrations) {
				if (notifications.eventReminder(registration)) registration.markReminderSent();
			}
			if (event.getOrganizerReminderSentAt() == null) {
				List<EventRegistration> activeRegistrations = registrations
						.findByEventIdOrderByRegisteredAtAsc(event.getId()).stream()
						.filter(registration -> "REGISTERED".equals(registration.getStatus())).toList();
				if (!activeRegistrations.isEmpty() && notifications.organizerEventReminder(event, activeRegistrations))
					event.markOrganizerReminderSent();
			}
		}
	}
}
