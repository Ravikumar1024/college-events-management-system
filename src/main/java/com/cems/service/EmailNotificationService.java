package com.cems.service;

import java.util.List;

import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.entity.User;

public interface EmailNotificationService {
	void emailVerification(User user, String otp, String activationUrl);

	void adminInvitation(User user, String setupUrl);

	void eventInvitation(User user, Event event, String invitationUrl);

	void registrationConfirmed(EventRegistration registration);

	void registrationCancelled(EventRegistration registration);

	boolean eventReminder(EventRegistration registration);

	boolean organizerEventReminder(Event event, List<EventRegistration> registrations);

}
