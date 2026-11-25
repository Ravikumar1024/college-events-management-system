package com.cems.exceptionHandler;

public class EmailNotVerifiedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailNotVerifiedException() {
		super("Verify your email using the one-time code or activation link before signing in.");
	}
}
