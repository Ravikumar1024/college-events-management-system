package com.cems.serviceImpl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.entity.Student;
import com.cems.repository.EventRegistrationRepository;
import com.cems.repository.EventRepository;
import com.cems.repository.StudentRepository;
import com.cems.service.EmailNotificationService;
import com.cems.service.NotificationService;
import com.cems.service.RegistrationService;

@Service
public class RegistrationServiceImpl implements RegistrationService {
	private static final List<String> CONFIRMED_STATUSES = List.of("REGISTERED", "ATTENDED");
	private static final DateTimeFormatter EVENT_TIME_FORMAT =
			DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a z", Locale.ENGLISH).withZone(ZoneId.systemDefault());

	private final EventRepository events;
	private final StudentRepository students;
	private final EventRegistrationRepository registrations;
	private final EventServiceImpl eventAccess;
	private final EmailNotificationService emailNotifications;
	private final NotificationService notifications;

	public RegistrationServiceImpl(EventRepository events, StudentRepository students,
			EventRegistrationRepository registrations, EventServiceImpl eventAccess,
			EmailNotificationService emailNotifications, NotificationService notifications) {
		this.events = events;
		this.students = students;
		this.registrations = registrations;
		this.eventAccess = eventAccess;
		this.emailNotifications = emailNotifications;
		this.notifications = notifications;
	}

	@Override
	@Transactional
	public EventRegistration register(String eventId, String email) {
		Event event = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found."));
		Student student = students.findByUserEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("Student profile not found."));
		validateOpenForRequests(event);

