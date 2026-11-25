package com.cems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cems.entity.EventCategory;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, String> {
}
