package com.cems.serviceImpl;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.dto.EventDtos;
import com.cems.entity.Event;
import com.cems.entity.EventCategory;
import com.cems.repository.EventCategoryRepository;
import com.cems.repository.EventRepository;
import com.cems.repository.UserRepository;
import com.cems.service.EventService;

@Service
public class EventServiceImpl implements EventService{
	private final EventRepository events;
	private final EventCategoryRepository categories;
	private final UserRepository users;

	public EventServiceImpl(EventRepository events, EventCategoryRepository categories, UserRepository users) {
		this.events = events;
		this.categories = categories;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<EventDtos.Response> published() {
		return events.findByStatusOrderByStartsAtAsc("PUBLISHED").stream().map(this::dto).toList();
	}

	@Transactional(readOnly = true)
	public List<EventDtos.Response> organizedBy(String email) {
		boolean admin = users.findByEmailIgnoreCase(email).orElseThrow().getRoles().stream()
				.anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
		var results = admin ? events.findAll(org.springframework.data.domain.Sort.by("startsAt").descending())
				: events.findByOrganizerEmailIgnoreCaseOrderByStartsAtDesc(email);
		return results.stream().map(this::dto).toList();
	}

	@Transactional(readOnly = true)
	public List<EventCategory> categories() {
		return categories.findAll(org.springframework.data.domain.Sort.by("name"));
	}

	@Transactional(readOnly = true)
	public Event requireOrganizer(String id, String email) {
		Event event = events.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));
		boolean admin = users.findByEmailIgnoreCase(email).orElseThrow().getRoles().stream()
				.anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
		if (!admin && !event.getOrganizer().getEmail().equalsIgnoreCase(email))
			throw new AccessDeniedException("You cannot manage this event");
		return event;
	}

	@Transactional(readOnly = true)
	public EventDtos.Response get(String id) {
		return dto(events.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found")));
	}

	@Transactional
	public EventDtos.Response create(EventDtos.CreateRequest r, String email) {
		validate(r);
		var u = users.findByEmailIgnoreCase(email).orElseThrow();
		var c = categories.findById(r.categoryId())
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
		return dto(events.save(new Event(r.title().trim(), r.description().trim(), c, u, r.venue().trim(), r.startsAt(),
				r.endsAt(), r.registrationDeadline(), r.capacity(), r.status())));
	}

	private void validate(EventDtos.CreateRequest r) {
		if (!r.endsAt().isAfter(r.startsAt()))
			throw new IllegalArgumentException("Event end must be after start");
		if (r.registrationDeadline().isAfter(r.startsAt()))
			throw new IllegalArgumentException("Registration deadline must be before event start");
		if (!List.of("DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED").contains(r.status()))
			throw new IllegalArgumentException("Invalid event status");
		if ("PUBLISHED".equals(r.status()) && !r.startsAt().isAfter(Instant.now()))
			throw new IllegalArgumentException("Published event must be in the future");
		if ("PUBLISHED".equals(r.status()) && !r.registrationDeadline().isAfter(Instant.now()))
			throw new IllegalArgumentException("Registration deadline must be in the future");
	}

	private EventDtos.Response dto(Event e) {
		return new EventDtos.Response(e.getId(), e.getTitle(), e.getDescription(), e.getCategory().getId(),
				e.getCategory().getName(), e.getOrganizer().getId(), e.getOrganizer().getFullName(), e.getVenue(),
				e.getStartsAt(), e.getEndsAt(), e.getRegistrationDeadline(), e.getCapacity(), e.getStatus());
	}
}
