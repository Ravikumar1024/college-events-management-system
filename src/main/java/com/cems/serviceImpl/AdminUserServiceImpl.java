package com.cems.serviceImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.dto.AdminUserDtos;
import com.cems.dto.AuthDtos;
import com.cems.entity.AdminInvitationToken;
import com.cems.entity.Role;
import com.cems.entity.User;
import com.cems.repository.AdminInvitationTokenRepository;
import com.cems.repository.RoleRepository;
import com.cems.repository.UserRepository;
import com.cems.security.SecureTokenGenerator;
import com.cems.service.AdminUserService;
import com.cems.service.EmailNotificationService;

@Service
public class AdminUserServiceImpl implements AdminUserService {
	private static final Duration INVITATION_LIFETIME = Duration.ofHours(24);
	private static final String ADMIN_ROLE = "ROLE_ADMIN";

	private final UserRepository users;
	private final RoleRepository roles;
	private final AdminInvitationTokenRepository invitations;
	private final PasswordEncoder passwordEncoder;
	private final EmailNotificationService notifications;
	private final String frontendUrl;

	public AdminUserServiceImpl(UserRepository users, RoleRepository roles,
			AdminInvitationTokenRepository invitations, PasswordEncoder passwordEncoder,
			EmailNotificationService notifications, @Value("${app.frontend-url}") String frontendUrl) {
		this.users = users;
		this.roles = roles;
		this.invitations = invitations;
		this.passwordEncoder = passwordEncoder;
		this.notifications = notifications;
		this.frontendUrl = frontendUrl.replaceAll("/+$", "");
	}

	@Override
	@Transactional
	public AdminUserDtos.CreateAdminResponse createAdmin(AdminUserDtos.CreateAdminRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		Role adminRole = roles.findByName(ADMIN_ROLE)
				.orElseThrow(() -> new IllegalStateException("Administrator role is not configured."));
		User user = users.findByNormalizedEmail(email).orElse(null);
		if (user != null) {
			if (user.getRoles().stream().anyMatch(role -> ADMIN_ROLE.equals(role.getName()))) {
				if (!user.isEmailVerified()) {
					return invite(user);
				}
				return new AdminUserDtos.CreateAdminResponse(email, "This account already has administrator access.");
			}
			user.addRole(adminRole);
			return new AdminUserDtos.CreateAdminResponse(email,
					"Administrator access was added to the existing account. It can sign in with its current password.");
		}

		String fullName = request.fullName() == null ? "" : request.fullName().trim();
		if (fullName.isBlank()) {
			throw new IllegalArgumentException("Enter a full name for the new administrator account.");
		}
		User newUser = new User(fullName, email, passwordEncoder.encode(SecureTokenGenerator.generateToken(32)));
		newUser.requireEmailVerification();
		newUser.addRole(adminRole);
		return invite(users.save(newUser));
	}

	@Override
	@Transactional
	public AuthDtos.VerificationResponse completeInvitation(AdminUserDtos.CompleteInvitationRequest request) {
		String tokenHash = SecureTokenGenerator.sha256(request.token());
		AdminInvitationToken invitation = invitations.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> new IllegalArgumentException("This administrator invitation is invalid or expired."));
		if (!invitation.getExpiresAt().isAfter(Instant.now())) {
			throw new IllegalArgumentException("This administrator invitation has expired. Ask an administrator to send a new one.");
		}

		User user = invitation.getUser();
		boolean hasAdminRole = user.getRoles().stream().anyMatch(role -> ADMIN_ROLE.equals(role.getName()));
		if (!user.isEnabled() || !hasAdminRole) {
			throw new IllegalArgumentException("This administrator invitation is no longer valid.");
		}

		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.verifyEmail();
		invitations.delete(invitation);
		return new AuthDtos.VerificationResponse(true, "Your administrator account is ready. You can now sign in.");
	}

	private AdminUserDtos.CreateAdminResponse invite(User user) {
		String rawToken = SecureTokenGenerator.generateToken(32);
		Instant expiresAt = Instant.now().plus(INVITATION_LIFETIME);
		AdminInvitationToken invitation = invitations.findByUserIdForUpdate(user.getId()).orElse(null);
		if (invitation == null) {
			invitation = new AdminInvitationToken(user, SecureTokenGenerator.sha256(rawToken), expiresAt);
		} else {
			invitation.replace(SecureTokenGenerator.sha256(rawToken), expiresAt);
		}
		invitations.save(invitation);
		notifications.adminInvitation(user, frontendUrl + "/set-password?token=" + rawToken);
		return new AdminUserDtos.CreateAdminResponse(user.getEmail(),
				"An administrator setup link was sent to the email address. It expires in 24 hours.");
	}
}
