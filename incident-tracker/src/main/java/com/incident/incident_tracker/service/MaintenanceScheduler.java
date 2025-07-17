package com.incident.incident_tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

    private final IncidentService incidentService;

    @Value("${incident.retention.days}")
    private int retentionDays;

    // Run cleanup every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldIncidents() {
        try {
            incidentService.deleteOldIncidents(retentionDays);
            log.info("Scheduled cleanup completed for incidents older than {} days", retentionDays);
        } catch (Exception e) {
            log.error("Error during scheduled incident cleanup", e);
        }
    }

    // Log health status every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logHealthStatus() {
        log.info("Incident Tracker Service is running normally");
    }

    // Placeholder for aggregation task - runs every 15 minutes
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void aggregateIncidentMetrics() {
        // This would implement periodic aggregation of incident metrics
        // For now, just log that it's running
        log.debug("Incident metrics aggregation task executed");
    }
} 