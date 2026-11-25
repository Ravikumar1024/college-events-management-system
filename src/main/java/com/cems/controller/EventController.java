package com.cems.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cems.dto.EventDtos;
import com.cems.entity.EventCategory;
import com.cems.service.EventService;
import com.cems.serviceImpl.EventServiceImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {
	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@GetMapping
	public List<EventDtos.Response> list() {
		return service.published();
	}

	@GetMapping("/categories")
	public List<EventCategory> categories() {
		return service.categories();
	}

	@GetMapping("/{id}")
	public EventDtos.Response get(@PathVariable("id") String id) {
		return service.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
	public EventDtos.Response create(@Valid @RequestBody EventDtos.CreateRequest r, Principal p) {
		return service.create(r, p.getName());
	}
}
