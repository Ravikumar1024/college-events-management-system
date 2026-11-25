//package com.cems.config;
//
//import org.h2.server.web.JakartaWebServlet;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.web.servlet.ServletRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
//public class H2ConsoleConfiguration {
//
//	@Bean
//	ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet(
//			@Value("${spring.h2.console.path:/h2-console}") String path,
//			@Value("${spring.h2.console.settings.web-allow-others:false}") boolean allowOthers) {
//		var registration = new ServletRegistrationBean<>(new JakartaWebServlet(), path + "/*");
//		registration.setName("H2Console");
//		registration.addInitParameter("webAllowOthers", Boolean.toString(allowOthers));
//		return registration;
//	}
//}
