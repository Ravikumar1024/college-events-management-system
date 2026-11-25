package com.cems.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cems.service.AdminStudentService;

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {
	private final AdminStudentService students;

	public AdminStudentController(AdminStudentService students) {
		this.students = students;
	}

	@GetMapping
	public List<AdminStudentService.StudentResponse> students() {
		return students.students();
	}

	@DeleteMapping("/{studentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable("studentId") String studentId) {
		students.deactivate(studentId);
	}
}
