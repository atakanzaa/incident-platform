package com.incident.dashboard_service.controller;

import com.incident.dashboard_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final DashboardService dashboardService;

    @SubscribeMapping("/topic/alerts")
    public Map<String, Object> subscribeToAlerts() {
        log.debug("Client subscribed to alerts topic");
        // Return initial data when client subscribes
        return Map.of(
                "type", "subscription_confirmation",
                "topic", "alerts",
                "recentAlerts", dashboardService.getRecentAlerts(10)
        );
    }

    @SubscribeMapping("/topic/metrics")
    public Map<String, Object> subscribeToMetrics() {
        log.debug("Client subscribed to metrics topic");
        // Return current metrics when client subscribes
        return Map.of(
                "type", "subscription_confirmation",
                "topic", "metrics",
                "currentMetrics", dashboardService.getCurrentMetrics()
        );
    }

    @MessageMapping("/dashboard/refresh")
    @SendTo("/topic/metrics")
    public Map<String, Object> handleRefreshRequest() {
        log.debug("Received refresh request from client");
        return Map.of(
                "type", "refresh_response",
                "metrics", dashboardService.getCurrentMetrics(),
                "summary", dashboardService.getDashboardSummary()
        );
    }

    @MessageMapping("/dashboard/ping")
    @SendTo("/topic/status")
    public Map<String, Object> handlePing() {
        return Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        );
    }

    @MessageMapping("/alerts/service")
    public void handleServiceAlertsRequest(Map<String, String> request) {
        String serviceName = request.get("serviceName");
        if (serviceName != null) {
            log.debug("Received service alerts request for: {}", serviceName);
            // In a real implementation, you might send service-specific alerts
            // to a user-specific destination
        }
    }
} 