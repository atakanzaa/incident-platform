"""
Anomaly detection service
Uses multiple ML algorithms to detect anomalies in log events
"""

import time
import numpy as np
from typing import List, Dict, Any
from sklearn.ensemble import IsolationForest
from sklearn.svm import OneClassSVM
from sklearn.neighbors import LocalOutlierFactor
import logging

from models.log_event import LogEvent, AnomalyResult, AnomalyType
from services.feature_extractor import FeatureExtractor
from config.settings import get_settings, MODEL_CONFIGS

logger = logging.getLogger(__name__)

class AnomalyDetectorService:
    """Main anomaly detection service using ensemble of algorithms"""
    
    def __init__(self, model_manager):
        self.model_manager = model_manager
        self.settings = get_settings()
        self.feature_extractor = FeatureExtractor()
        self.models = {}
        self.is_initialized = False
        self.config = {
            'anomaly_threshold': self.settings.default_anomaly_threshold,
            'confidence_threshold': self.settings.default_confidence_threshold,
            'model_version': '1.0.0'
        }
        
    async def initialize(self):
        """Initialize the anomaly detector"""
        try:
            logger.info("Initializing anomaly detector service...")
            
            # Initialize feature extractor
            await self.feature_extractor.initialize()
            
            # Initialize models
            await self._initialize_models()
            
            # Generate synthetic training data for demo purposes
            await self._train_with_synthetic_data()
            
            self.is_initialized = True
            logger.info("Anomaly detector service initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize anomaly detector: {e}")
            raise
    
    async def _initialize_models(self):
        """Initialize ML models"""
        try:
            # Isolation Forest
            self.models['isolation_forest'] = IsolationForest(
                contamination=MODEL_CONFIGS['isolation_forest']['parameters']['contamination'],
                n_estimators=MODEL_CONFIGS['isolation_forest']['parameters']['n_estimators'],
                random_state=MODEL_CONFIGS['isolation_forest']['parameters']['random_state']
            )
            
            # One-Class SVM
            self.models['one_class_svm'] = OneClassSVM(
                nu=MODEL_CONFIGS['one_class_svm']['parameters']['nu'],
                kernel=MODEL_CONFIGS['one_class_svm']['parameters']['kernel'],
                gamma=MODEL_CONFIGS['one_class_svm']['parameters']['gamma']
            )
            
            # Local Outlier Factor
            self.models['local_outlier_factor'] = LocalOutlierFactor(
                n_neighbors=MODEL_CONFIGS['local_outlier_factor']['parameters']['n_neighbors'],
                contamination=MODEL_CONFIGS['local_outlier_factor']['parameters']['contamination'],
                novelty=MODEL_CONFIGS['local_outlier_factor']['parameters']['novelty']
            )
            
            logger.info(f"Initialized {len(self.models)} ML models")
            
        except Exception as e:
            logger.error(f"Failed to initialize models: {e}")
            raise
    
    async def _train_with_synthetic_data(self):
        """Train models with synthetic data for demo purposes"""
        try:
            logger.info("Training models with synthetic data...")
            
            # Generate synthetic log events
            synthetic_events = self._generate_synthetic_training_data(1000)
            
            # Fit feature extractor
            await self.feature_extractor.fit(synthetic_events)
            
            # Extract features
            features, feature_names = await self.feature_extractor.transform(synthetic_events)
            
            # Train models
            if features.shape[1] > 0:
                # Isolation Forest
                self.models['isolation_forest'].fit(features)
                
                # One-Class SVM (handle potential memory issues with subset)
                subset_size = min(500, len(features))
                subset_indices = np.random.choice(len(features), subset_size, replace=False)
                self.models['one_class_svm'].fit(features[subset_indices])
                
                # Local Outlier Factor
                self.models['local_outlier_factor'].fit(features)
                
                logger.info(f"Models trained on {len(features)} samples with {features.shape[1]} features")
            else:
                logger.warning("No features extracted, using fallback scoring")
                
        except Exception as e:
            logger.error(f"Failed to train models: {e}")
            # Continue with fallback scoring
    
    def _generate_synthetic_training_data(self, num_samples: int) -> List[LogEvent]:
        """Generate synthetic log events for training"""
        from datetime import datetime, timedelta
        import random
        import uuid
        
        events = []
        base_time = datetime.utcnow()
        
        services = ['auth-service', 'log-collector-service', 'gateway-service', 'dashboard-service']
        log_levels = ['INFO', 'WARN', 'ERROR', 'DEBUG']
        
        normal_messages = [
            "User authentication successful",
            "Request processed successfully",
            "Database connection established",
            "Cache hit for key",
            "Scheduled task completed",
            "Configuration loaded",
            "Health check passed",
            "Request received from client"
        ]
        
        anomaly_messages = [
            "OutOfMemoryError: Java heap space",
            "Connection timeout after 30 seconds",
            "Authentication failed for user",
            "SQL injection attempt detected",
            "Rate limit exceeded for IP",
            "Disk space critically low",
            "Unusual login pattern detected",
            "Multiple failed authentication attempts"
        ]
        
        for i in range(num_samples):
            # 90% normal, 10% anomalous for training
            is_anomaly = random.random() < 0.1
            
            if is_anomaly:
                message = random.choice(anomaly_messages)
                log_level = random.choice(['ERROR', 'WARN'])
                duration_ms = random.uniform(1000, 5000)  # Slower for anomalies
            else:
                message = random.choice(normal_messages)
                log_level = random.choice(['INFO', 'DEBUG'])
                duration_ms = random.uniform(10, 500)  # Faster for normal
            
            event = LogEvent(
                log_id=f"synthetic_{i}",
                timestamp=base_time - timedelta(minutes=random.randint(0, 1440)),
                service_name=random.choice(services),
                hostname=f"pod-{random.randint(1, 10)}",
                log_level=log_level,
                message=message,
                thread=f"thread-{random.randint(1, 20)}",
                logger=f"com.incident.{random.choice(services).replace('-', '.')}.Controller",
                method=f"method_{random.randint(1, 10)}",
                line_number=random.randint(10, 500),
                duration_ms=duration_ms,
                http_status=random.choice([200, 201, 400, 401, 404, 500]) if random.random() < 0.7 else None,
                metadata={"synthetic": True}
            )
            events.append(event)
        
        return events
    
    async def predict_anomaly(self, log_event: LogEvent) -> AnomalyResult:
        """Predict if a single log event is anomalous"""
        start_time = time.time()
        
        try:
            if not self.is_initialized:
                raise ValueError("Anomaly detector not initialized")
            
            # Extract features
            features, feature_names = await self.feature_extractor.transform([log_event])
            
            if features.shape[1] == 0:
                # Fallback scoring based on heuristics
                return self._fallback_scoring(log_event, start_time)
            
            # Get predictions from all models
            predictions = {}
            scores = {}
            
            # Isolation Forest
            if 'isolation_forest' in self.models:
                pred = self.models['isolation_forest'].predict(features)[0]
                score = self.models['isolation_forest'].score_samples(features)[0]
                predictions['isolation_forest'] = bool(pred == -1)  # Convert numpy.bool_ to Python bool
                scores['isolation_forest'] = float(self._normalize_score(score, 'isolation_forest'))
            
            # One-Class SVM
            if 'one_class_svm' in self.models:
                pred = self.models['one_class_svm'].predict(features)[0]
                score = self.models['one_class_svm'].score_samples(features)[0]
                predictions['one_class_svm'] = bool(pred == -1)  # Convert numpy.bool_ to Python bool
                scores['one_class_svm'] = float(self._normalize_score(score, 'one_class_svm'))
            
            # Local Outlier Factor
            if 'local_outlier_factor' in self.models:
                pred = self.models['local_outlier_factor'].predict(features)[0]
                score = self.models['local_outlier_factor'].score_samples(features)[0]
                predictions['local_outlier_factor'] = bool(pred == -1)  # Convert numpy.bool_ to Python bool
                scores['local_outlier_factor'] = float(self._normalize_score(score, 'local_outlier_factor'))
            
            # Ensemble prediction
            ensemble_score = float(np.mean(list(scores.values()))) if scores else 0.5  # Convert numpy.float64 to Python float
            is_anomaly = bool(ensemble_score > self.config['anomaly_threshold'])  # Ensure Python bool
            confidence = float(abs(ensemble_score - 0.5) * 2)  # Convert to 0-1 range, ensure Python float
            
            # Determine anomaly type and reasons
            anomaly_type, reasons = self._analyze_anomaly(log_event, predictions, scores)
            
            # Generate recommendations
            recommendations = self._generate_recommendations(log_event, anomaly_type, ensemble_score)
            
            processing_time = float((time.time() - start_time) * 1000)  # Ensure Python float
            
            return AnomalyResult(
                log_id=log_event.log_id,
                is_anomaly=is_anomaly,
                anomaly_score=ensemble_score,
                confidence=confidence,
                anomaly_type=anomaly_type,
                anomaly_reasons=reasons,
                features_analyzed=feature_names[:10],  # Limit for readability
                model_version=self.config['model_version'],
                processing_time_ms=processing_time,
                threshold_used=self.config['anomaly_threshold'],
                recommendations=recommendations,
                metadata={
                    'model_predictions': predictions,
                    'individual_scores': scores,
                    'feature_count': features.shape[1]
                }
            )
            
        except Exception as e:
            logger.error(f"Error predicting anomaly for log {log_event.log_id}: {e}")
            # Return fallback result
            return self._fallback_scoring(log_event, start_time)
    
    async def predict_batch_anomalies(self, log_events: List[LogEvent]) -> List[AnomalyResult]:
        """Predict anomalies for multiple log events"""
        try:
            logger.info(f"Processing batch of {len(log_events)} log events")
            
            if len(log_events) > self.settings.max_batch_size:
                raise ValueError(f"Batch size {len(log_events)} exceeds maximum {self.settings.max_batch_size}")
            
            # Process each event (could be optimized for true batch processing)
            results = []
            for event in log_events:
                result = await self.predict_anomaly(event)
                results.append(result)
            
            return results
            
        except Exception as e:
            logger.error(f"Error in batch prediction: {e}")
            raise
    
    def _normalize_score(self, score: float, model_name: str) -> float:
        """Normalize model-specific scores to 0-1 range"""
        try:
            if model_name == 'isolation_forest':
                # Isolation forest scores are typically negative, more negative = more anomalous
                return max(0, min(1, 0.5 - score))
            elif model_name == 'one_class_svm':
                # SVM scores can be negative, normalize around 0
                return max(0, min(1, 0.5 - score))
            elif model_name == 'local_outlier_factor':
                # LOF scores are typically negative, more negative = more anomalous
                return max(0, min(1, 0.5 - score))
            else:
                return 0.5
        except:
            return 0.5
    
    def _analyze_anomaly(self, log_event: LogEvent, predictions: Dict, scores: Dict) -> tuple:
        """Analyze the type of anomaly and reasons"""
        reasons = []
        anomaly_type = AnomalyType.UNKNOWN
        
        # Check log level
        if log_event.log_level.value in ['ERROR', 'FATAL']:
            reasons.append(f"High severity log level: {log_event.log_level.value}")
            if 'exception' in log_event.message.lower() or log_event.exception:
                anomaly_type = AnomalyType.ERROR_SPIKE
            else:
                anomaly_type = AnomalyType.UNUSUAL_PATTERN
        
        # Check duration
        if log_event.duration_ms and log_event.duration_ms > 5000:
            reasons.append(f"High response time: {log_event.duration_ms}ms")
            anomaly_type = AnomalyType.PERFORMANCE_DEGRADATION
        
        # Check HTTP status
        if log_event.http_status and log_event.http_status >= 500:
            reasons.append(f"Server error status: {log_event.http_status}")
            anomaly_type = AnomalyType.ERROR_SPIKE
        elif log_event.http_status and log_event.http_status >= 400:
            reasons.append(f"Client error status: {log_event.http_status}")
            anomaly_type = AnomalyType.SECURITY_THREAT
        
        # Check for security indicators
        security_keywords = ['auth', 'login', 'password', 'token', 'security', 'attack', 'injection']
        if any(keyword in log_event.message.lower() for keyword in security_keywords):
            if log_event.log_level.value in ['ERROR', 'WARN']:
                reasons.append("Security-related error detected")
                anomaly_type = AnomalyType.SECURITY_THREAT
        
        # Add model-specific reasons
        anomalous_models = [name for name, pred in predictions.items() if pred]
        if anomalous_models:
            reasons.append(f"Flagged by models: {', '.join(anomalous_models)}")
        
        if not reasons:
            reasons.append("Statistical anomaly detected by ML models")
        
        return anomaly_type, reasons
    
    def _generate_recommendations(self, log_event: LogEvent, anomaly_type: AnomalyType, score: float) -> List[str]:
        """Generate actionable recommendations"""
        recommendations = []
        
        if anomaly_type == AnomalyType.ERROR_SPIKE:
            recommendations.extend([
                f"Investigate {log_event.service_name} service health",
                "Check recent deployments or configuration changes",
                "Monitor error rates and patterns"
            ])
        elif anomaly_type == AnomalyType.PERFORMANCE_DEGRADATION:
            recommendations.extend([
                "Check resource utilization (CPU, memory, disk)",
                "Analyze database query performance",
                "Review network connectivity"
            ])
        elif anomaly_type == AnomalyType.SECURITY_THREAT:
            recommendations.extend([
                "Review authentication logs",
                "Check for potential security breaches",
                "Verify user permissions and access patterns"
            ])
        else:
            recommendations.extend([
                "Monitor service for similar patterns",
                "Check service dependencies",
                "Review application logs for context"
            ])
        
        if score > 0.9:
            recommendations.insert(0, "HIGH PRIORITY: Immediate investigation required")
        
        return recommendations
    
    def _fallback_scoring(self, log_event: LogEvent, start_time: float) -> AnomalyResult:
        """Fallback scoring when ML models are not available"""
        score = 0.0
        reasons = []
        anomaly_type = AnomalyType.UNKNOWN
        
        # Heuristic scoring
        if log_event.log_level.value == 'ERROR':
            score += 0.7
            reasons.append("Error level log")
            anomaly_type = AnomalyType.ERROR_SPIKE
        elif log_event.log_level.value == 'FATAL':
            score += 0.9
            reasons.append("Fatal level log")
            anomaly_type = AnomalyType.ERROR_SPIKE
        elif log_event.log_level.value == 'WARN':
            score += 0.4
            reasons.append("Warning level log")
        
        if log_event.exception:
            score += 0.3
            reasons.append("Exception present")
        
        if log_event.http_status and log_event.http_status >= 500:
            score += 0.4
            reasons.append(f"Server error: {log_event.http_status}")
        
        if log_event.duration_ms and log_event.duration_ms > 5000:
            score += 0.3
            reasons.append("High response time")
            if anomaly_type == AnomalyType.UNKNOWN:
                anomaly_type = AnomalyType.PERFORMANCE_DEGRADATION
        
        score = min(score, 1.0)
        is_anomaly = bool(score > self.config['anomaly_threshold'])  # Ensure Python bool
        
        processing_time = float((time.time() - start_time) * 1000)  # Ensure Python float
        
        return AnomalyResult(
            log_id=log_event.log_id,
            is_anomaly=is_anomaly,
            anomaly_score=float(score),  # Ensure Python float
            confidence=0.6,  # Lower confidence for heuristic scoring
            anomaly_type=anomaly_type if is_anomaly else None,
            anomaly_reasons=reasons if is_anomaly else [],
            features_analyzed=['heuristic_rules'],
            model_version=f"{self.config['model_version']}_fallback",
            processing_time_ms=processing_time,
            threshold_used=self.config['anomaly_threshold'],
            recommendations=self._generate_recommendations(log_event, anomaly_type, score),
            metadata={'scoring_method': 'heuristic_fallback'}
        )
    
    async def get_model_config(self) -> Dict[str, Any]:
        """Get current model configuration"""
        return self.config.copy()
    
    async def update_model_config(self, config: Dict[str, Any]):
        """Update model configuration"""
        if 'anomaly_threshold' in config:
            self.config['anomaly_threshold'] = config['anomaly_threshold']
        if 'confidence_threshold' in config:
            self.config['confidence_threshold'] = config['confidence_threshold']
        
        logger.info(f"Model configuration updated: {self.config}") 