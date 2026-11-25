package com.cems.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cems.entity.EventRegistration;
import com.cems.service.RegistrationService;

@RestController
@RequestMapping("/api")
public class RegistrationController {
	private final RegistrationService service;

	public RegistrationController(RegistrationService s) {
		service = s;
	}

	public record RegistrationResponse(String id, String eventId, String eventTitle, String status,
			java.time.Instant registeredAt) {
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/events/{eventId}/registrations")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public RegistrationResponse register(@PathVariable("eventId") String eventId, Principal p) {
		return dto(service.register(eventId, p.getName()));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/students/me/registrations")
	public List<RegistrationResponse> mine(Principal p) {
		return service.mine(p.getName()).stream().map(this::dto).toList();
	}

	@PreAuthorize("hasRole('STUDENT')")
	@DeleteMapping("/registrations/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable("id") String id, Principal p) {
		service.cancel(id, p.getName());
	}

	private RegistrationResponse dto(EventRegistration r) {
		return new RegistrationResponse(r.getId(), r.getEvent().getId(), r.getEvent().getTitle(), r.getStatus(),
				r.getRegisteredAt());
	}
}
