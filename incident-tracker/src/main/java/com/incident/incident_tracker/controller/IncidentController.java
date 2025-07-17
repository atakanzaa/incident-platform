package com.incident.incident_tracker.controller;

import com.incident.incident_tracker.model.Alert;
import com.incident.incident_tracker.model.Incident;
import com.incident.incident_tracker.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "incident-tracker-service"
        ));
    }

    @GetMapping
    public ResponseEntity<Page<Incident>> getIncidents(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) Alert.AlertStatus status,
            @RequestParam(required = false) Alert.AlertSeverity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Incident> incidents = incidentService.findIncidents(
                serviceName, status, severity, start, end, page, size);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncident(@PathVariable String id) {
        Optional<Incident> incident = incidentService.findById(id);
        return incident.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/alert/{alertId}")
    public ResponseEntity<Incident> getIncidentByAlertId(@PathVariable String alertId) {
        Optional<Incident> incident = incidentService.findByAlertId(alertId);
        return incident.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Incident>> searchIncidents(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Incident> incidents = incidentService.searchIncidents(q, page, size);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Incident>> getRecentIncidents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Incident> incidents = incidentService.findRecentIncidents(limit);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/related/{correlationId}")
    public ResponseEntity<List<Incident>> getRelatedIncidents(@PathVariable String correlationId) {
        List<Incident> incidents = incidentService.findRelatedIncidents(correlationId);
        return ResponseEntity.ok(incidents);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Incident> addComment(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String comment = request.get("comment");
        String userId = request.getOrDefault("userId", "anonymous");
        
        if (comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Incident incident = incidentService.addComment(id, comment, userId);
            return ResponseEntity.ok(incident);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getIncidentSummary(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        // This would typically be implemented with aggregation queries
        // For now, return a placeholder response
        return ResponseEntity.ok(Map.of(
                "message", "Incident summary endpoint - implementation pending",
                "serviceName", serviceName != null ? serviceName : "all",
                "timeRange", Map.of(
                        "start", start != null ? start.toString() : "not specified",
                        "end", end != null ? end.toString() : "not specified"
                )
        ));
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupOldIncidents(
            @RequestParam(defaultValue = "90") int daysOld
    ) {
        try {
            incidentService.deleteOldIncidents(daysOld);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cleanup completed for incidents older than " + daysOld + " days"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Cleanup failed: " + e.getMessage()
            ));
        }
    }
} 