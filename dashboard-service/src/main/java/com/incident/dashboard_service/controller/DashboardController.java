package com.incident.dashboard_service.controller;

import com.incident.dashboard_service.model.Alert;
import com.incident.dashboard_service.model.DashboardMetrics;
import com.incident.dashboard_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "dashboard-service"
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetrics> getCurrentMetrics() {
        DashboardMetrics metrics = dashboardService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/alerts/recent")
    public ResponseEntity<List<Alert>> getRecentAlerts(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<Alert> alerts = dashboardService.getRecentAlerts(limit);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/service/{serviceName}")
    public ResponseEntity<List<Alert>> getAlertsForService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<Alert> alerts = dashboardService.getAlertsForService(serviceName, limit);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/broadcast/metrics")
    public ResponseEntity<Map<String, String>> triggerMetricsBroadcast() {
        try {
            dashboardService.broadcastPeriodicMetrics();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Metrics broadcast triggered successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to broadcast metrics: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDashboardStatus() {
        DashboardMetrics metrics = dashboardService.getCurrentMetrics();
        
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "connectedClients", "WebSocket endpoint info not available via REST",
                "totalAlerts", metrics.getTotalAlerts(),
                "criticalAlerts", metrics.getCriticalAlerts(),
                "systemHealth", metrics.getSystemHealthScore(),
                "lastUpdated", metrics.getTimestamp()
        ));
    }
} 