		EventRegistration registration = registrations.findByEventIdAndStudentId(eventId, student.getId())
				.orElse(null);
		if (registration != null && !List.of("CANCELLED", "REJECTED").contains(registration.getStatus())) {
			throw new IllegalArgumentException("A seat request or registration already exists for this event.");
		}
		if (registration == null) {
			registration = registrations.save(new EventRegistration(event, student));
		} else {
			registration.reactivate();
		}
		notifications.create(event.getOrganizer(), event, "SEAT_REQUEST", "New seat request",
				student.getUser().getFullName() + " requested a seat for " + event.getTitle() + ".",
				registration.getId());
		return registration;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EventRegistration> mine(String email) {
		Student student = students.findByUserEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("Student profile not found."));
		return registrations.findByStudentIdOrderByRegisteredAtDesc(student.getId());
	}

	@Override
	@Transactional
	public void cancel(String id, String email) {
		EventRegistration registration = registrations.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Registration not found."));
		if (!registration.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
			throw new AccessDeniedException("Cannot cancel another student's seat request.");
		}
		String status = registration.getStatus();
		if (!"REGISTERED".equals(status) && !"PENDING_APPROVAL".equals(status)) {
			throw new IllegalArgumentException("This seat request can no longer be cancelled.");
		}
		registration.cancel();
		if ("REGISTERED".equals(status)) {
			emailNotifications.registrationCancelled(registration);
		}
		notifications.markReadForReference(registration.getId(),
				registration.getEvent().getOrganizer().getEmail(), "SEAT_REQUEST");
		notifications.create(registration.getEvent().getOrganizer(), registration.getEvent(),
				"SEAT_REQUEST_CANCELLED", "Seat request cancelled",
				registration.getStudent().getUser().getFullName() + " cancelled their request for "
						+ registration.getEvent().getTitle() + ".",
				registration.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public List<EventRegistration> forEvent(String eventId) {
		return registrations.findByEventIdOrderByRegisteredAtAsc(eventId);
	}

	@Override
	@Transactional
	public EventRegistration approve(String registrationId, String organizerEmail) {
		EventRegistration registration = registrations.findById(registrationId)
				.orElseThrow(() -> new IllegalArgumentException("Seat request not found."));
		String eventId = registration.getEvent().getId();
		eventAccess.requireOrganizer(eventId, organizerEmail);
		Event event = events.findByIdForUpdate(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found."));
		registration = registrations.findById(registrationId)
				.orElseThrow(() -> new IllegalArgumentException("Seat request not found."));
		if (!"PENDING_APPROVAL".equals(registration.getStatus())) {
			throw new IllegalArgumentException("This seat request is no longer waiting for approval.");
		}
		validateCanApprove(event);
		if (event.getCapacity() != null
				&& registrations.countByEventIdAndStatusIn(eventId, CONFIRMED_STATUSES) >= event.getCapacity()) {
			throw new IllegalArgumentException("No seats remain. The request is still pending.");
		}
		registration.approve();
		notifications.markReadForReference(registration.getId(), registration.getEvent().getOrganizer().getEmail(),
				"SEAT_REQUEST");
		notifications.create(registration.getStudent().getUser(), event, "SEAT_APPROVED", "Your seat is confirmed",
				approvalNotificationMessage(event), registration.getId());
		emailNotifications.registrationConfirmed(registration);
		return registration;
	}

	private String approvalNotificationMessage(Event event) {
		return "Your seat for " + event.getTitle() + " is saved. "
				+ EVENT_TIME_FORMAT.format(event.getStartsAt()) + " at " + event.getVenue()
				+ ". Hosted by " + event.getOrganizer().getFullName() + ".";
	}

	@Override
	@Transactional
	public EventRegistration reject(String registrationId, String organizerEmail) {
		EventRegistration registration = registrations.findById(registrationId)
				.orElseThrow(() -> new IllegalArgumentException("Seat request not found."));
		String eventId = registration.getEvent().getId();
		eventAccess.requireOrganizer(eventId, organizerEmail);
		Event event = events.findByIdForUpdate(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found."));
		registration = registrations.findById(registrationId)
				.orElseThrow(() -> new IllegalArgumentException("Seat request not found."));
		if (!"PENDING_APPROVAL".equals(registration.getStatus())) {
			throw new IllegalArgumentException("This seat request is no longer waiting for approval.");
		}
		registration.reject();
		notifications.markReadForReference(registration.getId(), registration.getEvent().getOrganizer().getEmail(),
				"SEAT_REQUEST");
		notifications.create(registration.getStudent().getUser(), event, "SEAT_REJECTED", "Seat request update",
				"Your request for " + event.getTitle() + " was not approved.", registration.getId());
		return registration;
	}

	@Override
	@Transactional
	public EventRegistration registerFromInvitation(Event event, Student student) {
		validateOpenForRequests(event);
		EventRegistration registration = registrations.findByEventIdAndStudentId(event.getId(), student.getId())
				.orElse(null);
		if (registration != null && CONFIRMED_STATUSES.contains(registration.getStatus())) {
			return registration;
		}
		if (event.getCapacity() != null
				&& registrations.countByEventIdAndStatusIn(event.getId(), CONFIRMED_STATUSES) >= event.getCapacity()) {
			throw new IllegalArgumentException("No seats remain for this event. The invitation is still available.");
		}
		if (registration == null) {
			registration = new EventRegistration(event, student);
		} else {
			registration.reactivate();
		}
		registration.approve();
		registration = registrations.save(registration);
		emailNotifications.registrationConfirmed(registration);
		return registration;
	}

	private void validateOpenForRequests(Event event) {
		validateCanApprove(event);
		if (!event.getRegistrationDeadline().isAfter(Instant.now())) {
			throw new IllegalArgumentException("The registration deadline has passed.");
		}
	}

	private void validateCanApprove(Event event) {
		if (!"PUBLISHED".equals(event.getStatus())) {
			throw new IllegalArgumentException("This event is not open for seat requests.");
		}
		if (!event.getStartsAt().isAfter(Instant.now())) {
			throw new IllegalArgumentException("This event has already started.");
		}
	}

	@Override
	@Transactional
	public EventRegistration markAttended(String id, String organizerEmail) {
		EventRegistration registration = registrations.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Registration not found."));
		Event event = eventAccess.requireOrganizer(registration.getEvent().getId(), organizerEmail);
		if (!"REGISTERED".equals(registration.getStatus())) {
			throw new IllegalArgumentException("Only confirmed registrations can be marked attended.");
		}
		if (event.getStartsAt().isAfter(Instant.now())) {
			throw new IllegalArgumentException("Attendance cannot be recorded before the event starts.");
		}
		registration.markAttended();
		return registration;
	}

}
