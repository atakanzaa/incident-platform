package com.incident.dashboard_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {
    private String id;
    private LocalDateTime timestamp;
    
    // Alert counts by severity
    private long totalAlerts;
    private long criticalAlerts;
    private long highAlerts;
    private long mediumAlerts;
    private long lowAlerts;
    private long infoAlerts;
    
    // Alert counts by status
    private long openAlerts;
    private long acknowledgedAlerts;
    private long investigatingAlerts;
    private long resolvedAlerts;
    
    // Service-level metrics
    private Map<String, Long> alertsByService;
    private Map<String, Double> averageScoreByService;
    
    // Time-based metrics
    private double averageResolutionTime; // minutes
    private long alertsLast5Minutes;
    private long alertsLastHour;
    private long alertsLast24Hours;
    
    // Health indicators
    private double systemHealthScore; // 0-100
    private Map<String, String> serviceStatus; // service -> status
    
    // Trend data
    private Map<String, Double> alertTrends; // timeWindow -> alertCount
} 