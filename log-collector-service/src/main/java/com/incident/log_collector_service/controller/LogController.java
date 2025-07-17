package com.incident.log_collector_service.controller;

import com.incident.log_collector_service.model.LogEvent;
import com.incident.log_collector_service.service.LogProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogProducer logProducer;

    @PostMapping
    public ResponseEntity<Map<String, String>> collectLog(@Valid @RequestBody LogEvent logEvent) {
        // Set timestamp if not provided
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(LocalDateTime.now());
        }

        // Send to Kafka
        logProducer.sendLog(logEvent);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", "accepted",
                        "id", logEvent.getId(),
                        "message", "Log event queued for processing"
                ));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> collectLogs(@Valid @RequestBody List<LogEvent> logEvents) {
        // Process each log event
        logEvents.forEach(logEvent -> {
            if (logEvent.getId() == null) {
                logEvent.setId(UUID.randomUUID().toString());
            }
            if (logEvent.getTimestamp() == null) {
                logEvent.setTimestamp(LocalDateTime.now());
            }
            logProducer.sendLog(logEvent);
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", "accepted",
                        "count", logEvents.size(),
                        "message", "Log events queued for processing"
                ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "log-collector-service"
        ));
    }
} 