package com.cems.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cems.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	Optional<User> findByNormalizedEmail(String normalizedEmail);

	boolean existsByNormalizedEmail(String normalizedEmail);
}
