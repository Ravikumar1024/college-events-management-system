package com.cems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Entity
@Table(name = "event_categories")
public class EventCategory {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;
	@Column(nullable = false, unique = true, length = 80)
	private String name;

	protected EventCategory() {
	}

}
