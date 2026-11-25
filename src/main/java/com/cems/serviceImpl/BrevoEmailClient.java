package com.cems.serviceImpl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BrevoEmailClient {
	private static final String EMAIL_API_URL = "https://api.brevo.com/v3/smtp/email";

	private final RestClient restClient;
	private final String apiKey;
	private final String senderEmail;
	private final String senderName;

	@Autowired
	public BrevoEmailClient(@Value("${app.mail.brevo-api-key}") String apiKey,
			@Value("${app.mail.from}") String senderEmail,
			@Value("${app.mail.from-name}") String senderName) {
		this(createRestClient(), apiKey, senderEmail, senderName);
	}

	BrevoEmailClient(RestClient restClient, String apiKey, String senderEmail, String senderName) {
		this.restClient = restClient;
		this.apiKey = apiKey;
		this.senderEmail = senderEmail;
		this.senderName = senderName;
	}

	public void send(String recipient, String subject, String textContent, String htmlContent) {
		var request = new SendEmailRequest(new Sender(senderEmail, senderName), List.of(new Recipient(recipient)),
				subject, textContent, htmlContent);
		restClient.post()
				.uri(EMAIL_API_URL)
				.header("api-key", apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity();
	}

	private static RestClient createRestClient() {
		var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(10));
		return RestClient.builder().requestFactory(requestFactory).build();
	}

	private record Sender(String email, String name) {
	}

	private record Recipient(String email) {
	}

	private record SendEmailRequest(Sender sender, List<Recipient> to, String subject, String textContent,
			String htmlContent) {
	}
}
