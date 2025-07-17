package com.incident.notification_service.controller;

import com.incident.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notification-service",
                "enabledChannels", notificationService.getEnabledChannels(),
                "channelCount", notificationService.getEnabledChannelCount()
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        try {
            notificationService.sendTestNotification();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test notification sent to all enabled channels"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send test notification: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> getChannelStatus() {
        return ResponseEntity.ok(Map.of(
                "enabledChannels", notificationService.getEnabledChannels(),
                "totalEnabled", notificationService.getEnabledChannelCount()
        ));
    }
} 