package com.cems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cems.entity.Event;
import jakarta.persistence.LockModeType;


@Repository
public interface EventRepository extends JpaRepository<Event, String> {
	List<Event> findByStatusOrderByStartsAtAsc(String status);

	List<Event> findByOrganizerEmailIgnoreCaseOrderByStartsAtDesc(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Event e where e.id = :eventId")
	java.util.Optional<Event> findByIdForUpdate(@Param("eventId") String eventId);
}
