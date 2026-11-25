package com.cems.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cems.dto.AdminUserDtos;
import com.cems.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
	private final AdminUserService adminUsers;

	public AdminUserController(AdminUserService adminUsers) {
		this.adminUsers = adminUsers;
	}

	@PostMapping
	public AdminUserDtos.CreateAdminResponse createAdmin(@Valid @RequestBody AdminUserDtos.CreateAdminRequest request) {
		return adminUsers.createAdmin(request);
	}
}
