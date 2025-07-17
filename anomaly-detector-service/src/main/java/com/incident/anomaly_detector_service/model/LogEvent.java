package com.incident.anomaly_detector_service.model;

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
public class LogEvent {
    private String id;
    private String serviceName;
    private String hostname;
    private String podName;
    private LogLevel level;
    private String message;
    private String stackTrace;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private String traceId;
    private String spanId;
    
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
} 