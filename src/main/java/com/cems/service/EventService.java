package com.cems.service;

import java.util.List;

import com.cems.dto.EventDtos;
import com.cems.dto.EventDtos.Response;
import com.cems.entity.EventCategory;

public interface EventService {

	List<Response> published();

	List<EventCategory> categories();

	Response get(String id);

	Response create(EventDtos.CreateRequest r, String name);

}
