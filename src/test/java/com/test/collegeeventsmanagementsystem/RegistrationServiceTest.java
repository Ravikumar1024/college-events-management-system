package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.entity.Student;
import com.cems.entity.User;
import com.cems.repository.EventRegistrationRepository;
import com.cems.repository.EventRepository;
import com.cems.repository.StudentRepository;
import com.cems.service.EmailNotificationService;
import com.cems.service.NotificationService;
import com.cems.serviceImpl.EventServiceImpl;
import com.cems.serviceImpl.RegistrationServiceImpl;

class RegistrationServiceTest {
	private final EventRepository events = mock(EventRepository.class);
	private final StudentRepository students = mock(StudentRepository.class);
	private final EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
	private final EventServiceImpl eventAccess = mock(EventServiceImpl.class);
	private final EmailNotificationService emailNotifications = mock(EmailNotificationService.class);
	private final NotificationService notifications = mock(NotificationService.class);
	private RegistrationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new RegistrationServiceImpl(events, students, registrations, eventAccess, emailNotifications,
				notifications);
	}

	@Test
	void seatRequestStartsPendingAndNotifiesEventOrganizer() {
		String eventId = "event-27";
		Event event = mock(Event.class);
		Student student = mock(Student.class);
		User organizer = mock(User.class);
		User studentUser = mock(User.class);
		when(event.getStatus()).thenReturn("PUBLISHED");
		when(event.getRegistrationDeadline()).thenReturn(Instant.now().plusSeconds(3600));
		when(event.getStartsAt()).thenReturn(Instant.now().plusSeconds(7200));
		when(event.getOrganizer()).thenReturn(organizer);
		when(event.getTitle()).thenReturn("Hackathon");
		when(student.getId()).thenReturn("student-12");
		when(student.getUser()).thenReturn(studentUser);
		when(studentUser.getFullName()).thenReturn("Student");
		when(events.findById(eventId)).thenReturn(Optional.of(event));
		when(students.findByUserEmailIgnoreCase("student@college.edu")).thenReturn(Optional.of(student));
		when(registrations.findByEventIdAndStudentId(eventId, "student-12")).thenReturn(Optional.empty());
		when(registrations.save(any(EventRegistration.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		EventRegistration created = service.register(eventId, "student@college.edu");

		assertEquals("PENDING_APPROVAL", created.getStatus());
		verify(notifications).create(eq(organizer), eq(event), eq("SEAT_REQUEST"), eq("New seat request"),
				any(String.class), isNull());
		verify(emailNotifications, never()).registrationConfirmed(any(EventRegistration.class));
	}

	@Test
	void approvalChecksCapacityWhileHoldingEventLock() {
		String eventId = "event-27";
		Event event = mock(Event.class);
		EventRegistration pending = mock(EventRegistration.class);
		when(pending.getEvent()).thenReturn(event);
		when(pending.getStatus()).thenReturn("PENDING_APPROVAL");
		when(event.getId()).thenReturn(eventId);
		when(event.getStatus()).thenReturn("PUBLISHED");
		when(event.getStartsAt()).thenReturn(Instant.now().plusSeconds(3600));
		when(event.getCapacity()).thenReturn(1);
		when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
		when(registrations.findById("registration-8")).thenReturn(Optional.of(pending));
		when(registrations.countByEventIdAndStatusIn(eq(eventId), anyCollection())).thenReturn(1L);
		when(eventAccess.requireOrganizer(eventId, "organizer@college.edu")).thenReturn(event);

		assertThrows(IllegalArgumentException.class,
				() -> service.approve("registration-8", "organizer@college.edu"));

		verify(events).findByIdForUpdate(eventId);
		verify(pending, never()).approve();
		verify(emailNotifications, never()).registrationConfirmed(pending);
	}

	@Test
	void existingRequestsCanBeApprovedAfterRegistrationDeadline() {
		String eventId = "event-27";
		Event event = mock(Event.class);
		EventRegistration pending = mock(EventRegistration.class);
		Student student = mock(Student.class);
		User organizer = mock(User.class);
		User studentUser = mock(User.class);
		when(pending.getEvent()).thenReturn(event);
		when(pending.getStatus()).thenReturn("PENDING_APPROVAL");
		when(pending.getId()).thenReturn("registration-8");
		when(pending.getStudent()).thenReturn(student);
		when(event.getOrganizer()).thenReturn(organizer);
		when(organizer.getEmail()).thenReturn("organizer@college.edu");
		when(student.getUser()).thenReturn(studentUser);
		when(studentUser.getFullName()).thenReturn("Student");
		when(event.getId()).thenReturn(eventId);
		when(event.getTitle()).thenReturn("Robotics Hackathon");
		when(event.getVenue()).thenReturn("Innovation Hall");
		when(event.getStatus()).thenReturn("PUBLISHED");
		when(event.getStartsAt()).thenReturn(Instant.now().plusSeconds(3600));
		when(event.getRegistrationDeadline()).thenReturn(Instant.now().minusSeconds(10));
		when(event.getCapacity()).thenReturn(10);
		when(organizer.getFullName()).thenReturn("Event Host");
		when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
		when(registrations.findById("registration-8")).thenReturn(Optional.of(pending));
		when(registrations.countByEventIdAndStatusIn(eq(eventId), anyCollection())).thenReturn(0L);
		when(eventAccess.requireOrganizer(eventId, "organizer@college.edu")).thenReturn(event);

		assertSame(pending, service.approve("registration-8", "organizer@college.edu"));

		verify(pending).approve();
		verify(emailNotifications).registrationConfirmed(pending);
		verify(notifications).create(eq(studentUser), eq(event), eq("SEAT_APPROVED"),
				eq("Your seat is confirmed"),
				argThat(message -> message.contains("Robotics Hackathon")
						&& message.contains("Innovation Hall") && message.contains("Event Host")),
				eq("registration-8"));
	}

	@Test
	void invitationCannotReserveASeatAfterCapacityIsReached() {
		String eventId = "event-27";
		Event event = mock(Event.class);
		Student student = mock(Student.class);
		when(event.getId()).thenReturn(eventId);
		when(event.getStatus()).thenReturn("PUBLISHED");
		when(event.getStartsAt()).thenReturn(Instant.now().plusSeconds(3600));
		when(event.getRegistrationDeadline()).thenReturn(Instant.now().plusSeconds(1800));
		when(event.getCapacity()).thenReturn(2);
		when(student.getId()).thenReturn("student-12");
		when(registrations.findByEventIdAndStudentId(eventId, "student-12")).thenReturn(Optional.empty());
		when(registrations.countByEventIdAndStatusIn(eq(eventId), anyCollection())).thenReturn(2L);

		assertThrows(IllegalArgumentException.class, () -> service.registerFromInvitation(event, student));

		verify(registrations, never()).save(any(EventRegistration.class));
		verify(emailNotifications, never()).registrationConfirmed(any(EventRegistration.class));
	}
}
