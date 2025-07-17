package com.incident.incident_tracker.repository;

import com.incident.incident_tracker.model.Alert;
import com.incident.incident_tracker.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends MongoRepository<Incident, String> {
    
    Optional<Incident> findByAlertId(String alertId);
    
    Page<Incident> findByServiceName(String serviceName, Pageable pageable);
    
    Page<Incident> findByStatus(Alert.AlertStatus status, Pageable pageable);
    
    Page<Incident> findBySeverity(Alert.AlertSeverity severity, Pageable pageable);
    
    Page<Incident> findByServiceNameAndStatus(String serviceName, Alert.AlertStatus status, Pageable pageable);
    
    Page<Incident> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<Incident> findByServiceNameAndCreatedAtBetween(
            String serviceName, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    List<Incident> findByStatusAndCreatedAtBefore(Alert.AlertStatus status, LocalDateTime before);
    
    @Query("{ 'anomalyScore' : { $gte : ?0 } }")
    Page<Incident> findByAnomalyScoreGreaterThanEqual(double score, Pageable pageable);
    
    @Query("{ 'serviceName' : ?0, 'severity' : ?1, 'createdAt' : { $gte : ?2, $lt : ?3 } }")
    List<Incident> findByServiceAndSeverityInTimeRange(
            String serviceName, Alert.AlertSeverity severity, 
            LocalDateTime start, LocalDateTime end);
    
    @Query("{ '$text' : { '$search' : ?0 } }")
    Page<Incident> findByTextSearch(String searchText, Pageable pageable);
    
    @Query("{ 'assignee' : ?0, 'status' : { $in : ?1 } }")
    Page<Incident> findByAssigneeAndStatusIn(String assignee, List<Alert.AlertStatus> statuses, Pageable pageable);
    
    // Aggregation queries
    @Query(value = "{ 'serviceName' : ?0, 'createdAt' : { $gte : ?1, $lt : ?2 } }", 
           count = true)
    long countByServiceNameAndCreatedAtBetween(String serviceName, LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'severity' : ?0, 'createdAt' : { $gte : ?1, $lt : ?2 } }")
    List<Incident> findBySeverityAndCreatedAtBetween(
            Alert.AlertSeverity severity, LocalDateTime start, LocalDateTime end);
    
    List<Incident> findTop10ByOrderByCreatedAtDesc();
    
    List<Incident> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
} 