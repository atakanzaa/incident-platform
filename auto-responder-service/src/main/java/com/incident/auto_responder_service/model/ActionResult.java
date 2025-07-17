package com.incident.auto_responder_service.model;

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
public class ActionResult {
    private String actionId;
    private String alertId;
    private String serviceName;
    private ActionType actionType;
    private ActionStatus status;
    private String description;
    private LocalDateTime executedAt;
    private LocalDateTime completedAt;
    private Map<String, Object> details;
    private String errorMessage;
    private boolean success;
    private boolean dryRun;
    
    public enum ActionType {
        POD_RESTART,
        SERVICE_SCALE_UP,
        SERVICE_SCALE_DOWN,
        CIRCUIT_BREAKER_OPEN,
        CIRCUIT_BREAKER_CLOSE,
        HEALTH_CHECK,
        ROLLBACK
    }
    
    public enum ActionStatus {
        PENDING,
        EXECUTING,
        COMPLETED,
        FAILED,
        SKIPPED,
        DRY_RUN
    }
} 