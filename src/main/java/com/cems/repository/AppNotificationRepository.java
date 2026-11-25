package com.cems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cems.entity.AppNotification;

public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {
	@Query("""
			select n from AppNotification n left join fetch n.event
			where lower(n.recipient.email) = lower(:email)
			order by n.createdAt desc
			""")
	List<AppNotification> findForRecipient(@Param("email") String email);

	@Query("""
			select n from AppNotification n
			where n.id = :id and lower(n.recipient.email) = lower(:email)
			""")
	Optional<AppNotification> findByIdAndRecipientEmail(@Param("id") String id, @Param("email") String email);

	@Query("""
			select n from AppNotification n
			where n.referenceId = :referenceId and n.type = :type
			  and lower(n.recipient.email) = lower(:email)
			""")
	List<AppNotification> findForReference(@Param("referenceId") String referenceId, @Param("email") String email,
			@Param("type") String type);
}
