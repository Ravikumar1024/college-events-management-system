package com.cems.exceptionHandler;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final Pattern EMAIL_ADDRESS = Pattern.compile(
			"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
	private static final Pattern CREDENTIAL = Pattern.compile(
			"(?i)(password|secret|token|username|api[-_]?key)(\\s*[=:]\\s*)[^\\s,;]+");

	public record ApiError(Instant timestamp, int status, String error, Map<String, String> details) {
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> badCredentials(BadCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(Instant.now(), 401, "Unauthorized",
						Map.of("message", "Email or password is incorrect.")));
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	public ResponseEntity<ApiError> emailNotVerified(EmailNotVerifiedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(Instant.now(), 403, "Email Not Verified", Map.of("message", e.getMessage())));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> accessDenied(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(Instant.now(), 403, "Forbidden",
						Map.of("message", "Your account does not have permission to perform this action.")));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest()
				.body(new ApiError(Instant.now(), 400, "Bad Request", Map.of("message", e.getMessage())));
	}

	@ExceptionHandler(RestClientException.class)
	public ResponseEntity<ApiError> emailApiFailure(RestClientException e) {
		LOGGER.error("Email delivery failed: {}", mailFailureSummary(e));
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError(Instant.now(), 503, "Email Delivery Failed",
						Map.of("message", "The email could not be sent. Check Brevo API configuration and try again.")));
	}

	@ExceptionHandler(EmailDeliveryDisabledException.class)
	public ResponseEntity<ApiError> emailDeliveryDisabled(EmailDeliveryDisabledException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError(Instant.now(), 503, "Email Delivery Disabled",
						Map.of("message", "Enable email notifications to send administrator invitations.")));
	}

	private String mailFailureSummary(RestClientException exception) {
		Throwable cause = exception;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		String message = cause.getMessage();
		if (message == null || message.isBlank()) {
			return cause.getClass().getSimpleName();
		}
		String sanitized = EMAIL_ADDRESS.matcher(message).replaceAll("[email redacted]");
		sanitized = CREDENTIAL.matcher(sanitized).replaceAll("$1$2[redacted]");
		return cause.getClass().getSimpleName() + ": " + sanitized;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(Instant.now(), 409, "Data Conflict",
						Map.of("message", "This operation conflicts with existing data. Refresh and try again.")));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
		var errors = e.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(x -> x.getField(),
				x -> x.getDefaultMessage() == null ? "Invalid" : x.getDefaultMessage(), (a, b) -> a));
		return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Validation Failed", errors));
	}
}
