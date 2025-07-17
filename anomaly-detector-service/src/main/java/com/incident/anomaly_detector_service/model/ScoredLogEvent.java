package com.incident.anomaly_detector_service.model;

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
public class ScoredLogEvent {
    private String id;
    private String serviceName;
    private String hostname;
    private String podName;
    private LogEvent.LogLevel level;
    private String message;
    private String stackTrace;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private String traceId;
    private String spanId;
    
    // Anomaly detection results
    private double anomalyScore;
    private List<String> anomalyReasons;
    private Map<String, Double> featureScores;
    private boolean isAnomaly;
    private String anomalyType;
    private LocalDateTime scoredAt;
} 