package com.cems.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cems.dto.AuthDtos;
import com.cems.service.AuthService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService service;

	public AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthDtos.RegistrationResponse register(@Valid @RequestBody AuthDtos.RegisterRequest r) {
		return service.register(r);
	}

	@PostMapping("/login")
	public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest r) {
		return service.login(r);
	}

	@PostMapping("/verify-otp")
	public AuthDtos.VerificationResponse verifyOtp(@Valid @RequestBody AuthDtos.OtpVerificationRequest request) {
		return service.verifyOtp(request);
	}

	@PostMapping("/verify-link")
	public AuthDtos.VerificationResponse verifyActivationLink(
			@Valid @RequestBody AuthDtos.ActivationLinkRequest request) {
		return service.verifyActivationLink(request);
	}

	@PostMapping("/resend-verification")
	public AuthDtos.RegistrationResponse resendVerification(@Valid @RequestBody AuthDtos.EmailRequest request) {
		return service.resendVerification(request);
	}
}
