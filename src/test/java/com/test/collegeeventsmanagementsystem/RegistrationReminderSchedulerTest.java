package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.repository.EventRegistrationRepository;
import com.cems.service.EmailNotificationService;
import com.cems.serviceImpl.RegistrationReminderScheduler;

class RegistrationReminderSchedulerTest {
	private final EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
	private final EmailNotificationService notifications = mock(EmailNotificationService.class);
	private final RegistrationReminderScheduler scheduler = new RegistrationReminderScheduler(registrations,
			notifications);

	@Test
	void sendsRemindersToRegisteredStudentsAndOrganizerOncePerEvent() {
		Event event = mock(Event.class);
		when(event.getId()).thenReturn("event-15");
		EventRegistration first = registration(event);
		EventRegistration second = registration(event);
		List<EventRegistration> attendees = List.of(first, second);
		when(registrations.findRemindersDue(any(Instant.class), any(Instant.class))).thenReturn(attendees);
		when(registrations.findByEventIdOrderByRegisteredAtAsc("event-15")).thenReturn(attendees);
		when(notifications.eventReminder(any(EventRegistration.class))).thenReturn(true);
		when(notifications.organizerEventReminder(eq(event), anyList())).thenReturn(true);

		scheduler.sendUpcomingEventReminders();

		ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
		verify(registrations).findRemindersDue(from.capture(), to.capture());
		assertEquals(Duration.ofMinutes(30), Duration.between(from.getValue(), to.getValue()));
		verify(notifications).eventReminder(first);
		verify(notifications).eventReminder(second);
		verify(notifications).organizerEventReminder(event, attendees);
		verify(first).markReminderSent();
		verify(second).markReminderSent();
		verify(event).markOrganizerReminderSent();
	}

	@Test
	void doesNotMarkRemindersSentWhenEmailIsDisabled() {
		Event event = mock(Event.class);
		when(event.getId()).thenReturn("event-21");
		EventRegistration attendee = registration(event);
		when(registrations.findRemindersDue(any(Instant.class), any(Instant.class))).thenReturn(List.of(attendee));
		when(registrations.findByEventIdOrderByRegisteredAtAsc("event-21")).thenReturn(List.of(attendee));
		when(notifications.eventReminder(attendee)).thenReturn(false);
		when(notifications.organizerEventReminder(event, List.of(attendee))).thenReturn(false);

		scheduler.sendUpcomingEventReminders();

		verify(attendee, never()).markReminderSent();
		verify(event, never()).markOrganizerReminderSent();
	}

	private EventRegistration registration(Event event) {
		EventRegistration registration = mock(EventRegistration.class);
		when(registration.getEvent()).thenReturn(event);
		when(registration.getStatus()).thenReturn("REGISTERED");
		return registration;
	}
}
