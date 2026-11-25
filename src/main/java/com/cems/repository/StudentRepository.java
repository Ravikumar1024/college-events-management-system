package com.cems.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cems.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
	Optional<Student> findByUserEmailIgnoreCase(String email);

	boolean existsByStudentNumber(String studentNumber);

	boolean existsByNormalizedStudentNumber(String normalizedStudentNumber);

	@Query("select s from Student s join fetch s.user order by s.user.fullName")
	List<Student> findAllWithUsers();

	@Query("""
			select distinct s from Student s join fetch s.user u
			where u.enabled = true and u.emailVerified = true
			  and exists (select r.id from EventRegistration r
				where r.student = s and lower(r.event.organizer.email) = lower(:organizerEmail))
			order by u.fullName
			""")
	List<Student> findInviteCandidates(@Param("organizerEmail") String organizerEmail);

	@Query("select s from Student s join fetch s.user u where u.enabled = true and u.emailVerified = true order by u.fullName")
	List<Student> findAllInviteCandidates();

	@Query("""
			select s.id from Student s
			where s.id in :studentIds and s.user.enabled = true and s.user.emailVerified = true
			  and exists (select r.id from EventRegistration r
				where r.student = s and lower(r.event.organizer.email) = lower(:organizerEmail))
			""")
	List<String> findAccessibleStudentIds(@Param("organizerEmail") String organizerEmail,
			@Param("studentIds") List<String> studentIds);
}
