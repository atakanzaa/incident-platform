package com.incident.incident_tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "incident_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummary {
    
    @Id
    private String id;
    
    @Indexed
    private LocalDateTime windowStart;
    
    @Indexed
    private LocalDateTime windowEnd;
    
    @Indexed
    private String serviceName;
    
    private long totalIncidents;
    private long criticalIncidents;
    private long highIncidents;
    private long mediumIncidents;
    private long lowIncidents;
    private long infoIncidents;
    
    private long resolvedIncidents;
    private long openIncidents;
    
    private double averageAnomalyScore;
    private double averageTimeToResolve; // minutes
    private double averageTimeToAcknowledge; // minutes
    
    private Map<String, Long> incidentsByType;
    private Map<String, Long> incidentsByHost;
    
    private LocalDateTime createdAt;
    
    @Indexed(expireAfterSeconds = 15552000) // 180 days TTL
    private LocalDateTime expiresAt;
} 