package com.cems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cems.entity.EventInvitation;

import jakarta.persistence.LockModeType;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, String> {
	Optional<EventInvitation> findByEventIdAndStudentId(String eventId, String studentId);

	@Query("""
			select i from EventInvitation i join fetch i.event e
			where lower(i.student.user.email) = lower(:email)
			order by i.createdAt desc
			""")
	List<EventInvitation> findByStudentEmail(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from EventInvitation i where i.id = :invitationId")
	Optional<EventInvitation> findByIdForUpdate(@Param("invitationId") String invitationId);
}
