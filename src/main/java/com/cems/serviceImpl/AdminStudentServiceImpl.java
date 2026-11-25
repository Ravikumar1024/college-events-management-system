package com.cems.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cems.entity.Student;
import com.cems.repository.StudentRepository;
import com.cems.service.AdminStudentService;

@Service
public class AdminStudentServiceImpl implements AdminStudentService {
	private final StudentRepository students;

	public AdminStudentServiceImpl(StudentRepository students) {
		this.students = students;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentResponse> students() {
		return students.findAllWithUsers().stream().map(this::response).toList();
	}

	@Override
	@Transactional
	public void deactivate(String studentId) {
		Student student = students.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("Student account not found."));
		boolean hasPrivilegedRole = student.getUser().getRoles().stream()
				.anyMatch(role -> role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_ORGANIZER"));
		if (hasPrivilegedRole) {
			throw new IllegalArgumentException(
					"Accounts with organizer or administrator access cannot be deactivated from student management.");
		}
		student.getUser().deactivate();
	}

	private StudentResponse response(Student student) {
		var user = student.getUser();
		return new StudentResponse(student.getId(), user.getFullName(), user.getEmail(), student.getStudentNumber(),
				user.isEnabled(), user.isEmailVerified(), user.getCreatedAt());
	}
}
