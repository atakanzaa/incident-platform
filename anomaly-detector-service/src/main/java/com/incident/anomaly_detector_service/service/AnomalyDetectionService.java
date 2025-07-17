package com.incident.anomaly_detector_service.service;

import com.incident.anomaly_detector_service.model.LogEvent;
import com.incident.anomaly_detector_service.model.ScoredLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final KafkaTemplate<String, ScoredLogEvent> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    @Value("${kafka.topics.logs-scored}")
    private String logsScoredTopic;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.endpoint}")
    private String aiServiceEndpoint;

    @Value("${anomaly.detection.threshold}")
    private double anomalyThreshold;

    public void processLogEvent(LogEvent logEvent) {
        try {
            // Call AI service for anomaly detection
            ScoredLogEvent scoredLogEvent = detectAnomaly(logEvent).block();
            
            if (scoredLogEvent != null) {
                // Send scored event to Kafka
                kafkaTemplate.send(logsScoredTopic, logEvent.getServiceName(), scoredLogEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Sent scored log event: {} with score: {}", 
                                scoredLogEvent.getId(), scoredLogEvent.getAnomalyScore());
                        } else {
                            log.error("Failed to send scored log event: {}", scoredLogEvent.getId(), ex);
                        }
                    });
            }
        } catch (Exception e) {
            log.error("Error processing log event: {}", logEvent.getId(), e);
            // Send with default score in case of AI service failure
            sendDefaultScoredEvent(logEvent);
        }
    }

    public void processLogEventsBatch(List<LogEvent> logEvents) {
        logEvents.forEach(this::processLogEvent);
    }

    private Mono<ScoredLogEvent> detectAnomaly(LogEvent logEvent) {
        WebClient webClient = webClientBuilder
                .baseUrl(aiServiceUrl)
                .build();

        Map<String, Object> aiRequest = buildAIRequest(logEvent);

        return webClient.post()
                .uri(aiServiceEndpoint)
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> mapToScoredLogEvent(logEvent, response))
                .onErrorReturn(createDefaultScoredLogEvent(logEvent));
    }

    private Map<String, Object> buildAIRequest(LogEvent logEvent) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", logEvent.getId());
        request.put("service", logEvent.getServiceName());
        request.put("level", logEvent.getLevel().toString());
        request.put("message", logEvent.getMessage());
        request.put("timestamp", logEvent.getTimestamp().toString());
        request.put("metadata", logEvent.getMetadata());
        return request;
    }

    private ScoredLogEvent mapToScoredLogEvent(LogEvent logEvent, Map<String, Object> aiResponse) {
        double score = ((Number) aiResponse.getOrDefault("anomaly_score", 0.0)).doubleValue();
        List<String> reasons = (List<String>) aiResponse.getOrDefault("reasons", new ArrayList<>());
        Map<String, Double> features = (Map<String, Double>) aiResponse.getOrDefault("feature_scores", new HashMap<>());
        String anomalyType = (String) aiResponse.getOrDefault("anomaly_type", "unknown");

        return ScoredLogEvent.builder()
                .id(logEvent.getId())
                .serviceName(logEvent.getServiceName())
                .hostname(logEvent.getHostname())
                .podName(logEvent.getPodName())
                .level(logEvent.getLevel())
                .message(logEvent.getMessage())
                .stackTrace(logEvent.getStackTrace())
                .timestamp(logEvent.getTimestamp())
                .metadata(logEvent.getMetadata())
                .traceId(logEvent.getTraceId())
                .spanId(logEvent.getSpanId())
                .anomalyScore(score)
                .anomalyReasons(reasons)
                .featureScores(features)
                .isAnomaly(score > anomalyThreshold)
                .anomalyType(anomalyType)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    private ScoredLogEvent createDefaultScoredLogEvent(LogEvent logEvent) {
        // Default scoring based on log level
        double defaultScore = switch (logEvent.getLevel()) {
            case ERROR, FATAL -> 0.7;
            case WARN -> 0.4;
            default -> 0.1;
        };

        return ScoredLogEvent.builder()
                .id(logEvent.getId())
                .serviceName(logEvent.getServiceName())
                .hostname(logEvent.getHostname())
                .podName(logEvent.getPodName())
                .level(logEvent.getLevel())
                .message(logEvent.getMessage())
                .stackTrace(logEvent.getStackTrace())
                .timestamp(logEvent.getTimestamp())
                .metadata(logEvent.getMetadata())
                .traceId(logEvent.getTraceId())
                .spanId(logEvent.getSpanId())
                .anomalyScore(defaultScore)
                .anomalyReasons(List.of("AI service unavailable, default scoring applied"))
                .featureScores(new HashMap<>())
                .isAnomaly(defaultScore > anomalyThreshold)
                .anomalyType("default")
                .scoredAt(LocalDateTime.now())
                .build();
    }

    private void sendDefaultScoredEvent(LogEvent logEvent) {
        ScoredLogEvent defaultScored = createDefaultScoredLogEvent(logEvent);
        kafkaTemplate.send(logsScoredTopic, logEvent.getServiceName(), defaultScored);
    }
} 