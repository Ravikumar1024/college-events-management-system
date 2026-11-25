package com.cems.service;

import java.util.List;

import com.cems.entity.EventRegistration;
import com.cems.entity.Event;
import com.cems.entity.Student;

public interface RegistrationService {

	EventRegistration register(String eventId, String name);

	void cancel(String id, String name);

	List<EventRegistration> mine(String name);

	List<EventRegistration> forEvent(String eventId);

	EventRegistration approve(String id, String organizerEmail);

	EventRegistration reject(String id, String organizerEmail);

	EventRegistration markAttended(String id, String organizerEmail);

	EventRegistration registerFromInvitation(Event event, Student student);

}
