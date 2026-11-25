package com.cems.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter filter;

	public SecurityConfig(JwtAuthenticationFilter filter) {
		this.filter = filter;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a.requestMatchers("/", "/events", "/login", "/register", "/verify-email", "/set-password", "/dashboard", "/my-events", "/admin/students",
								"/organizer", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/h2-console",
								"/h2-console/**", "/api/auth/**", "/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll().anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> writeSecurityError(response,
								HttpStatus.UNAUTHORIZED, "Please log in to continue. Your session may have expired."))
						.accessDeniedHandler((request, response, exception) -> writeSecurityError(response,
								HttpStatus.FORBIDDEN, "Your account does not have permission to perform this action.")))
				.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
	}

	private void writeSecurityError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
				+ ",\"error\":\"" + status.getReasonPhrase() + "\",\"details\":{\"message\":\"" + message + "\"}}");
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin) {
		CorsConfiguration c = new CorsConfiguration();
		c.setAllowedOrigins(List.of(origin));
		c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
		s.registerCorsConfiguration("/**", c);
		return s;
	}
}
