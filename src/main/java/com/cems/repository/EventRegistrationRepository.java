package com.cems.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cems.entity.EventRegistration;


@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
	Optional<EventRegistration> findByEventIdAndStudentId(String eventId, String studentId);

	long countByEventIdAndStatusIn(String eventId, Collection<String> statuses);

	@Query("select r from EventRegistration r join fetch r.event where r.student.id = :studentId order by r.registeredAt desc")
	List<EventRegistration> findByStudentIdOrderByRegisteredAtDesc(@Param("studentId") String studentId);

	@Query("select r from EventRegistration r join fetch r.student s join fetch s.user where r.event.id = :eventId order by r.registeredAt")
	List<EventRegistration> findByEventIdOrderByRegisteredAtAsc(@Param("eventId") String eventId);

	@Query("select r from EventRegistration r join fetch r.event e join fetch r.student s join fetch s.user where r.status = 'REGISTERED' and r.reminderSentAt is null and e.status = 'PUBLISHED' and e.startsAt > :from and e.startsAt <= :to")
	List<EventRegistration> findRemindersDue(@Param("from") Instant from, @Param("to") Instant to);
}
