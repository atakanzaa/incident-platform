package com.incident.alert_manager_service.service;

import com.incident.alert_manager_service.model.Alert;
import com.incident.alert_manager_service.model.ScoredLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertPublisher alertPublisher;

    @Value("${alert.thresholds.critical}")
    private double criticalThreshold;

    @Value("${alert.thresholds.high}")
    private double highThreshold;

    @Value("${alert.thresholds.medium}")
    private double mediumThreshold;

    @Value("${alert.thresholds.low}")
    private double lowThreshold;

    @Value("${alert.rules.suppress-duplicate-window}")
    private int suppressDuplicateWindow;

    @Value("${alert.rules.max-alerts-per-service}")
    private int maxAlertsPerService;

    // In-memory store for duplicate suppression - should be Redis in production
    private final Map<String, LocalDateTime> suppressionCache = new HashMap<>();
    private final Map<String, Integer> serviceAlertCount = new HashMap<>();

    public void processAlert(ScoredLogEvent scoredLogEvent) {
        try {
            Alert alert = createAlert(scoredLogEvent);
            
            if (shouldSuppressAlert(alert)) {
                log.debug("Alert suppressed: {}", alert.getAlertId());
                return;
            }

            if (exceedsServiceLimit(alert)) {
                log.warn("Service {} has exceeded max alerts limit", alert.getServiceName());
                return;
            }

            // Update suppression cache and service count
            updateSuppressionCache(alert);
            incrementServiceCount(alert.getServiceName());

            // Publish alert based on severity
            alertPublisher.publishAlert(alert);
            
            log.info("Alert created and published: {} with severity: {}", 
                alert.getAlertId(), alert.getSeverity());

        } catch (Exception e) {
            log.error("Error processing alert for scored log event: {}", scoredLogEvent.getId(), e);
        }
    }

    private Alert createAlert(ScoredLogEvent scoredLogEvent) {
        Alert.AlertSeverity severity = determineSeverity(scoredLogEvent.getAnomalyScore());
        String alertId = generateAlertId();
        String title = generateAlertTitle(scoredLogEvent, severity);
        String description = generateAlertDescription(scoredLogEvent);
        String fingerprint = generateFingerprint(scoredLogEvent);

        return Alert.builder()
                .alertId(alertId)
                .correlationId(generateCorrelationId(scoredLogEvent))
                .sourceLogId(scoredLogEvent.getId())
                .serviceName(scoredLogEvent.getServiceName())
                .hostname(scoredLogEvent.getHostname())
                .podName(scoredLogEvent.getPodName())
                .severity(severity)
                .status(Alert.AlertStatus.OPEN)
                .title(title)
                .description(description)
                .anomalyScore(scoredLogEvent.getAnomalyScore())
                .anomalyReasons(scoredLogEvent.getAnomalyReasons())
                .anomalyType(scoredLogEvent.getAnomalyType())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .metadata(enrichMetadata(scoredLogEvent))
                .tags(generateTags(scoredLogEvent))
                .escalationLevel(0)
                .suppressDuplicates(true)
                .fingerprint(fingerprint)
                .build();
    }

    private Alert.AlertSeverity determineSeverity(double anomalyScore) {
        if (anomalyScore >= criticalThreshold) {
            return Alert.AlertSeverity.CRITICAL;
        } else if (anomalyScore >= highThreshold) {
            return Alert.AlertSeverity.HIGH;
        } else if (anomalyScore >= mediumThreshold) {
            return Alert.AlertSeverity.MEDIUM;
        } else if (anomalyScore >= lowThreshold) {
            return Alert.AlertSeverity.LOW;
        } else {
            return Alert.AlertSeverity.INFO;
        }
    }

    private String generateAlertTitle(ScoredLogEvent scoredLogEvent, Alert.AlertSeverity severity) {
        return String.format("%s Alert: %s anomaly detected in %s", 
            severity.name(), 
            scoredLogEvent.getAnomalyType(), 
            scoredLogEvent.getServiceName());
    }

    private String generateAlertDescription(ScoredLogEvent scoredLogEvent) {
        StringBuilder description = new StringBuilder();
        description.append("Anomaly detected with score: ").append(scoredLogEvent.getAnomalyScore()).append("\n");
        description.append("Service: ").append(scoredLogEvent.getServiceName()).append("\n");
        description.append("Host: ").append(scoredLogEvent.getHostname()).append("\n");
        
        if (scoredLogEvent.getPodName() != null) {
            description.append("Pod: ").append(scoredLogEvent.getPodName()).append("\n");
        }
        
        description.append("Log Level: ").append(scoredLogEvent.getLevel()).append("\n");
        description.append("Message: ").append(scoredLogEvent.getMessage()).append("\n");
        
        if (scoredLogEvent.getAnomalyReasons() != null && !scoredLogEvent.getAnomalyReasons().isEmpty()) {
            description.append("Reasons: ").append(String.join(", ", scoredLogEvent.getAnomalyReasons()));
        }
        
        return description.toString();
    }

    private String generateFingerprint(ScoredLogEvent scoredLogEvent) {
        String input = scoredLogEvent.getServiceName() + 
                      scoredLogEvent.getHostname() + 
                      scoredLogEvent.getAnomalyType() + 
                      scoredLogEvent.getLevel();
        
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating fingerprint", e);
            return input.hashCode() + "";
        }
    }

    private String generateAlertId() {
        return "ALERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateCorrelationId(ScoredLogEvent scoredLogEvent) {
        return scoredLogEvent.getTraceId() != null ? 
            scoredLogEvent.getTraceId() : 
            "COR-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> enrichMetadata(ScoredLogEvent scoredLogEvent) {
        Map<String, Object> metadata = new HashMap<>();
        if (scoredLogEvent.getMetadata() != null) {
            metadata.putAll(scoredLogEvent.getMetadata());
        }
        
        metadata.put("originalTimestamp", scoredLogEvent.getTimestamp());
        metadata.put("scoredAt", scoredLogEvent.getScoredAt());
        metadata.put("traceId", scoredLogEvent.getTraceId());
        metadata.put("spanId", scoredLogEvent.getSpanId());
        
        if (scoredLogEvent.getFeatureScores() != null) {
            metadata.put("featureScores", scoredLogEvent.getFeatureScores());
        }
        
        return metadata;
    }

    private List<String> generateTags(ScoredLogEvent scoredLogEvent) {
        List<String> tags = new ArrayList<>();
        tags.add("service:" + scoredLogEvent.getServiceName());
        tags.add("level:" + scoredLogEvent.getLevel().toString().toLowerCase());
        tags.add("anomaly_type:" + scoredLogEvent.getAnomalyType());
        
        if (scoredLogEvent.getHostname() != null) {
            tags.add("host:" + scoredLogEvent.getHostname());
        }
        
        if (scoredLogEvent.getPodName() != null) {
            tags.add("pod:" + scoredLogEvent.getPodName());
        }
        
        return tags;
    }

    private boolean shouldSuppressAlert(Alert alert) {
        if (!alert.isSuppressDuplicates()) {
            return false;
        }

        String suppressionKey = alert.getFingerprint();
        LocalDateTime lastAlert = suppressionCache.get(suppressionKey);
        
        if (lastAlert != null) {
            LocalDateTime suppressionWindow = lastAlert.plusSeconds(suppressDuplicateWindow);
            return LocalDateTime.now().isBefore(suppressionWindow);
        }
        
        return false;
    }

    private boolean exceedsServiceLimit(Alert alert) {
        String serviceName = alert.getServiceName();
        int currentCount = serviceAlertCount.getOrDefault(serviceName, 0);
        return currentCount >= maxAlertsPerService;
    }

    private void updateSuppressionCache(Alert alert) {
        if (alert.isSuppressDuplicates()) {
            suppressionCache.put(alert.getFingerprint(), LocalDateTime.now());
        }
    }

    private void incrementServiceCount(String serviceName) {
        serviceAlertCount.merge(serviceName, 1, Integer::sum);
    }

    // Method to reset service counts (should be called periodically)
    public void resetServiceCounts() {
        serviceAlertCount.clear();
        log.info("Service alert counts reset");
    }

    // Method to clean old suppression entries (should be called periodically)
    public void cleanSuppressionCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(suppressDuplicateWindow);
        suppressionCache.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        log.info("Suppression cache cleaned, removed entries older than {} seconds", suppressDuplicateWindow);
    }
} 