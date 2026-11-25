package com.cems.service;

import com.cems.dto.AuthDtos;
import com.cems.dto.AuthDtos.AuthResponse;

public interface AuthService {

	AuthDtos.RegistrationResponse register(AuthDtos.RegisterRequest r);

	AuthResponse login(AuthDtos.LoginRequest r);

	AuthDtos.VerificationResponse verifyOtp(AuthDtos.OtpVerificationRequest request);

	AuthDtos.VerificationResponse verifyActivationLink(AuthDtos.ActivationLinkRequest request);

	AuthDtos.RegistrationResponse resendVerification(AuthDtos.EmailRequest request);

}
