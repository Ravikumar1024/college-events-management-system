package com.cems.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cems.dto.AdminUserDtos;
import com.cems.dto.AuthDtos;
import com.cems.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/admin-invitations")
public class AdminInvitationController {
	private final AdminUserService adminUsers;

	public AdminInvitationController(AdminUserService adminUsers) {
		this.adminUsers = adminUsers;
	}

	@PostMapping("/complete")
	public AuthDtos.VerificationResponse complete(@Valid @RequestBody AdminUserDtos.CompleteInvitationRequest request) {
		return adminUsers.completeInvitation(request);
	}
}
