package com.cems.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cems.entity.AdminInvitationToken;

import jakarta.persistence.LockModeType;

public interface AdminInvitationTokenRepository extends JpaRepository<AdminInvitationToken, String> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from AdminInvitationToken t join fetch t.user where t.user.id = :userId")
	Optional<AdminInvitationToken> findByUserIdForUpdate(@Param("userId") String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from AdminInvitationToken t join fetch t.user where t.tokenHash = :tokenHash")
	Optional<AdminInvitationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
