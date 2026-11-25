package com.cems.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "notifications")
public class AppNotification {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_user_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User recipient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Event event;

	@Column(nullable = false, length = 40)
	private String type;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "reference_id")
	private String referenceId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "read_at")
	private Instant readAt;

	protected AppNotification() {
	}

	public AppNotification(User recipient, Event event, String type, String title, String message, String referenceId) {
		this.recipient = recipient;
		this.event = event;
		this.type = type;
		this.title = title;
		this.message = message;
		this.referenceId = referenceId;
	}

	public void markRead() {
		if (readAt == null) readAt = Instant.now();
	}
}
