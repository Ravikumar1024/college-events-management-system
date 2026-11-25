package com.cems.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cems.entity.EmailVerificationToken;

import jakarta.persistence.LockModeType;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {
	Optional<EmailVerificationToken> findByUserId(String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from EmailVerificationToken t join fetch t.user where t.user.id = :userId")
	Optional<EmailVerificationToken> findByUserIdForUpdate(@Param("userId") String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from EmailVerificationToken t join fetch t.user where t.activationTokenHash = :hash")
	Optional<EmailVerificationToken> findByActivationTokenHashForUpdate(@Param("hash") String hash);

	void deleteByUserId(String userId);
}
