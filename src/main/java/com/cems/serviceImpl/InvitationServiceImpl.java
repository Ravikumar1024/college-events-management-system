package com.cems.serviceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.entity.Event;
import com.cems.entity.EventInvitation;
import com.cems.entity.Student;
import com.cems.repository.EventInvitationRepository;
import com.cems.repository.EventRepository;
import com.cems.repository.StudentRepository;
import com.cems.repository.UserRepository;
import com.cems.service.EmailNotificationService;
import com.cems.service.InvitationService;
import com.cems.service.NotificationService;
import com.cems.service.RegistrationService;

@Service
public class InvitationServiceImpl implements InvitationService {
	private final EventServiceImpl eventAccess;
	private final EventRepository events;
	private final StudentRepository students;
	private final EventInvitationRepository invitations;
	private final UserRepository users;
	private final NotificationService notifications;
	private final EmailNotificationService emailNotifications;
	private final RegistrationService registrations;
	private final String frontendUrl;

	public InvitationServiceImpl(EventServiceImpl eventAccess, EventRepository events, StudentRepository students,
			EventInvitationRepository invitations, UserRepository users, NotificationService notifications,
			EmailNotificationService emailNotifications, RegistrationService registrations,
			@Value("${app.frontend-url}") String frontendUrl) {
		this.eventAccess = eventAccess;
		this.events = events;
		this.students = students;
		this.invitations = invitations;
		this.users = users;
		this.notifications = notifications;
		this.emailNotifications = emailNotifications;
		this.registrations = registrations;
		this.frontendUrl = frontendUrl.replaceAll("/+$", "");
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentOption> candidates(String eventId, String organizerEmail) {
		eventAccess.requireOrganizer(eventId, organizerEmail);
		List<Student> candidates = isAdmin(organizerEmail) ? students.findAllInviteCandidates()
				: students.findInviteCandidates(organizerEmail);
		return candidates.stream()
				.map(student -> new StudentOption(student.getId(), student.getUser().getFullName(),
						student.getUser().getEmail(), student.getStudentNumber()))
				.toList();
	}

	@Override
	@Transactional
	public InviteResult invite(String eventId, List<String> studentIds, String organizerEmail) {
		Event event = eventAccess.requireOrganizer(eventId, organizerEmail);
		validateOpenForInvites(event);
		if (studentIds == null || studentIds.isEmpty()) {
			throw new IllegalArgumentException("Select at least one student to invite.");
		}
		List<String> requestedIds = studentIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
		if (requestedIds.isEmpty()) {
			throw new IllegalArgumentException("Select at least one student to invite.");
		}
		Map<String, Student> candidates = (isAdmin(organizerEmail) ? students.findAllInviteCandidates()
				: students.findInviteCandidates(organizerEmail)).stream()
				.collect(Collectors.toMap(Student::getId, Function.identity()));
		if (!candidates.keySet().containsAll(requestedIds)) {
			throw new AccessDeniedException("Select only active students you are allowed to invite.");
		}

		var invitedBy = users.findByEmailIgnoreCase(organizerEmail)
				.orElseThrow(() -> new IllegalArgumentException("Account not found."));
		int invited = 0;
		for (String studentId : requestedIds) {
			Student student = candidates.get(studentId);
			EventInvitation invitation = invitations.findByEventIdAndStudentId(eventId, studentId).orElse(null);
			if (invitation != null && !"DECLINED".equals(invitation.getStatus())) {
				continue;
			}
			if (invitation == null) {
				invitation = new EventInvitation(event, student, invitedBy);
			} else {
				invitation.inviteAgain(invitedBy);
			}
			invitation = invitations.save(invitation);
			notifications.create(student.getUser(), event, "EVENT_INVITATION", "New event invitation",
					"You have been invited to " + event.getTitle() + ".", invitation.getId());
			emailNotifications.eventInvitation(student.getUser(), event,
					frontendUrl + "/my-events#invitations");
			invited++;
		}
		return new InviteResult(invited, requestedIds.size() - invited);
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvitationResponse> mine(String studentEmail) {
		return invitations.findByStudentEmail(studentEmail).stream().map(this::response).toList();
	}

	@Override
	@Transactional
	public InvitationResponse respond(String invitationId, String studentEmail, boolean accept) {
		EventInvitation invitation = invitations.findByIdForUpdate(invitationId)
				.orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
		if (!invitation.getStudent().getUser().getEmail().equalsIgnoreCase(studentEmail)) {
			throw new AccessDeniedException("This invitation belongs to another account.");
		}
		if (!"PENDING".equals(invitation.getStatus())) {
			throw new IllegalArgumentException("This invitation has already been answered.");
		}
		Event event = events.findByIdForUpdate(invitation.getEvent().getId())
				.orElseThrow(() -> new IllegalArgumentException("Event not found."));
		if (accept) {
			validateOpenForInvites(event);
			registrations.registerFromInvitation(event, invitation.getStudent());
			invitation.accept();
			notifications.markReadForReference(invitation.getId(), studentEmail, "EVENT_INVITATION");
			notifications.create(event.getOrganizer(), event, "INVITATION_ACCEPTED", "Invitation accepted",
					invitation.getStudent().getUser().getFullName() + " accepted the invitation to " + event.getTitle()
							+ ".",
					invitation.getId());
		} else {
			invitation.decline();
			notifications.markReadForReference(invitation.getId(), studentEmail, "EVENT_INVITATION");
			notifications.create(event.getOrganizer(), event, "INVITATION_DECLINED", "Invitation declined",
					invitation.getStudent().getUser().getFullName() + " declined the invitation to " + event.getTitle()
							+ ".",
					invitation.getId());
		}
		return response(invitation);
	}

	private void validateOpenForInvites(Event event) {
		if (!"PUBLISHED".equals(event.getStatus()) || !event.getStartsAt().isAfter(Instant.now())
				|| !event.getRegistrationDeadline().isAfter(Instant.now())) {
			throw new IllegalArgumentException("This event is no longer accepting invitations.");
		}
	}

	private boolean isAdmin(String email) {
		return users.findByEmailIgnoreCase(email).orElseThrow().getRoles().stream()
				.anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
	}

	private InvitationResponse response(EventInvitation invitation) {
		return new InvitationResponse(invitation.getId(), invitation.getEvent().getId(),
				invitation.getEvent().getTitle(), invitation.getStatus(), invitation.getCreatedAt(),
				invitation.getRespondedAt());
	}
}
