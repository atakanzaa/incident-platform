"""
Data models for AI Service
"""

from pydantic import BaseModel, Field
from typing import Dict, List, Optional, Any
from datetime import datetime
from enum import Enum

class LogLevel(str, Enum):
    """Log levels"""
    TRACE = "TRACE"
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARN = "WARN"
    ERROR = "ERROR"
    FATAL = "FATAL"

class LogEvent(BaseModel):
    """Log event data model"""
    log_id: str = Field(..., description="Unique log identifier")
    timestamp: datetime = Field(..., description="Log timestamp")
    service_name: str = Field(..., description="Service that generated the log")
    hostname: str = Field(..., description="Hostname where log was generated")
    log_level: LogLevel = Field(..., description="Log level")
    message: str = Field(..., description="Log message content")
    thread: Optional[str] = Field(None, description="Thread identifier")
    logger: Optional[str] = Field(None, description="Logger name")
    method: Optional[str] = Field(None, description="Method/function name")
    line_number: Optional[int] = Field(None, description="Line number in source code")
    exception: Optional[str] = Field(None, description="Exception details if any")
    stack_trace: Optional[str] = Field(None, description="Stack trace if available")
    user_id: Optional[str] = Field(None, description="User ID associated with log")
    session_id: Optional[str] = Field(None, description="Session ID")
    request_id: Optional[str] = Field(None, description="Request ID for tracing")
    http_method: Optional[str] = Field(None, description="HTTP method if web request")
    http_status: Optional[int] = Field(None, description="HTTP status code")
    endpoint: Optional[str] = Field(None, description="API endpoint")
    duration_ms: Optional[float] = Field(None, description="Request duration in milliseconds")
    metadata: Optional[Dict[str, Any]] = Field(default_factory=dict, description="Additional metadata")
    
    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
        schema_extra = {
            "example": {
                "log_id": "log_12345",
                "timestamp": "2024-01-01T12:00:00Z",
                "service_name": "auth-service",
                "hostname": "auth-service-pod-123",
                "log_level": "ERROR",
                "message": "Failed to authenticate user: Invalid credentials",
                "thread": "http-nio-8081-exec-1",
                "logger": "com.incident.auth.AuthController",
                "method": "authenticate",
                "line_number": 156,
                "exception": "AuthenticationException",
                "user_id": "user_123",
                "session_id": "session_456",
                "request_id": "req_789",
                "http_method": "POST",
                "http_status": 401,
                "endpoint": "/api/auth/login",
                "duration_ms": 245.5,
                "metadata": {
                    "ip_address": "192.168.1.100",
                    "user_agent": "Mozilla/5.0..."
                }
            }
        }

class AnomalyType(str, Enum):
    """Types of anomalies that can be detected"""
    ERROR_SPIKE = "error_spike"
    UNUSUAL_PATTERN = "unusual_pattern"
    PERFORMANCE_DEGRADATION = "performance_degradation"
    SECURITY_THREAT = "security_threat"
    RESOURCE_EXHAUSTION = "resource_exhaustion"
    CORRELATION_ANOMALY = "correlation_anomaly"
    UNKNOWN = "unknown"

class AnomalyResult(BaseModel):
    """Result of anomaly detection"""
    log_id: str = Field(..., description="Log ID that was analyzed")
    is_anomaly: bool = Field(..., description="Whether the log is anomalous")
    anomaly_score: float = Field(..., ge=0.0, le=1.0, description="Anomaly score (0-1)")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence in prediction")
    anomaly_type: Optional[AnomalyType] = Field(None, description="Type of anomaly detected")
    anomaly_reasons: List[str] = Field(default_factory=list, description="Reasons for anomaly classification")
    features_analyzed: List[str] = Field(default_factory=list, description="Features used in analysis")
    model_version: str = Field(..., description="Version of the model used")
    processing_time_ms: float = Field(..., description="Time taken for processing in milliseconds")
    threshold_used: float = Field(..., description="Anomaly threshold used")
    similar_patterns: Optional[List[str]] = Field(default_factory=list, description="Similar historical patterns")
    recommendations: Optional[List[str]] = Field(default_factory=list, description="Recommended actions")
    metadata: Optional[Dict[str, Any]] = Field(default_factory=dict, description="Additional analysis metadata")
    
    class Config:
        schema_extra = {
            "example": {
                "log_id": "log_12345",
                "is_anomaly": True,
                "anomaly_score": 0.85,
                "confidence": 0.92,
                "anomaly_type": "error_spike",
                "anomaly_reasons": [
                    "Unusual error frequency detected",
                    "Authentication failures above normal threshold",
                    "Error pattern not seen in training data"
                ],
                "features_analyzed": [
                    "log_level",
                    "error_frequency",
                    "temporal_pattern",
                    "service_context"
                ],
                "model_version": "v1.2.3",
                "processing_time_ms": 23.5,
                "threshold_used": 0.7,
                "similar_patterns": ["pattern_auth_failure_2023_12"],
                "recommendations": [
                    "Investigate authentication service",
                    "Check for potential security breach",
                    "Monitor related services for cascading failures"
                ],
                "metadata": {
                    "model_type": "isolation_forest",
                    "feature_importance": {
                        "log_level": 0.3,
                        "error_frequency": 0.4,
                        "temporal_pattern": 0.2,
                        "service_context": 0.1
                    }
                }
            }
        }

class BatchAnomalyRequest(BaseModel):
    """Request for batch anomaly detection"""
    log_events: List[LogEvent] = Field(..., description="List of log events to analyze")
    correlation_id: Optional[str] = Field(None, description="Correlation ID for tracking")
    analysis_type: Optional[str] = Field("standard", description="Type of analysis to perform")
    
class ModelStatus(BaseModel):
    """Model status information"""
    model_name: str = Field(..., description="Name of the model")
    version: str = Field(..., description="Model version")
    status: str = Field(..., description="Model status (loaded, training, error)")
    last_trained: Optional[datetime] = Field(None, description="Last training timestamp")
    accuracy: Optional[float] = Field(None, description="Model accuracy score")
    precision: Optional[float] = Field(None, description="Model precision score")
    recall: Optional[float] = Field(None, description="Model recall score")
    f1_score: Optional[float] = Field(None, description="Model F1 score")
    training_samples: Optional[int] = Field(None, description="Number of training samples")
    model_size_mb: Optional[float] = Field(None, description="Model size in megabytes")
    
class FeatureImportance(BaseModel):
    """Feature importance for model interpretability"""
    feature_name: str = Field(..., description="Name of the feature")
    importance_score: float = Field(..., description="Importance score")
    description: Optional[str] = Field(None, description="Feature description")

class ModelConfig(BaseModel):
    """Configuration for ML models"""
    anomaly_threshold: float = Field(0.7, ge=0.0, le=1.0, description="Threshold for anomaly detection")
    confidence_threshold: float = Field(0.8, ge=0.0, le=1.0, description="Minimum confidence threshold")
    batch_size: int = Field(100, gt=0, description="Batch size for processing")
    enable_feature_selection: bool = Field(True, description="Enable automatic feature selection")
    retrain_interval_hours: int = Field(24, gt=0, description="Retrain interval in hours")
    max_training_samples: int = Field(100000, gt=0, description="Maximum samples for training")
    model_persistence: bool = Field(True, description="Enable model persistence")
    enable_online_learning: bool = Field(False, description="Enable online learning")
    feature_weights: Optional[Dict[str, float]] = Field(default_factory=dict, description="Custom feature weights") 