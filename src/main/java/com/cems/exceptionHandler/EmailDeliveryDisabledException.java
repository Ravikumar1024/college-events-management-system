package com.cems.exceptionHandler;

public class EmailDeliveryDisabledException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailDeliveryDisabledException() {
		super("Email notifications must be enabled to invite a new administrator.");
	}
}
