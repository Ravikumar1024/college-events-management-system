package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.cems.CollegeEventsManagementSystemApplication;
import com.cems.entity.User;
import com.cems.repository.RoleRepository;
import com.cems.repository.UserRepository;

@SpringBootTest(classes = CollegeEventsManagementSystemApplication.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:campus;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false" })
class CollegeEventsManagementSystemApplicationTests {
	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RoleRepository roles;

	@Autowired
	private UserRepository users;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void persistsUuidStringEntityIds() {
		String roleName = "ROLE_TEST_" + UUID.randomUUID().toString().substring(0, 8);
		jdbc.update("INSERT INTO roles (id, name) VALUES (?, ?)", UUID.randomUUID().toString(), roleName);
		User user = new User("UUID User", UUID.randomUUID() + "@college.edu", "encoded-password");
		user.addRole(roles.findByName(roleName).orElseThrow());

		User saved = users.saveAndFlush(user);

		assertTrue(saved.getId().matches(
				"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
	}

	@Test
	void cleanBrowserRoutesForwardToTheirPages() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.cems.controller.PageController()).build();
		mockMvc.perform(get("/events")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(forwardedUrl("/register.html"));
		mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/login.html"));
		mockMvc.perform(get("/my-events")).andExpect(status().isOk()).andExpect(forwardedUrl("/my-events.html"));
		mockMvc.perform(get("/organizer")).andExpect(status().isOk()).andExpect(forwardedUrl("/organizer.html"));
		mockMvc.perform(get("/dashboard")).andExpect(status().isOk()).andExpect(forwardedUrl("/dashboard.html"));
		mockMvc.perform(get("/admin/students")).andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin-students.html"));
	}

}
