package com.cems.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cems.dto.EventDtos;
import com.cems.entity.EventRegistration;
import com.cems.service.InvitationService;
import com.cems.service.RegistrationService;
import com.cems.serviceImpl.EventServiceImpl;

@RestController
@RequestMapping("/api/organizer")
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
public class OrganizerController {
	private final EventServiceImpl events;
	private final RegistrationService registrations;
	private final InvitationService invitations;

	public OrganizerController(EventServiceImpl events, RegistrationService registrations,
			InvitationService invitations) {
		this.events = events;
		this.registrations = registrations;
		this.invitations = invitations;
	}

	public record Attendee(String id, String fullName, String email, String studentNumber, String status,
			Instant registeredAt) {
	}

	public record InviteRequest(List<String> studentIds) {
	}

	@GetMapping("/events")
	public List<EventDtos.Response> events(Principal principal) {
		return events.organizedBy(principal.getName());
	}

	@GetMapping("/events/{eventId}/registrations")
	public List<Attendee> attendees(@PathVariable("eventId") String eventId, Principal principal) {
		events.requireOrganizer(eventId, principal.getName());
		return registrations.forEvent(eventId).stream().map(this::attendee).toList();
	}

	@GetMapping("/events/{eventId}/invite-candidates")
	public List<InvitationService.StudentOption> inviteCandidates(@PathVariable("eventId") String eventId,
			Principal principal) {
		return invitations.candidates(eventId, principal.getName());
	}

	@PostMapping("/events/{eventId}/invitations")
	public InvitationService.InviteResult invite(@PathVariable("eventId") String eventId,
			@RequestBody InviteRequest request, Principal principal) {
		return invitations.invite(eventId, request.studentIds(), principal.getName());
	}

	@PostMapping("/registrations/{registrationId}/approve")
	public Attendee approve(@PathVariable("registrationId") String registrationId, Principal principal) {
		return attendee(registrations.approve(registrationId, principal.getName()));
	}

	@PostMapping("/registrations/{registrationId}/reject")
	public Attendee reject(@PathVariable("registrationId") String registrationId, Principal principal) {
		return attendee(registrations.reject(registrationId, principal.getName()));
	}

	@PostMapping("/registrations/{registrationId}/attendance")
	public Attendee attendance(@PathVariable("registrationId") String registrationId, Principal principal) {
		return attendee(registrations.markAttended(registrationId, principal.getName()));
	}

	@GetMapping(value = "/events/{eventId}/report.csv", produces = "text/csv")
	public ResponseEntity<byte[]> report(@PathVariable("eventId") String eventId, Principal principal) {
		events.requireOrganizer(eventId, principal.getName());
		StringBuilder csv = new StringBuilder("Student name,Email,Student number,Status,Registered at\n");
		registrations.forEvent(eventId)
				.forEach(r -> csv.append(row(r.getStudent().getUser().getFullName())).append(',')
						.append(row(r.getStudent().getUser().getEmail())).append(',')
						.append(row(r.getStudent().getStudentNumber())).append(',').append(row(r.getStatus()))
						.append(',').append(row(r.getRegisteredAt().toString())).append("\r\n"));
		return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"event-" + eventId + "-participation.csv\"")
				.body(csv.toString().getBytes(StandardCharsets.UTF_8));
	}

	private Attendee attendee(EventRegistration registration) {
		return new Attendee(registration.getId(), registration.getStudent().getUser().getFullName(),
				registration.getStudent().getUser().getEmail(), registration.getStudent().getStudentNumber(),
				registration.getStatus(), registration.getRegisteredAt());
	}

	private String row(String value) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
