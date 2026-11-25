package com.cems.serviceImpl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoEmailClientTest {
	@Test
	void sendsHtmlAndTextThroughBrevoHttpsApi() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		BrevoEmailClient client = new BrevoEmailClient(builder.build(), "test-api-key", "sender@example.com",
				"Offbeat");
		server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("api-key", "test-api-key"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "sender": {"email": "sender@example.com", "name": "Offbeat"},
						  "to": [{"email": "recipient@example.com"}],
						  "subject": "Seat saved",
						  "textContent": "Text content",
						  "htmlContent": "<p>Saved</p>"
						}
						"""))
				.andRespond(withSuccess("{\"messageId\":\"test-id\"}", MediaType.APPLICATION_JSON));

		client.send("recipient@example.com", "Seat saved", "Text content", "<p>Saved</p>");

		server.verify();
	}
}
