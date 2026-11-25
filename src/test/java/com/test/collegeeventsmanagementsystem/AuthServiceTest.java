package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cems.dto.AuthDtos;
import com.cems.entity.EmailVerificationToken;
import com.cems.entity.User;
import com.cems.exceptionHandler.EmailNotVerifiedException;
import com.cems.repository.EmailVerificationTokenRepository;
import com.cems.repository.RoleRepository;
import com.cems.repository.StudentRepository;
import com.cems.repository.UserRepository;
import com.cems.security.JwtService;
import com.cems.service.EmailNotificationService;
import com.cems.serviceImpl.AuthServiceImpl;

class AuthServiceTest {
	private final UserRepository users = mock(UserRepository.class);
	private final RoleRepository roles = mock(RoleRepository.class);
	private final StudentRepository students = mock(StudentRepository.class);
	private final EmailVerificationTokenRepository tokens = mock(EmailVerificationTokenRepository.class);
	private final org.springframework.security.crypto.password.PasswordEncoder encoder = mock(
			org.springframework.security.crypto.password.PasswordEncoder.class);
	private final JwtService jwt = mock(JwtService.class);
	private final EmailNotificationService emailNotifications = mock(EmailNotificationService.class);
	private final AuthServiceImpl service = new AuthServiceImpl(users, roles, students, tokens, encoder, jwt,
			emailNotifications, "http://localhost:8080");

	@Test
	void duplicateEmailIsRejectedCaseInsensitivelyBeforeCreatingStudent() {
		when(users.existsByNormalizedEmail("student@college.edu")).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> service.register(new AuthDtos.RegisterRequest("Student", " Student@College.edu ",
						"password123", "S-123")));

		verify(students, never()).existsByNormalizedStudentNumber("S-123");
		verify(students, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void duplicateStudentNumberIsRejectedCaseInsensitively() {
		when(users.existsByNormalizedEmail("student@college.edu")).thenReturn(false);
		when(students.existsByNormalizedStudentNumber("S-123")).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> service.register(new AuthDtos.RegisterRequest("Student", "student@college.edu",
						"password123", "s-123")));

		verify(users, never()).save(org.mockito.ArgumentMatchers.any(User.class));
	}

	@Test
	void loginRequiresVerifiedEmailBeforeIssuingJwt() {
		User user = mock(User.class);
		when(users.findByNormalizedEmail("student@college.edu")).thenReturn(Optional.of(user));
		when(user.getPasswordHash()).thenReturn("password-hash");
		when(user.isEnabled()).thenReturn(true);
		when(user.isEmailVerified()).thenReturn(false);
		when(encoder.matches("password123", "password-hash")).thenReturn(true);

		assertThrows(EmailNotVerifiedException.class,
				() -> service.login(new AuthDtos.LoginRequest("Student@College.edu", "password123")));

		verify(jwt, never()).generate(user);
	}

	@Test
	void validOtpVerifiesAccountAndConsumesToken() {
		User user = mock(User.class);
		EmailVerificationToken token = mock(EmailVerificationToken.class);
		when(users.findByNormalizedEmail("student@college.edu")).thenReturn(Optional.of(user));
		when(user.getId()).thenReturn("user-7");
		when(user.isEmailVerified()).thenReturn(false);
		when(tokens.findByUserIdForUpdate("user-7")).thenReturn(Optional.of(token));
		when(token.getOtpExpiresAt()).thenReturn(Instant.now().plusSeconds(600));
		when(token.getFailedAttempts()).thenReturn(0);
		when(token.getOtpHash()).thenReturn("otp-hash");
		when(encoder.matches("123456", "otp-hash")).thenReturn(true);

		AuthDtos.VerificationResponse response = service.verifyOtp(
				new AuthDtos.OtpVerificationRequest("student@college.edu", "123456"));

		assertEquals(true, response.verified());
		verify(user).verifyEmail();
		verify(tokens).delete(token);
	}
}
