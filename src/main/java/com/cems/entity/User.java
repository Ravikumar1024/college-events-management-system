package com.cems.entity;

import java.time.Instant;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID", length = 36, nullable = false)
	@Comment("Primary id of the table and acts as unique identifier.")
	@Setter(AccessLevel.NONE)
	private String id;
	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;
	@Column(nullable = false, unique = true, length = 180)
	private String email;
	@Column(name = "normalized_email", nullable = false, unique = true, length = 180)
	private String normalizedEmail;
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;
	@Column(nullable = false)
	private boolean enabled = true;
	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified = true;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Set<Role> roles = new HashSet<>();

	protected User() {
	}

	public User(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email.trim().toLowerCase(Locale.ROOT);
		this.normalizedEmail = this.email;
		this.passwordHash = passwordHash;
	}

	public void addRole(Role role) {
		roles.add(role);
	}

	public void requireEmailVerification() {
		emailVerified = false;
	}

	public void verifyEmail() {
		emailVerified = true;
	}

	public void deactivate() {
		enabled = false;
	}
}
