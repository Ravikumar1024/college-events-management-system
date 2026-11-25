package com.cems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import org.hibernate.annotations.Comment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@Table(name = "students")
public class Student {
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
	@Column(name = "student_number", nullable = false, unique = true, length = 40)
	private String studentNumber;
	@Column(name = "normalized_student_number", nullable = false, unique = true, length = 40)
	private String normalizedStudentNumber;
	@Column(name = "department_id")
	private String departmentId;
	@Column(name = "academic_year")
	private Short academicYear;

	protected Student() {
	}

	public Student(User user, String studentNumber) {
		this.user = user;
		this.studentNumber = studentNumber.trim();
		this.normalizedStudentNumber = this.studentNumber.toUpperCase(Locale.ROOT);
	}

}
