package com.cems.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

	@GetMapping({ "/events" })
	public String events() {
		return "forward:/index.html";
	}

	@GetMapping({ "/","/login" })
	public String login() {
		return "forward:/login.html";
	}

	@GetMapping("/register")
	public String register() {
		return "forward:/register.html";
	}

	@GetMapping("/verify-email")
	public String verifyEmail() {
		return "forward:/verify-email.html";
	}

	@GetMapping("/set-password")
	public String setPassword() {
		return "forward:/set-password.html";
	}

	@GetMapping("/my-events")
	public String myEvents() {
		return "forward:/my-events.html";
	}

	@GetMapping("/organizer")
	public String organizer() {
		return "forward:/organizer.html";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "forward:/dashboard.html";
	}

	@GetMapping("/admin/students")
	public String adminStudents() {
		return "forward:/admin-students.html";
	}
}
