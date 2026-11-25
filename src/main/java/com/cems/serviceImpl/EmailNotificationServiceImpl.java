package com.cems.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cems.entity.Event;
import com.cems.entity.EventRegistration;
import com.cems.entity.User;
import com.cems.service.EmailNotificationService;

import jakarta.mail.MessagingException;

@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {
	private static final DateTimeFormatter EVENT_TIME_FORMAT =
			DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a z").withZone(ZoneId.systemDefault());

	private final JavaMailSender mailSender;
	private final String from;
	private final String frontendUrl;
	private final boolean enabled;

	public EmailNotificationServiceImpl(JavaMailSender mailSender, @Value("${app.mail.from}") String from,
			@Value("${app.frontend-url}") String frontendUrl,
			@Value("${notification.email.enabled:true}") boolean enabled) {
		this.mailSender = mailSender;
		this.from = from;
		this.frontendUrl = frontendUrl.replaceAll("/+$", "");
		this.enabled = enabled;
	}

	@Override
	public void emailVerification(User user, String otp, String activationUrl) {
		String plainText = "Hi " + user.getFullName() + ",\n\n"
				+ "Verify your Offbeat account with this one-time code: " + otp + "\n"
				+ "The code expires in 10 minutes. You can also use this activation link within 24 hours:\n"
				+ activationUrl + "\n\nIf you did not create this account, you can ignore this email.";
		String details = "<div style=\"margin-top:22px;padding:20px;background:#dce99a;border:1px solid #1b3128;\">"
				+ "<p style=\"margin:0 0 8px;color:#53634f;font-size:11px;font-weight:bold;letter-spacing:1px;\">"
				+ "YOUR ONE-TIME CODE</p><p style=\"margin:0;color:#1b3128;font-size:30px;font-weight:bold;"
				+ "letter-spacing:8px;\">" + escapeHtml(otp) + "</p>"
				+ "<p style=\"margin:10px 0 0;color:#53634f;font-size:12px;\">Expires in 10 minutes. The activation link is valid for 24 hours.</p></div>";
		send(user.getEmail(), "Verify your Offbeat account", plainText,
				emailHtml("Email verification", "One quick step", "Use your one-time code or activate your account from the link below.",
						details, activationUrl, "Activate my account"));
	}

	@Override
	public void eventInvitation(User user, Event event, String invitationUrl) {
		String plainText = "Hi " + user.getFullName() + ",\n\n"
				+ event.getOrganizer().getFullName() + " invited you to " + event.getTitle() + ".\n\n"
				+ eventDetailsText(event) + "\n\nView your invitation: " + invitationUrl
				+ "\n\nSee you there,\nOffbeat";
		String details = eventDetailsHtml(event)
				+ detailRow("Invited by", event.getOrganizer().getFullName());
		send(user.getEmail(), "You’re invited: " + event.getTitle(), plainText,
				emailHtml("A campus invitation", "You’re on the list", "You’ve been invited to join " + event.getTitle() + ".",
						details, invitationUrl, "View invitation"));
	}

	@Override
	public void registrationConfirmed(EventRegistration registration) {
		Event event = registration.getEvent();
		User organizer = event.getOrganizer();
		String plansUrl = frontendUrl + "/my-events";
		String plainText = "Hi " + registration.getStudent().getUser().getFullName() + ",\n\n"
				+ "Your seat is saved for " + event.getTitle() + ".\n\n"
				+ eventDetailsText(event) + "\n"
				+ "Hosted by: " + organizer.getFullName() + " (" + organizer.getEmail() + ")\n\n"
				+ "Manage your plans: " + plansUrl + "\n\nSee you there,\nOffbeat";
		String details = eventDetailsHtml(event)
				+ detailRow("Host", organizer.getFullName() + " · " + organizer.getEmail());
		send(registration.getStudent().getUser().getEmail(), "Your seat is saved: " + event.getTitle(), plainText,
				emailHtml("Registration confirmed", event.getTitle(),
						"Hi " + registration.getStudent().getUser().getFullName() + ", your seat is saved.",
						details, plansUrl, "View my plans"));
	}

	@Override
	public void registrationCancelled(EventRegistration registration) {
		Event event = registration.getEvent();
		String eventsUrl = frontendUrl + "/events";
		String plainText = "Your spot for " + event.getTitle() + " has been released.\n\n"
				+ eventDetailsText(event) + "\n\nFind another campus plan: " + eventsUrl + "\n\n"
				+ "Hope to see you at another Offbeat event.";
		send(registration.getStudent().getUser().getEmail(), "Your spot is released: " + event.getTitle(), plainText,
				emailHtml("Registration cancelled", "Your spot is released",
						"Your registration for " + event.getTitle() + " has been cancelled.",
						eventDetailsHtml(event), eventsUrl, "Find another event"));
	}

	@Override
	public boolean eventReminder(EventRegistration registration) {
		Event event = registration.getEvent();
		User organizer = event.getOrganizer();
		String plansUrl = frontendUrl + "/my-events";
		String plainText = "Hi " + registration.getStudent().getUser().getFullName() + ",\n\n"
				+ event.getTitle() + " starts in about 30 minutes. Your seat is saved.\n\n"
				+ eventDetailsText(event) + "\n"
				+ "Hosted by: " + organizer.getFullName() + " (" + organizer.getEmail() + ")\n\n"
				+ "View your plans: " + plansUrl + "\n\nSee you soon,\nOffbeat";
		String details = eventDetailsHtml(event)
				+ detailRow("Host", organizer.getFullName() + " · " + organizer.getEmail());
		return send(registration.getStudent().getUser().getEmail(), "Starting in 30 minutes: " + event.getTitle(),
				plainText, emailHtml("A little heads-up", "You’re on in 30 minutes",
						"Hi " + registration.getStudent().getUser().getFullName()
								+ ", " + event.getTitle() + " is almost here. Your seat is saved.",
						details, plansUrl, "View my plans"));
	}

	@Override
	public boolean organizerEventReminder(Event event, List<EventRegistration> registrations) {
		User organizer = event.getOrganizer();
		String attendeeText = registrations.stream()
				.map(registration -> "- " + registration.getStudent().getUser().getFullName() + " <"
						+ registration.getStudent().getUser().getEmail() + ">")
				.reduce((first, next) -> first + "\n" + next)
				.orElse("No active registrations.");
		String attendeeHtml = registrations.stream()
				.map(registration -> "<tr><td style=\"padding:11px 8px;border-bottom:1px solid #d7dfd4;font-weight:bold;\">"
						+ escapeHtml(registration.getStudent().getUser().getFullName())
						+ "</td><td style=\"padding:11px 8px;border-bottom:1px solid #d7dfd4;color:#6c7b70;\">"
						+ escapeHtml(registration.getStudent().getUser().getEmail()) + "</td></tr>")
				.reduce((first, next) -> first + next)
				.orElse("<tr><td colspan=\"2\" style=\"padding:11px 8px;\">No active registrations.</td></tr>");
		String rosterHtml = "<div style=\"margin-top:20px;padding:20px;background:#fffef9;border:1px solid #1b3128;\">"
				+ "<p style=\"margin:0 0 12px;color:#8b5a15;font-size:11px;font-weight:bold;letter-spacing:1.5px;\">"
				+ "THE GUEST LIST · " + registrations.size() + " "
				+ (registrations.size() == 1 ? "STUDENT" : "STUDENTS") + "</p>"
				+ "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
				+ attendeeHtml + "</table></div>";
		String plainText = "Hi " + organizer.getFullName() + ",\n\n"
				+ "Your event starts in about 30 minutes.\n\n" + eventDetailsText(event)
				+ "\n\nRegistered attendees (" + registrations.size() + "):\n" + attendeeText
				+ "\n\nView your event dashboard: " + frontendUrl + "/organizer\n\n"
				+ "Good luck,\nOffbeat";
		String details = eventDetailsHtml(event) + rosterHtml;
		return send(organizer.getEmail(), "Your event starts in 30 minutes: " + event.getTitle(), plainText,
				emailHtml("Host reminder", "Your event is almost here",
						"Hi " + organizer.getFullName() + ", your event starts in about 30 minutes.",
						details, frontendUrl + "/organizer", "Open host desk"));
	}

	private boolean send(String recipient, String subject, String plainText, String html) {
		if (!enabled) return false;
		var message = mailSender.createMimeMessage();
		try {
			var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(from);
			helper.setTo(recipient);
			helper.setSubject(subject);
			helper.setText(plainText, html);
		} catch (MessagingException exception) {
			throw new IllegalStateException("Could not build the event email.", exception);
		}
		mailSender.send(message);
		return true;
	}

	private String eventDetailsText(Event event) {
		return "When: " + EVENT_TIME_FORMAT.format(event.getStartsAt()) + " – "
				+ EVENT_TIME_FORMAT.format(event.getEndsAt()) + "\nWhere: " + event.getVenue();
	}

	private String eventDetailsHtml(Event event) {
		return "<div style=\"margin-top:22px;padding:20px;background:#dce99a;border:1px solid #1b3128;\">"
				+ "<p style=\"margin:0 0 12px;color:#1b3128;font-size:11px;font-weight:bold;letter-spacing:1.5px;\">"
				+ "THE PLAN / FIELD NOTES</p>"
				+ detailRow("When", EVENT_TIME_FORMAT.format(event.getStartsAt()) + " – "
						+ EVENT_TIME_FORMAT.format(event.getEndsAt()))
				+ detailRow("Where", event.getVenue()) + "</div>";
	}

	private String detailRow(String label, String value) {
		return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
				+ "<tr><td width=\"78\" style=\"padding:7px 8px 7px 0;color:#53634f;font-size:11px;font-weight:bold;"
				+ "letter-spacing:1px;text-transform:uppercase;vertical-align:top;\">"
				+ escapeHtml(label) + "</td><td style=\"padding:7px 0;color:#1b3128;font-size:14px;line-height:1.5;\">"
				+ escapeHtml(value) + "</td></tr></table>";
	}

	private String emailHtml(String eyebrow, String heading, String intro, String details, String actionUrl,
			String actionLabel) {
		return """
				<!doctype html>
				<html lang="en">
				<head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
				<body style="margin:0;background:#f4f3ea;color:#1b3128;font-family:Arial,Helvetica,sans-serif;">
				<div style="display:none!important;visibility:hidden;opacity:0;color:transparent;height:0;width:0;overflow:hidden;">%s</div>
				<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f4f3ea;">
				  <tr><td align="center" style="padding:28px 14px;">
				    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:620px;">
				      <tr><td style="padding:0 4px 18px;">
				        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
				          <tr>
				            <td width="38" valign="middle"><span style="display:inline-block;width:30px;height:30px;line-height:30px;text-align:center;background:#dce99a;border:1px solid #1b3128;font-size:20px;">✳</span></td>
				            <td valign="middle" style="color:#1b3128;font-size:15px;font-weight:bold;letter-spacing:1px;">OFFBEAT.<br><span style="font-size:9px;font-weight:normal;letter-spacing:1.5px;color:#6c7b70;">CAMPUS, OFF SCRIPT</span></td>
				            <td align="right" valign="middle" style="color:#6c7b70;font-size:9px;letter-spacing:1.5px;">FIELD NOTE<br>NO. 01</td>
				          </tr>
				        </table>
				      </td></tr>
				      <tr><td style="padding:0 0 14px;">
				        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#244e3c;border:1px solid #1b3128;">
				          <tr><td style="padding:30px 28px 32px;">
				            <p style="margin:0 0 13px;color:#dce99a;font-size:11px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;">%s</p>
				            <h1 style="margin:0 0 13px;color:#fffef9;font-size:34px;line-height:1.08;letter-spacing:-1px;">%s</h1>
				            <p style="margin:0;color:#e2ece4;font-size:15px;line-height:1.65;">%s</p>
				          </td><td width="70" align="center" valign="bottom" style="padding:0 18px 18px 0;color:#dce99a;font-size:56px;">✳</td></tr>
				        </table>
				      </td></tr>
				      <tr><td style="padding:24px 26px;background:#fffef9;border:1px solid #1b3128;">
				        %s
				        <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin-top:24px;">
				          <tr><td style="background:#1b3128;padding:0 4px 4px 0;">
				            <a href="%s" style="display:inline-block;background:#dce99a;border:1px solid #1b3128;color:#1b3128;text-decoration:none;font-size:14px;font-weight:bold;padding:13px 18px;">%s&nbsp; ↗</a>
				          </td></tr>
				        </table>
				      </td></tr>
				      <tr><td align="center" style="padding:18px 10px;color:#6c7b70;font-size:11px;line-height:1.6;">
				        YOUR CAMPUS. NO RERUNS.<br>Show up curious. Leave with a story.
				      </td></tr>
				    </table>
				  </td></tr>
				</table>
				</body></html>
				""".formatted(escapeHtml(intro), escapeHtml(eyebrow), escapeHtml(heading), escapeHtml(intro),
				details, escapeHtml(actionUrl), escapeHtml(actionLabel));
	}

	private String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
