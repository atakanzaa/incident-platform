package com.incident.dashboard_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsScheduler {

    private final DashboardService dashboardService;

    @Value("${dashboard.cache.refresh-interval-seconds}")
    private int refreshIntervalSeconds;

    // Broadcast metrics every 30 seconds (configurable)
    @Scheduled(fixedDelayString = "${dashboard.cache.refresh-interval-seconds:30}000")
    public void broadcastPeriodicMetrics() {
        try {
            dashboardService.broadcastPeriodicMetrics();
            log.debug("Periodic metrics broadcast completed");
        } catch (Exception e) {
            log.error("Error during periodic metrics broadcast", e);
        }
    }

    // Log health status every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logHealthStatus() {
        try {
            var metrics = dashboardService.getCurrentMetrics();
            log.info("Dashboard Service Health - Total Alerts: {}, Critical: {}, System Health: {}%", 
                    metrics.getTotalAlerts(), 
                    metrics.getCriticalAlerts(), 
                    metrics.getSystemHealthScore());
        } catch (Exception e) {
            log.error("Error logging dashboard health status", e);
        }
    }

    // Cleanup old data every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupOldData() {
        try {
            // This would implement cleanup of old cached data
            // For now, just log that it's running
            log.debug("Dashboard data cleanup task executed");
        } catch (Exception e) {
            log.error("Error during dashboard data cleanup", e);
        }
    }
} 