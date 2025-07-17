package com.incident.incident_tracker.service;

import com.incident.incident_tracker.model.Alert;
import com.incident.incident_tracker.model.Incident;
import com.incident.incident_tracker.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public Incident createIncidentFromAlert(Alert alert) {
        // Check if incident already exists for this alert
        Optional<Incident> existing = incidentRepository.findByAlertId(alert.getAlertId());
        if (existing.isPresent()) {
            log.debug("Incident already exists for alert: {}", alert.getAlertId());
            return updateIncident(existing.get(), alert);
        }

        // Create new incident
        Incident incident = Incident.builder()
                .alertId(alert.getAlertId())
                .correlationId(alert.getCorrelationId())
                .sourceLogId(alert.getSourceLogId())
                .serviceName(alert.getServiceName())
                .hostname(alert.getHostname())
                .podName(alert.getPodName())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .anomalyScore(alert.getAnomalyScore())
                .anomalyReasons(alert.getAnomalyReasons())
                .anomalyType(alert.getAnomalyType())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .resolvedAt(alert.getResolvedAt())
                .metadata(alert.getMetadata())
                .tags(alert.getTags())
                .assignee(alert.getAssignee())
                .escalationLevel(alert.getEscalationLevel())
                .suppressDuplicates(alert.isSuppressDuplicates())
                .fingerprint(alert.getFingerprint())
                .events(new ArrayList<>())
                .metrics(Incident.IncidentMetrics.builder()
                        .escalationCount(0)
                        .notificationsSent(0)
                        .automatedActionsTriggered(0)
                        .build())
                .affectedServices(List.of(alert.getServiceName()))
                .impactScore(calculateImpactScore(alert))
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        // Add creation event
        addEvent(incident, Incident.IncidentEvent.IncidentEventType.CREATED, 
                "Incident created from alert", "system");

        Incident saved = incidentRepository.save(incident);
        log.info("Created new incident: {} for alert: {}", saved.getId(), alert.getAlertId());
        return saved;
    }

    public Incident updateIncident(Incident incident, Alert alert) {
        boolean changed = false;

        // Update status if changed
        if (!incident.getStatus().equals(alert.getStatus())) {
            Alert.AlertStatus oldStatus = incident.getStatus();
            incident.setStatus(alert.getStatus());
            addEvent(incident, mapStatusToEventType(alert.getStatus()), 
                    "Status changed from " + oldStatus + " to " + alert.getStatus(), "system");
            changed = true;

            // Update metrics based on status change
            if (alert.getStatus() == Alert.AlertStatus.RESOLVED && incident.getResolvedAt() == null) {
                incident.setResolvedAt(alert.getResolvedAt() != null ? alert.getResolvedAt() : LocalDateTime.now());
                updateTimeToResolveMetric(incident);
            }
        }

        // Update other fields
        if (!incident.getSeverity().equals(alert.getSeverity())) {
            incident.setSeverity(alert.getSeverity());
            addEvent(incident, Incident.IncidentEvent.IncidentEventType.ESCALATED, 
                    "Severity changed to " + alert.getSeverity(), "system");
            changed = true;
        }

        if (alert.getAssignee() != null && !alert.getAssignee().equals(incident.getAssignee())) {
            incident.setAssignee(alert.getAssignee());
            addEvent(incident, Incident.IncidentEvent.IncidentEventType.ASSIGNED, 
                    "Assigned to " + alert.getAssignee(), "system");
            changed = true;
        }

        if (changed) {
            incident.setUpdatedAt(LocalDateTime.now());
            incident = incidentRepository.save(incident);
            log.debug("Updated incident: {} for alert: {}", incident.getId(), alert.getAlertId());
        }

        return incident;
    }

    public Optional<Incident> findByAlertId(String alertId) {
        return incidentRepository.findByAlertId(alertId);
    }

    public Optional<Incident> findById(String id) {
        return incidentRepository.findById(id);
    }

    public Page<Incident> findIncidents(String serviceName, Alert.AlertStatus status, 
                                       Alert.AlertSeverity severity, LocalDateTime start, 
                                       LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (serviceName != null && status != null) {
            return incidentRepository.findByServiceNameAndStatus(serviceName, status, pageable);
        } else if (serviceName != null && start != null && end != null) {
            return incidentRepository.findByServiceNameAndCreatedAtBetween(serviceName, start, end, pageable);
        } else if (serviceName != null) {
            return incidentRepository.findByServiceName(serviceName, pageable);
        } else if (status != null) {
            return incidentRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            return incidentRepository.findBySeverity(severity, pageable);
        } else if (start != null && end != null) {
            return incidentRepository.findByCreatedAtBetween(start, end, pageable);
        }

        return incidentRepository.findAll(pageable);
    }

    public Page<Incident> searchIncidents(String searchText, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return incidentRepository.findByTextSearch(searchText, pageable);
    }

    public List<Incident> findRecentIncidents(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return incidentRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<Incident> findRelatedIncidents(String correlationId) {
        return incidentRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
    }

    public Incident addComment(String incidentId, String comment, String userId) {
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            throw new RuntimeException("Incident not found: " + incidentId);
        }

        Incident incident = incidentOpt.get();
        addEvent(incident, Incident.IncidentEvent.IncidentEventType.COMMENTED, comment, userId);
        incident.setUpdatedAt(LocalDateTime.now());
        
        return incidentRepository.save(incident);
    }

    public void deleteOldIncidents(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        List<Incident> oldIncidents = incidentRepository.findByStatusAndCreatedAtBefore(
                Alert.AlertStatus.CLOSED, cutoff);
        
        if (!oldIncidents.isEmpty()) {
            incidentRepository.deleteAll(oldIncidents);
            log.info("Deleted {} old incidents older than {} days", oldIncidents.size(), daysOld);
        }
    }

    private void addEvent(Incident incident, Incident.IncidentEvent.IncidentEventType eventType, 
                         String description, String userId) {
        if (incident.getEvents() == null) {
            incident.setEvents(new ArrayList<>());
        }

        Incident.IncidentEvent event = Incident.IncidentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .description(description)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        incident.getEvents().add(event);
    }

    private Incident.IncidentEvent.IncidentEventType mapStatusToEventType(Alert.AlertStatus status) {
        return switch (status) {
            case ACKNOWLEDGED -> Incident.IncidentEvent.IncidentEventType.ACKNOWLEDGED;
            case INVESTIGATING -> Incident.IncidentEvent.IncidentEventType.INVESTIGATING;
            case RESOLVED -> Incident.IncidentEvent.IncidentEventType.RESOLVED;
            case CLOSED -> Incident.IncidentEvent.IncidentEventType.CLOSED;
            default -> Incident.IncidentEvent.IncidentEventType.CREATED;
        };
    }

    private int calculateImpactScore(Alert alert) {
        int score = 0;
        
        // Base score on severity
        score += switch (alert.getSeverity()) {
            case CRITICAL -> 100;
            case HIGH -> 75;
            case MEDIUM -> 50;
            case LOW -> 25;
            case INFO -> 10;
        };
        
        // Add anomaly score factor
        score += (int) (alert.getAnomalyScore() * 50);
        
        return Math.min(score, 200); // Cap at 200
    }

    private void updateTimeToResolveMetric(Incident incident) {
        if (incident.getCreatedAt() != null && incident.getResolvedAt() != null) {
            long timeToResolve = java.time.Duration.between(
                    incident.getCreatedAt(), incident.getResolvedAt()).toMillis();
            
            if (incident.getMetrics() != null) {
                incident.getMetrics().setTimeToResolve(timeToResolve);
            }
        }
    }
} 