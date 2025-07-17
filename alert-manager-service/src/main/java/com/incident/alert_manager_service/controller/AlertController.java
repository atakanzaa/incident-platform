package com.incident.alert_manager_service.controller;

import com.incident.alert_manager_service.service.AlertPublisher;
import com.incident.alert_manager_service.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertPublisher alertPublisher;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "alert-manager-service"
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestAlert() {
        try {
            alertPublisher.publishTestAlert();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test alert sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send test alert: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/maintenance/reset-counts")
    public ResponseEntity<Map<String, String>> resetServiceCounts() {
        alertService.resetServiceCounts();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Service alert counts reset"
        ));
    }

    @PostMapping("/maintenance/clean-cache")
    public ResponseEntity<Map<String, String>> cleanSuppressionCache() {
        alertService.cleanSuppressionCache();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Suppression cache cleaned"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        // This would typically get stats from a metrics service or database
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Alert statistics endpoint - implementation pending"
        ));
    }
} 