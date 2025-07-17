package com.incident.notification_service.service;

import com.incident.notification_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    private final WebClient.Builder webClientBuilder;

    @Value("${notification.channels.slack.enabled}")
    private boolean enabled;

    @Value("${notification.channels.slack.webhook-url}")
    private String webhookUrl;

    @Value("${notification.channels.slack.channel}")
    private String channel;

    @Value("${notification.templates.slack-message}")
    private String messageTemplate;

    @Override
    public void sendNotification(Alert alert) throws Exception {
        if (!isEnabled()) {
            log.debug("Slack notifications are disabled");
            return;
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("Slack webhook URL is not configured");
            return;
        }

        String message = generateSlackMessage(alert);
        String color = getSeverityColor(alert.getSeverity());

        Map<String, Object> payload = createSlackPayload(message, color, alert);

        WebClient webClient = webClientBuilder.build();
        
        Mono<String> response = webClient.post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(
            result -> log.info("Slack notification sent for alert: {}", alert.getAlertId()),
            error -> log.error("Failed to send Slack notification for alert: {}", alert.getAlertId(), error)
        );
    }

    @Override
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.trim().isEmpty();
    }

    @Override
    public String getChannelName() {
        return "SLACK";
    }

    private String generateSlackMessage(Alert alert) {
        return messageTemplate
                .replace("{{ severity }}", alert.getSeverity().toString())
                .replace("{{ serviceName }}", alert.getServiceName())
                .replace("{{ title }}", alert.getTitle())
                .replace("{{ description }}", alert.getDescription())
                .replace("{{ anomalyScore }}", String.format("%.3f", alert.getAnomalyScore()))
                .replace("{{ createdAt }}", alert.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private String getSeverityColor(Alert.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#FF0000";  // Red
            case HIGH -> "#FF8C00";      // Dark Orange
            case MEDIUM -> "#FFD700";    // Gold
            case LOW -> "#32CD32";       // Lime Green
            case INFO -> "#1E90FF";      // Dodger Blue
        };
    }

    private Map<String, Object> createSlackPayload(String message, String color, Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        
        if (channel != null && !channel.trim().isEmpty()) {
            payload.put("channel", channel);
        }
        
        payload.put("username", "Incident Platform");
        payload.put("icon_emoji", ":warning:");

        // Create attachment for rich formatting
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("text", message);
        attachment.put("fallback", message);
        attachment.put("title", "Alert: " + alert.getTitle());
        attachment.put("title_link", ""); // Could link to dashboard
        
        // Add fields for structured data
        Map<String, Object>[] fields = new Map[]{
            createField("Alert ID", alert.getAlertId(), true),
            createField("Service", alert.getServiceName(), true),
            createField("Severity", alert.getSeverity().toString(), true),
            createField("Score", String.format("%.3f", alert.getAnomalyScore()), true)
        };
        attachment.put("fields", fields);
        
        attachment.put("timestamp", alert.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
        
        payload.put("attachments", new Object[]{attachment});
        
        return payload;
    }

    private Map<String, Object> createField(String title, String value, boolean shortField) {
        Map<String, Object> field = new HashMap<>();
        field.put("title", title);
        field.put("value", value);
        field.put("short", shortField);
        return field;
    }
} 