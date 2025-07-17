package com.incident.notification_service.service;

import com.incident.notification_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final List<NotificationChannel> notificationChannels;

    public void sendNotification(Alert alert) {
        log.info("Sending notifications for alert: {} with severity: {}", 
            alert.getAlertId(), alert.getSeverity());

        List<CompletableFuture<Void>> futures = notificationChannels.stream()
                .filter(NotificationChannel::isEnabled)
                .map(channel -> sendNotificationAsync(channel, alert))
                .toList();

        // Wait for all notifications to complete (with timeout)
        CompletableFuture<Void> allNotifications = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        allNotifications.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Some notifications failed for alert: {}", alert.getAlertId(), throwable);
            } else {
                log.info("All notifications sent successfully for alert: {}", alert.getAlertId());
            }
        });
    }

    private CompletableFuture<Void> sendNotificationAsync(NotificationChannel channel, Alert alert) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Sending notification via {} for alert: {}", 
                    channel.getChannelName(), alert.getAlertId());
                    
                channel.sendNotification(alert);
                
                log.debug("Notification sent successfully via {} for alert: {}", 
                    channel.getChannelName(), alert.getAlertId());
                    
            } catch (Exception e) {
                log.error("Failed to send notification via {} for alert: {}", 
                    channel.getChannelName(), alert.getAlertId(), e);
                // Don't rethrow - we want other channels to continue working
            }
        });
    }

    public void sendTestNotification() {
        Alert testAlert = Alert.builder()
                .alertId("TEST-NOTIFICATION")
                .serviceName("test-service")
                .hostname("test-host")
                .severity(Alert.AlertSeverity.INFO)
                .status(Alert.AlertStatus.OPEN)
                .title("Test Notification")
                .description("This is a test notification to verify all channels are working correctly.")
                .anomalyScore(0.5)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        sendNotification(testAlert);
    }

    public List<String> getEnabledChannels() {
        return notificationChannels.stream()
                .filter(NotificationChannel::isEnabled)
                .map(NotificationChannel::getChannelName)
                .toList();
    }

    public int getEnabledChannelCount() {
        return (int) notificationChannels.stream()
                .filter(NotificationChannel::isEnabled)
                .count();
    }
} 