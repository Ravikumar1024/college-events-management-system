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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "event_invitations", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "student_id" }))
public class EventInvitation {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_by_user_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User invitedBy;

	@Column(nullable = false, length = 20)
	private String status = "PENDING";

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "responded_at")
	private Instant respondedAt;

	protected EventInvitation() {
	}

	public EventInvitation(Event event, Student student, User invitedBy) {
		this.event = event;
		this.student = student;
		this.invitedBy = invitedBy;
	}

	public void accept() {
		status = "ACCEPTED";
		respondedAt = Instant.now();
	}

	public void decline() {
		status = "DECLINED";
		respondedAt = Instant.now();
	}

	public void inviteAgain(User invitedBy) {
		this.invitedBy = invitedBy;
		status = "PENDING";
		createdAt = Instant.now();
		respondedAt = null;
	}
}
