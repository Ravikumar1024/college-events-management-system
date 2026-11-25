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
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
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

	@Column(name = "otp_hash", nullable = false, length = 100)
	private String otpHash;

	@Column(name = "activation_token_hash", nullable = false, length = 64)
	private String activationTokenHash;

	@Column(name = "otp_expires_at", nullable = false)
	private Instant otpExpiresAt;

	@Column(name = "activation_expires_at", nullable = false)
	private Instant activationExpiresAt;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "last_sent_at", nullable = false)
	private Instant lastSentAt;

	protected EmailVerificationToken() {
	}

	public EmailVerificationToken(User user, String otpHash, String activationTokenHash, Instant otpExpiresAt,
			Instant activationExpiresAt) {
		this.user = user;
		replace(otpHash, activationTokenHash, otpExpiresAt, activationExpiresAt);
	}

	public void replace(String otpHash, String activationTokenHash, Instant otpExpiresAt,
			Instant activationExpiresAt) {
		this.otpHash = otpHash;
		this.activationTokenHash = activationTokenHash;
		this.otpExpiresAt = otpExpiresAt;
		this.activationExpiresAt = activationExpiresAt;
		this.failedAttempts = 0;
		this.lastSentAt = Instant.now();
	}

	public void recordFailedAttempt() {
		failedAttempts++;
	}
}
