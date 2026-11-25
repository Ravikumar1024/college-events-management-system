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
@Table(name = "events")
public class Event {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;
	@Column(nullable = false, length = 200)
	private String title;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private EventCategory category;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organizer_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User organizer;
	@Column(nullable = false, length = 200)
	private String venue;
	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;
	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;
	@Column(name = "registration_deadline", nullable = false)
	private Instant registrationDeadline;
	private Integer capacity;
	@Column(nullable = false, length = 20)
	private String status = "DRAFT";
	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();
	@Column(name = "organizer_reminder_sent_at")
	private Instant organizerReminderSentAt;

	protected Event() {
	}

	public Event(String title, String description, EventCategory category, User organizer, String venue,
			Instant startsAt, Instant endsAt, Instant deadline, Integer capacity, String status) {
		this.title = title;
		this.description = description;
		this.category = category;
		this.organizer = organizer;
		this.venue = venue;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.registrationDeadline = deadline;
		this.capacity = capacity;
		this.status = status;
	}

	public void markOrganizerReminderSent() {
		organizerReminderSentAt = Instant.now();
	}
}
