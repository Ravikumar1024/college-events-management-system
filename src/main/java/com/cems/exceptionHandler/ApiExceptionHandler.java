package com.cems.exceptionHandler;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
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

	@ExceptionHandler(MailException.class)
	public ResponseEntity<ApiError> mailFailure(MailException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError(Instant.now(), 503, "Email Delivery Failed",
						Map.of("message", "The email could not be sent. Check SMTP configuration and try again.")));
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
