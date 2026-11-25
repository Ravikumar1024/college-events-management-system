package com.cems.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cems.service.InvitationService;

@RestController
@RequestMapping("/api")
public class InvitationController {
	private final InvitationService invitations;

	public InvitationController(InvitationService invitations) {
		this.invitations = invitations;
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/students/me/invitations")
	public List<InvitationService.InvitationResponse> mine(Principal principal) {
		return invitations.mine(principal.getName());
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/invitations/{invitationId}/accept")
	public InvitationService.InvitationResponse accept(@PathVariable String invitationId, Principal principal) {
		return invitations.respond(invitationId, principal.getName(), true);
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/invitations/{invitationId}/decline")
	public InvitationService.InvitationResponse decline(@PathVariable String invitationId, Principal principal) {
		return invitations.respond(invitationId, principal.getName(), false);
	}
}
