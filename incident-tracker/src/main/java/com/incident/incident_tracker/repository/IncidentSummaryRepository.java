package com.incident.incident_tracker.repository;

import com.incident.incident_tracker.model.IncidentSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentSummaryRepository extends MongoRepository<IncidentSummary, String> {
    
    Optional<IncidentSummary> findByServiceNameAndWindowStartAndWindowEnd(
            String serviceName, LocalDateTime windowStart, LocalDateTime windowEnd);
    
    List<IncidentSummary> findByServiceNameAndWindowStartBetween(
            String serviceName, LocalDateTime start, LocalDateTime end);
    
    List<IncidentSummary> findByWindowStartBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'windowStart' : { $gte : ?0, $lt : ?1 } }")
    List<IncidentSummary> findSummariesInTimeRange(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'serviceName' : ?0, 'windowStart' : { $gte : ?1 } }")
    List<IncidentSummary> findByServiceNameAndWindowStartAfter(String serviceName, LocalDateTime after);
    
    List<IncidentSummary> findTop10ByServiceNameOrderByWindowStartDesc(String serviceName);
    
    @Query(value = "{ 'windowStart' : { $gte : ?0, $lt : ?1 } }", 
           fields = "{ 'serviceName' : 1, 'totalIncidents' : 1, 'criticalIncidents' : 1 }")
    List<IncidentSummary> findServiceSummariesInTimeRange(LocalDateTime start, LocalDateTime end);
} 