package com.cems.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cems.entity.User;
import com.cems.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwt;
	private final UserRepository users;

	public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
		this.jwt = jwt;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			try {
				String email = jwt.subject(header.substring(7));
				User user = users.findByEmailIgnoreCase(email).orElse(null);
				if (user != null && user.isEnabled()
						&& SecurityContextHolder.getContext().getAuthentication() == null) {
					var authorities = user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName()))
							.toList();
					SecurityContextHolder.getContext().setAuthentication(
							new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities));
				}
			} catch (RuntimeException ex) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}
}
