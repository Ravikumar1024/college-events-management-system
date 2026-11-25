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
@Table(name = "event_registrations", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "student_id" }))
public class EventRegistration {
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
	@Column(nullable = false, length = 20)
	private String status = "PENDING_APPROVAL";
	@Column(name = "registered_at", nullable = false)
	private Instant registeredAt = Instant.now();
	@Column(name = "reminder_sent_at")
	private Instant reminderSentAt;

	protected EventRegistration() {
	}

	public EventRegistration(Event e, Student s) {
		event = e;
		student = s;
	}

	public void cancel() {
		status = "CANCELLED";
	}

	public void markAttended() {
		status = "ATTENDED";
	}

	public void reactivate() {
		status = "PENDING_APPROVAL";
		registeredAt = Instant.now();
		reminderSentAt = null;
	}

	public void approve() {
		status = "REGISTERED";
		reminderSentAt = null;
	}

	public void reject() {
		status = "REJECTED";
		reminderSentAt = null;
	}

	public void markReminderSent() {
		reminderSentAt = Instant.now();
	}
}
