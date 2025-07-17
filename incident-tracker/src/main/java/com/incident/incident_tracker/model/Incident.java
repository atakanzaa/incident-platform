package com.incident.incident_tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String alertId;
    
    private String correlationId;
    private String sourceLogId;
    
    @Indexed
    private String serviceName;
    
    @Indexed
    private String hostname;
    
    private String podName;
    
    @Indexed
    private Alert.AlertSeverity severity;
    
    @Indexed
    private Alert.AlertStatus status;
    
    @TextIndexed
    private String title;
    
    @TextIndexed
    private String description;
    
    @Indexed
    private double anomalyScore;
    
    private List<String> anomalyReasons;
    private String anomalyType;
    
    @Indexed
    private LocalDateTime createdAt;
    
    @Indexed
    private LocalDateTime updatedAt;
    
    @Indexed
    private LocalDateTime resolvedAt;
    
    private Map<String, Object> metadata;
    private List<String> tags;
    private String assignee;
    private int escalationLevel;
    private boolean suppressDuplicates;
    private String fingerprint;
    
    // Incident tracking fields
    private List<IncidentEvent> events;
    private IncidentMetrics metrics;
    private List<String> relatedAlerts;
    private String resolution;
    private String rootCause;
    private List<String> affectedServices;
    private int impactScore;
    
    @Indexed(expireAfterSeconds = 7776000) // 90 days TTL
    private LocalDateTime expiresAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentEvent {
        private String eventId;
        private IncidentEventType eventType;
        private String description;
        private String userId;
        private LocalDateTime timestamp;
        private Map<String, Object> eventData;
        
        public enum IncidentEventType {
            CREATED, ACKNOWLEDGED, INVESTIGATING, ESCALATED, 
            RESOLVED, CLOSED, REOPENED, COMMENTED, ASSIGNED
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentMetrics {
        private Long timeToAcknowledge; // milliseconds
        private Long timeToResolve; // milliseconds
        private int escalationCount;
        private int notificationsSent;
        private int automatedActionsTriggered;
        private Double businessImpact;
    }
} 