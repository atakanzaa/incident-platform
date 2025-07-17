package com.incident.dashboard_service.service;

import com.incident.dashboard_service.model.Alert;
import com.incident.dashboard_service.model.DashboardMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${dashboard.cache.alert-history-size}")
    private int alertHistorySize;

    @Value("${websocket.topics.alerts}")
    private String alertsTopic;

    @Value("${websocket.topics.metrics}")
    private String metricsTopic;

    // In-memory cache for recent alerts (in production, consider Redis)
    private final List<Alert> recentAlerts = new CopyOnWriteArrayList<>();
    private final Map<String, Long> alertCountsByService = new ConcurrentHashMap<>();
    private final Map<String, Long> alertCountsBySeverity = new ConcurrentHashMap<>();
    private final Map<String, Long> alertCountsByStatus = new ConcurrentHashMap<>();
    private final Map<LocalDateTime, Long> alertTimeSeries = new ConcurrentHashMap<>();

    public void processNewAlert(Alert alert) {
        log.debug("Processing new alert for dashboard: {}", alert.getAlertId());

        // Add to recent alerts cache
        addToRecentAlerts(alert);

        // Update counters
        updateCounters(alert);

        // Broadcast alert to WebSocket subscribers
        broadcastAlert(alert);

        // Update and broadcast metrics
        broadcastMetrics();
    }

    public List<Alert> getRecentAlerts(int limit) {
        return recentAlerts.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public DashboardMetrics getCurrentMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourAgo = now.minusHours(1);
        LocalDateTime dayAgo = now.minusDays(1);

        return DashboardMetrics.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(now)
                .totalAlerts(recentAlerts.size())
                .criticalAlerts(alertCountsBySeverity.getOrDefault("CRITICAL", 0L))
                .highAlerts(alertCountsBySeverity.getOrDefault("HIGH", 0L))
                .mediumAlerts(alertCountsBySeverity.getOrDefault("MEDIUM", 0L))
                .lowAlerts(alertCountsBySeverity.getOrDefault("LOW", 0L))
                .infoAlerts(alertCountsBySeverity.getOrDefault("INFO", 0L))
                .openAlerts(alertCountsByStatus.getOrDefault("OPEN", 0L))
                .acknowledgedAlerts(alertCountsByStatus.getOrDefault("ACKNOWLEDGED", 0L))
                .investigatingAlerts(alertCountsByStatus.getOrDefault("INVESTIGATING", 0L))
                .resolvedAlerts(alertCountsByStatus.getOrDefault("RESOLVED", 0L))
                .alertsByService(new HashMap<>(alertCountsByService))
                .averageScoreByService(calculateAverageScoresByService())
                .alertsLast5Minutes(countAlertsInTimeRange(now.minusMinutes(5), now))
                .alertsLastHour(countAlertsInTimeRange(hourAgo, now))
                .alertsLast24Hours(countAlertsInTimeRange(dayAgo, now))
                .systemHealthScore(calculateSystemHealthScore())
                .serviceStatus(getServiceStatusMap())
                .alertTrends(calculateAlertTrends())
                .build();
    }

    public Map<String, Object> getDashboardSummary() {
        DashboardMetrics metrics = getCurrentMetrics();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAlerts", metrics.getTotalAlerts());
        summary.put("criticalAlerts", metrics.getCriticalAlerts());
        summary.put("recentAlerts", getRecentAlerts(10));
        summary.put("topServices", getTopServicesByAlerts(5));
        summary.put("systemHealth", metrics.getSystemHealthScore());
        summary.put("lastUpdated", LocalDateTime.now());
        
        return summary;
    }

    private void addToRecentAlerts(Alert alert) {
        recentAlerts.add(0, alert); // Add to beginning of list
        
        // Trim list to maintain size limit
        while (recentAlerts.size() > alertHistorySize) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }
    }

    private void updateCounters(Alert alert) {
        // Update service counters
        alertCountsByService.merge(alert.getServiceName(), 1L, Long::sum);
        
        // Update severity counters
        alertCountsBySeverity.merge(alert.getSeverity().toString(), 1L, Long::sum);
        
        // Update status counters
        alertCountsByStatus.merge(alert.getStatus().toString(), 1L, Long::sum);
        
        // Update time series (rounded to minute)
        LocalDateTime minute = alert.getCreatedAt().withSecond(0).withNano(0);
        alertTimeSeries.merge(minute, 1L, Long::sum);
        
        // Clean old time series data (keep last 24 hours)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        alertTimeSeries.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
    }

    private void broadcastAlert(Alert alert) {
        try {
            messagingTemplate.convertAndSend(alertsTopic, alert);
            log.debug("Broadcasted alert {} to WebSocket topic", alert.getAlertId());
        } catch (Exception e) {
            log.error("Failed to broadcast alert via WebSocket: {}", alert.getAlertId(), e);
        }
    }

    private void broadcastMetrics() {
        try {
            DashboardMetrics metrics = getCurrentMetrics();
            messagingTemplate.convertAndSend(metricsTopic, metrics);
            log.debug("Broadcasted metrics to WebSocket topic");
        } catch (Exception e) {
            log.error("Failed to broadcast metrics via WebSocket", e);
        }
    }

    private Map<String, Double> calculateAverageScoresByService() {
        return recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        Alert::getServiceName,
                        Collectors.averagingDouble(Alert::getAnomalyScore)
                ));
    }

    private long countAlertsInTimeRange(LocalDateTime start, LocalDateTime end) {
        return recentAlerts.stream()
                .filter(alert -> !alert.getCreatedAt().isBefore(start) && 
                               !alert.getCreatedAt().isAfter(end))
                .count();
    }

    private double calculateSystemHealthScore() {
        if (recentAlerts.isEmpty()) {
            return 100.0;
        }

        // Calculate health based on recent critical/high severity alerts
        long criticalCount = alertCountsBySeverity.getOrDefault("CRITICAL", 0L);
        long highCount = alertCountsBySeverity.getOrDefault("HIGH", 0L);
        long totalAlerts = recentAlerts.size();

        double healthScore = 100.0 - (criticalCount * 20.0) - (highCount * 10.0);
        return Math.max(0.0, Math.min(100.0, healthScore));
    }

    private Map<String, String> getServiceStatusMap() {
        Map<String, String> serviceStatus = new HashMap<>();
        
        for (String service : alertCountsByService.keySet()) {
            long criticalAlerts = recentAlerts.stream()
                    .filter(alert -> alert.getServiceName().equals(service))
                    .filter(alert -> alert.getSeverity() == Alert.AlertSeverity.CRITICAL)
                    .count();
            
            if (criticalAlerts > 0) {
                serviceStatus.put(service, "CRITICAL");
            } else {
                long highAlerts = recentAlerts.stream()
                        .filter(alert -> alert.getServiceName().equals(service))
                        .filter(alert -> alert.getSeverity() == Alert.AlertSeverity.HIGH)
                        .count();
                
                serviceStatus.put(service, highAlerts > 0 ? "DEGRADED" : "HEALTHY");
            }
        }
        
        return serviceStatus;
    }

    private Map<String, Double> calculateAlertTrends() {
        Map<String, Double> trends = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        trends.put("last5min", (double) countAlertsInTimeRange(now.minusMinutes(5), now));
        trends.put("last15min", (double) countAlertsInTimeRange(now.minusMinutes(15), now));
        trends.put("last30min", (double) countAlertsInTimeRange(now.minusMinutes(30), now));
        trends.put("lastHour", (double) countAlertsInTimeRange(now.minusHours(1), now));
        
        return trends;
    }

    private List<Map<String, Object>> getTopServicesByAlerts(int limit) {
        return alertCountsByService.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> Map.of(
                        "serviceName", (Object) entry.getKey(),
                        "alertCount", entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    // Method for periodic metrics broadcasting
    public void broadcastPeriodicMetrics() {
        broadcastMetrics();
    }

    // Method to get specific service alerts
    public List<Alert> getAlertsForService(String serviceName, int limit) {
        return recentAlerts.stream()
                .filter(alert -> alert.getServiceName().equals(serviceName))
                .limit(limit)
                .collect(Collectors.toList());
    }
} 