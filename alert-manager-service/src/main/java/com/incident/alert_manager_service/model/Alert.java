package com.incident.alert_manager_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String alertId;
    private String correlationId;
    private String sourceLogId;
    private String serviceName;
    private String hostname;
    private String podName;
    private AlertSeverity severity;
    private AlertStatus status;
    private String title;
    private String description;
    private double anomalyScore;
    private List<String> anomalyReasons;
    private String anomalyType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Map<String, Object> metadata;
    private List<String> tags;
    private String assignee;
    private int escalationLevel;
    private boolean suppressDuplicates;
    private String fingerprint;
    
    public enum AlertSeverity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }
    
    public enum AlertStatus {
        OPEN, ACKNOWLEDGED, INVESTIGATING, RESOLVED, SUPPRESSED, CLOSED
    }
} 