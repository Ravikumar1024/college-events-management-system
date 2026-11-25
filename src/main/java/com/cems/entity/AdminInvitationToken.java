package com.cems.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "admin_invitation_tokens")
public class AdminInvitationToken {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected AdminInvitationToken() {
	}

	public AdminInvitationToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		replace(tokenHash, expiresAt);
	}

	public void replace(String tokenHash, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}
}
