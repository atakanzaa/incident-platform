package com.incident.alert_manager_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

    private final AlertService alertService;

    // Clean suppression cache every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanSuppressionCache() {
        try {
            alertService.cleanSuppressionCache();
        } catch (Exception e) {
            log.error("Error during scheduled suppression cache cleanup", e);
        }
    }

    // Reset service counts every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void resetServiceCounts() {
        try {
            alertService.resetServiceCounts();
        } catch (Exception e) {
            log.error("Error during scheduled service counts reset", e);
        }
    }

    // Log health status every 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void logHealthStatus() {
        log.info("Alert Manager Service is running normally");
    }
} 