package com.cems.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.cems.dto.AuthDtos;
import com.cems.entity.EmailVerificationToken;
import com.cems.entity.Role;
import com.cems.entity.Student;
import com.cems.entity.User;
import com.cems.exceptionHandler.EmailNotVerifiedException;
import com.cems.repository.EmailVerificationTokenRepository;
import com.cems.repository.RoleRepository;
import com.cems.repository.StudentRepository;
import com.cems.repository.UserRepository;
import com.cems.security.JwtService;
import com.cems.service.AuthService;
import com.cems.service.EmailNotificationService;

@Service
public class AuthServiceImpl implements AuthService {
	private static final int MAX_OTP_ATTEMPTS = 5;
	private static final long RESEND_COOLDOWN_SECONDS = 60;
	private static final String PENDING_VERIFICATION_MESSAGE =
			"If this email has a pending account, a new verification email will be sent.";

	private final UserRepository users;
	private final RoleRepository roles;
	private final StudentRepository students;
	private final EmailVerificationTokenRepository verificationTokens;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final EmailNotificationService notifications;
	private final String frontendUrl;
	private final SecureRandom random = new SecureRandom();

	public AuthServiceImpl(UserRepository users, RoleRepository roles, StudentRepository students,
			EmailVerificationTokenRepository verificationTokens, PasswordEncoder encoder, JwtService jwt,
			EmailNotificationService notifications, @Value("${app.frontend-url}") String frontendUrl) {
		this.users = users;
		this.roles = roles;
		this.students = students;
		this.verificationTokens = verificationTokens;
		this.encoder = encoder;
		this.jwt = jwt;
		this.notifications = notifications;
		this.frontendUrl = frontendUrl.replaceAll("/+$", "");
	}

	@Override
	@Transactional
	public AuthDtos.RegistrationResponse register(AuthDtos.RegisterRequest request) {
		String email = normalizeEmail(request.email());
		String studentNumber = request.studentNumber().trim();
		String normalizedStudentNumber = normalizeStudentNumber(studentNumber);
		if (users.existsByNormalizedEmail(email)) {
			throw new IllegalArgumentException("Email is already registered.");
		}
		if (students.existsByNormalizedStudentNumber(normalizedStudentNumber)) {
			throw new IllegalArgumentException("Student ID is already registered.");
		}

		User user = new User(request.fullName().trim(), email, encoder.encode(request.password()));
		user.requireEmailVerification();
		user.addRole(roles.findByName("ROLE_STUDENT").orElseThrow());
		user = users.save(user);
		students.save(new Student(user, studentNumber));
		sendVerification(user, null);
		return new AuthDtos.RegistrationResponse(email,
				"Check your inbox for a one-time code or activation link to verify your email.");
	}

	@Override
	@Transactional(readOnly = true)
	public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
		User user = users.findByNormalizedEmail(normalizeEmail(request.email()))
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		if (!encoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
			throw new BadCredentialsException("Invalid email or password");
		}
		if (!user.isEmailVerified()) {
			throw new EmailNotVerifiedException();
		}
		return response(user);
	}

	@Override
	@Transactional
	public AuthDtos.VerificationResponse verifyOtp(AuthDtos.OtpVerificationRequest request) {
		User user = users.findByNormalizedEmail(normalizeEmail(request.email())).orElse(null);
		if (user == null) {
			return verificationFailure();
		}
		if (user.isEmailVerified()) {
			return new AuthDtos.VerificationResponse(true, "Email already verified. You can sign in.");
		}
		EmailVerificationToken token = verificationTokens.findByUserIdForUpdate(user.getId()).orElse(null);
		if (token == null || token.getOtpExpiresAt().isBefore(Instant.now())) {
			return new AuthDtos.VerificationResponse(false,
					"That code has expired. Request a new verification email.");
		}
		if (token.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
			return new AuthDtos.VerificationResponse(false,
					"Too many incorrect attempts. Request a new verification email.");
		}
		if (!encoder.matches(request.otp(), token.getOtpHash())) {
			token.recordFailedAttempt();
			return verificationFailure();
		}
		user.verifyEmail();
		verificationTokens.delete(token);
		return new AuthDtos.VerificationResponse(true, "Email verified. You can now sign in.");
	}

	@Override
	@Transactional
	public AuthDtos.VerificationResponse verifyActivationLink(AuthDtos.ActivationLinkRequest request) {
		String tokenHash = sha256(request.token());
		EmailVerificationToken verification = verificationTokens.findByActivationTokenHashForUpdate(tokenHash)
				.orElse(null);
		if (verification == null || verification.getActivationExpiresAt().isBefore(Instant.now())) {
			return new AuthDtos.VerificationResponse(false,
					"This activation link is invalid or expired. Request a new verification email.");
		}
		User user = verification.getUser();
		if (!user.isEnabled()) {
			return new AuthDtos.VerificationResponse(false,
					"This account is inactive. Contact your campus administrator.");
		}
		user.verifyEmail();
		verificationTokens.delete(verification);
		return new AuthDtos.VerificationResponse(true, "Email verified. You can now sign in.");
	}

	@Override
	@Transactional
	public AuthDtos.RegistrationResponse resendVerification(AuthDtos.EmailRequest request) {
		String email = normalizeEmail(request.email());
		User user = users.findByNormalizedEmail(email).orElse(null);
		if (user == null || user.isEmailVerified() || !user.isEnabled()) {
			return new AuthDtos.RegistrationResponse(email, PENDING_VERIFICATION_MESSAGE);
		}
		EmailVerificationToken existing = verificationTokens.findByUserIdForUpdate(user.getId()).orElse(null);
		if (existing != null && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(Instant.now())) {
			return new AuthDtos.RegistrationResponse(email,
					"Please wait a minute before requesting another verification email.");
		}
		sendVerification(user, existing);
		return new AuthDtos.RegistrationResponse(email, PENDING_VERIFICATION_MESSAGE);
	}

	private void sendVerification(User user, EmailVerificationToken existing) {
		String otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
		String activationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
		Instant now = Instant.now();
		Instant otpExpiresAt = now.plus(10, ChronoUnit.MINUTES);
		Instant activationExpiresAt = now.plus(24, ChronoUnit.HOURS);
		if (existing == null) {
			verificationTokens.save(new EmailVerificationToken(user, encoder.encode(otp), sha256(activationToken),
					otpExpiresAt, activationExpiresAt));
		} else {
			existing.replace(encoder.encode(otp), sha256(activationToken), otpExpiresAt, activationExpiresAt);
		}
		String activationUrl = frontendUrl + "/verify-email?token="
				+ UriUtils.encodeQueryParam(activationToken, StandardCharsets.UTF_8);
		notifications.emailVerification(user, otp, activationUrl);
	}

	private byte[] randomBytes(int size) {
		byte[] value = new byte[size];
		random.nextBytes(value);
		return value;
	}

	private String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private AuthDtos.VerificationResponse verificationFailure() {
		return new AuthDtos.VerificationResponse(false, "The email or verification code is incorrect.");
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeStudentNumber(String studentNumber) {
		return studentNumber.trim().toUpperCase(Locale.ROOT);
	}

	private AuthDtos.AuthResponse response(User user) {
		return new AuthDtos.AuthResponse(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), user.getId(),
				user.getFullName(), user.getEmail(), user.getRoles().stream().map(Role::getName).sorted().toList());
	}
}
