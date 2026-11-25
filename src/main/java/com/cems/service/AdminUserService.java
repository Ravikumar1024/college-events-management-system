package com.cems.service;

import com.cems.dto.AdminUserDtos;
import com.cems.dto.AuthDtos;

public interface AdminUserService {
	AdminUserDtos.CreateAdminResponse createAdmin(AdminUserDtos.CreateAdminRequest request);

	AuthDtos.VerificationResponse completeInvitation(AdminUserDtos.CompleteInvitationRequest request);
}
