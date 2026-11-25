package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cems.dto.AdminUserDtos;
import com.cems.entity.AdminInvitationToken;
import com.cems.entity.Role;
import com.cems.entity.User;
import com.cems.repository.AdminInvitationTokenRepository;
import com.cems.repository.RoleRepository;
import com.cems.repository.UserRepository;
import com.cems.security.SecureTokenGenerator;
import com.cems.service.EmailNotificationService;
import com.cems.serviceImpl.AdminUserServiceImpl;

class AdminUserServiceTest {
	private final UserRepository users = mock(UserRepository.class);
	private final RoleRepository roles = mock(RoleRepository.class);
	private final AdminInvitationTokenRepository invitations = mock(AdminInvitationTokenRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final EmailNotificationService notifications = mock(EmailNotificationService.class);
	private final Role adminRole = mock(Role.class);
	private final AdminUserServiceImpl service = new AdminUserServiceImpl(users, roles, invitations, passwordEncoder,
			notifications, "https://offbeat.example");

	@BeforeEach
	void setupAdminRole() {
		when(adminRole.getName()).thenReturn("ROLE_ADMIN");
		when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
	}

	@Test
	void existingStudentGetsAdminMembershipWithoutChangingTheirAccount() {
		User student = new User("Student Name", "student@college.edu", "existing-password-hash");
		when(users.findByNormalizedEmail("student@college.edu")).thenReturn(Optional.of(student));

		AdminUserDtos.CreateAdminResponse result = service.createAdmin(
				new AdminUserDtos.CreateAdminRequest(null, "Student@College.edu"));

		assertTrue(student.getRoles().contains(adminRole));
		assertEquals("existing-password-hash", student.getPasswordHash());
		assertEquals("Student Name", student.getFullName());
		assertTrue(result.message().contains("existing account"));
		verify(users, never()).save(any(User.class));
		verify(invitations, never()).save(any(AdminInvitationToken.class));
		verify(notifications, never()).adminInvitation(any(User.class), anyString());
	}

	@Test
	void newAdministratorHasNoStudentProfileAndReceivesHashedSetupInvitation() {
		when(users.findByNormalizedEmail("admin@college.edu")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("random-initial-password-hash");
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(invitations.findByUserIdForUpdate(anyString())).thenReturn(Optional.empty());

		AdminUserDtos.CreateAdminResponse result = service.createAdmin(
				new AdminUserDtos.CreateAdminRequest("New Admin", "admin@college.edu"));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(users).save(userCaptor.capture());
		User created = userCaptor.getValue();
		assertEquals("New Admin", created.getFullName());
		assertEquals("admin@college.edu", created.getEmail());
		assertFalse(created.isEmailVerified());
		assertTrue(created.getRoles().contains(adminRole));

		ArgumentCaptor<AdminInvitationToken> invitationCaptor = ArgumentCaptor.forClass(AdminInvitationToken.class);
		verify(invitations).save(invitationCaptor.capture());
		ArgumentCaptor<String> setupUrlCaptor = ArgumentCaptor.forClass(String.class);
		verify(notifications).adminInvitation(same(created), setupUrlCaptor.capture());
		String rawToken = setupUrlCaptor.getValue().substring(setupUrlCaptor.getValue().indexOf("token=") + 6);
		assertEquals(SecureTokenGenerator.sha256(rawToken), invitationCaptor.getValue().getTokenHash());
		assertEquals(created, invitationCaptor.getValue().getUser());
		assertTrue(result.message().contains("setup link"));
	}

	@Test
	void completingInvitationSetsPasswordVerifiesUserAndConsumesToken() {
		String rawToken = SecureTokenGenerator.generateToken(32);
		User user = new User("New Admin", "admin@college.edu", "random-password-hash");
		user.addRole(adminRole);
		AdminInvitationToken invitation = new AdminInvitationToken(user, SecureTokenGenerator.sha256(rawToken),
				Instant.now().plusSeconds(600));
		when(invitations.findByTokenHashForUpdate(SecureTokenGenerator.sha256(rawToken)))
				.thenReturn(Optional.of(invitation));
		when(passwordEncoder.encode("new-password-123")).thenReturn("new-password-hash");

		var result = service.completeInvitation(
				new AdminUserDtos.CompleteInvitationRequest(rawToken, "new-password-123"));

		assertTrue(result.verified());
		assertTrue(user.isEmailVerified());
		assertEquals("new-password-hash", user.getPasswordHash());
		verify(invitations).delete(invitation);
	}
}
