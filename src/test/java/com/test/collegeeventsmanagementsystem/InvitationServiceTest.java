package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cems.entity.Event;
import com.cems.entity.EventInvitation;
import com.cems.entity.Student;
import com.cems.entity.User;
import com.cems.repository.EventInvitationRepository;
import com.cems.repository.EventRepository;
import com.cems.repository.StudentRepository;
import com.cems.repository.UserRepository;
import com.cems.service.EmailNotificationService;
import com.cems.service.NotificationService;
import com.cems.service.RegistrationService;
import com.cems.serviceImpl.EventServiceImpl;
import com.cems.serviceImpl.InvitationServiceImpl;

class InvitationServiceTest {
	@Test
	void capacityFailureLeavesInvitationPending() {
		EventServiceImpl eventAccess = mock(EventServiceImpl.class);
		EventRepository events = mock(EventRepository.class);
		StudentRepository students = mock(StudentRepository.class);
		EventInvitationRepository invitations = mock(EventInvitationRepository.class);
		UserRepository users = mock(UserRepository.class);
		NotificationService notifications = mock(NotificationService.class);
		EmailNotificationService emailNotifications = mock(EmailNotificationService.class);
		RegistrationService registrations = mock(RegistrationService.class);
		InvitationServiceImpl service = new InvitationServiceImpl(eventAccess, events, students, invitations,
				users, notifications, emailNotifications, registrations, "http://localhost:8080");

		String eventId = "event-23";
		String invitationId = "invitation-91";
		Event event = mock(Event.class);
		EventInvitation invitation = mock(EventInvitation.class);
		Student student = mock(Student.class);
		User user = mock(User.class);
		when(invitation.getStudent()).thenReturn(student);
		when(invitation.getEvent()).thenReturn(event);
		when(invitation.getStatus()).thenReturn("PENDING");
		when(student.getUser()).thenReturn(user);
		when(user.getEmail()).thenReturn("student@college.edu");
		when(event.getId()).thenReturn(eventId);
		when(event.getStatus()).thenReturn("PUBLISHED");
		when(event.getStartsAt()).thenReturn(Instant.now().plusSeconds(3600));
		when(event.getRegistrationDeadline()).thenReturn(Instant.now().plusSeconds(1800));
		when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
		when(invitations.findByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));
		when(registrations.registerFromInvitation(event, student))
				.thenThrow(new IllegalArgumentException("No seats remain."));

		assertThrows(IllegalArgumentException.class,
				() -> service.respond(invitationId, "student@college.edu", true));

		verify(invitation, never()).accept();
		verify(notifications, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
	}
}
