package com.incident.notification_service.service;

import com.incident.notification_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.channels.email.enabled}")
    private boolean enabled;

    @Value("${notification.channels.email.from}")
    private String fromEmail;

    @Value("${notification.channels.email.to-addresses}")
    private String toAddresses;

    @Value("${notification.templates.subject}")
    private String subjectTemplate;

    @Override
    public void sendNotification(Alert alert) throws Exception {
        if (!isEnabled()) {
            log.debug("Email notifications are disabled");
            return;
        }

        List<String> recipients = parseRecipients(toAddresses);
        if (recipients.isEmpty()) {
            log.warn("No email recipients configured");
            return;
        }

        String subject = generateSubject(alert);
        String body = generateEmailBody(alert);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(recipients.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(body, true); // true indicates HTML content

        mailSender.send(message);
        
        log.info("Email notification sent for alert: {} to {} recipients", 
            alert.getAlertId(), recipients.size());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    private List<String> parseRecipients(String addresses) {
        if (addresses == null || addresses.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.asList(addresses.split(","))
                .stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toList();
    }

    private String generateSubject(Alert alert) {
        return subjectTemplate
                .replace("{{ severity }}", alert.getSeverity().toString())
                .replace("{{ title }}", alert.getTitle())
                .replace("{{ serviceName }}", alert.getServiceName());
    }

    private String generateEmailBody(Alert alert) {
        Context context = new Context();
        context.setVariable("alert", alert);
        context.setVariable("alertId", alert.getAlertId());
        context.setVariable("serviceName", alert.getServiceName());
        context.setVariable("severity", alert.getSeverity().toString());
        context.setVariable("title", alert.getTitle());
        context.setVariable("description", alert.getDescription());
        context.setVariable("anomalyScore", String.format("%.3f", alert.getAnomalyScore()));
        context.setVariable("createdAt", alert.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        context.setVariable("hostname", alert.getHostname());
        context.setVariable("podName", alert.getPodName());
        context.setVariable("anomalyReasons", alert.getAnomalyReasons());
        context.setVariable("tags", alert.getTags());

        try {
            return templateEngine.process("email-alert", context);
        } catch (Exception e) {
            log.warn("Failed to process email template, using fallback", e);
            return generateFallbackEmailBody(alert);
        }
    }

    private String generateFallbackEmailBody(Alert alert) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<h2>Alert Notification</h2>");
        body.append("<table border='1' cellpadding='5' cellspacing='0'>");
        body.append("<tr><td><b>Alert ID:</b></td><td>").append(alert.getAlertId()).append("</td></tr>");
        body.append("<tr><td><b>Service:</b></td><td>").append(alert.getServiceName()).append("</td></tr>");
        body.append("<tr><td><b>Severity:</b></td><td>").append(alert.getSeverity()).append("</td></tr>");
        body.append("<tr><td><b>Title:</b></td><td>").append(alert.getTitle()).append("</td></tr>");
        body.append("<tr><td><b>Description:</b></td><td>").append(alert.getDescription()).append("</td></tr>");
        body.append("<tr><td><b>Anomaly Score:</b></td><td>").append(String.format("%.3f", alert.getAnomalyScore())).append("</td></tr>");
        body.append("<tr><td><b>Time:</b></td><td>").append(alert.getCreatedAt()).append("</td></tr>");
        if (alert.getHostname() != null) {
            body.append("<tr><td><b>Host:</b></td><td>").append(alert.getHostname()).append("</td></tr>");
        }
        if (alert.getPodName() != null) {
            body.append("<tr><td><b>Pod:</b></td><td>").append(alert.getPodName()).append("</td></tr>");
        }
        body.append("</table>");
        body.append("</body></html>");
        return body.toString();
    }
} 