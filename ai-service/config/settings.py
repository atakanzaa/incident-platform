"""
Configuration settings for AI Service
"""

import os
from functools import lru_cache
from pydantic_settings import BaseSettings
from pydantic import Field
from typing import List, Dict, Any

class Settings(BaseSettings):
    """Application settings"""
    
    # Pydantic configuration to resolve model_ namespace conflicts
    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "protected_namespaces": ()
    }
    
    # Service Configuration
    service_name: str = Field(default="ai-service", env="SERVICE_NAME")
    version: str = Field(default="1.0.0", env="SERVICE_VERSION")
    log_level: str = Field(default="INFO", env="LOG_LEVEL")
    
    # API Configuration
    api_host: str = Field(default="0.0.0.0", env="API_HOST")
    api_port: int = Field(default=8000, env="API_PORT")
    api_workers: int = Field(default=1, env="API_WORKERS")
    
    # Model Configuration
    model_directory: str = Field(default="./models", env="MODEL_DIRECTORY")
    model_cache_size: int = Field(default=3, env="MODEL_CACHE_SIZE")
    default_model_name: str = Field(default="isolation_forest", env="DEFAULT_MODEL_NAME")
    
    # Anomaly Detection Settings
    default_anomaly_threshold: float = Field(default=0.7, env="ANOMALY_THRESHOLD")
    default_confidence_threshold: float = Field(default=0.8, env="CONFIDENCE_THRESHOLD")
    max_batch_size: int = Field(default=1000, env="MAX_BATCH_SIZE")
    
    # Feature Engineering
    feature_extraction_enabled: bool = Field(default=True, env="FEATURE_EXTRACTION_ENABLED")
    text_analysis_enabled: bool = Field(default=True, env="TEXT_ANALYSIS_ENABLED")
    temporal_analysis_enabled: bool = Field(default=True, env="TEMPORAL_ANALYSIS_ENABLED")
    
    # Model Training
    auto_retrain_enabled: bool = Field(default=True, env="AUTO_RETRAIN_ENABLED")
    retrain_interval_hours: int = Field(default=24, env="RETRAIN_INTERVAL_HOURS")
    min_training_samples: int = Field(default=1000, env="MIN_TRAINING_SAMPLES")
    max_training_samples: int = Field(default=100000, env="MAX_TRAINING_SAMPLES")
    
    # Performance Settings
    async_processing: bool = Field(default=True, env="ASYNC_PROCESSING")
    enable_caching: bool = Field(default=True, env="ENABLE_CACHING")
    cache_ttl_seconds: int = Field(default=3600, env="CACHE_TTL_SECONDS")
    
    # Redis Configuration (for caching)
    redis_host: str = Field(default="localhost", env="REDIS_HOST")
    redis_port: int = Field(default=6379, env="REDIS_PORT")
    redis_db: int = Field(default=0, env="REDIS_DB")
    redis_password: str = Field(default="", env="REDIS_PASSWORD")
    
    # Monitoring and Metrics
    metrics_enabled: bool = Field(default=True, env="METRICS_ENABLED")
    prometheus_port: int = Field(default=8001, env="PROMETHEUS_PORT")
    health_check_interval: int = Field(default=30, env="HEALTH_CHECK_INTERVAL")
    
    # Security
    api_key_required: bool = Field(default=False, env="API_KEY_REQUIRED")
    api_keys: List[str] = Field(default=[], env="API_KEYS")
    rate_limit_enabled: bool = Field(default=True, env="RATE_LIMIT_ENABLED")
    rate_limit_per_minute: int = Field(default=100, env="RATE_LIMIT_PER_MINUTE")
    
    # Data Sources
    training_data_path: str = Field(default="./data/training", env="TRAINING_DATA_PATH")
    model_backup_path: str = Field(default="./data/backups", env="MODEL_BACKUP_PATH")
    
    # Feature Engineering Parameters
    max_message_length: int = Field(default=10000, env="MAX_MESSAGE_LENGTH")
    text_vectorizer_max_features: int = Field(default=10000, env="TEXT_VECTORIZER_MAX_FEATURES")
    time_window_minutes: int = Field(default=5, env="TIME_WINDOW_MINUTES")
    
    # Model-specific settings
    isolation_forest_contamination: float = Field(default=0.1, env="ISOLATION_FOREST_CONTAMINATION")
    isolation_forest_n_estimators: int = Field(default=100, env="ISOLATION_FOREST_N_ESTIMATORS")
    isolation_forest_max_samples: str = Field(default="auto", env="ISOLATION_FOREST_MAX_SAMPLES")
    
    one_class_svm_nu: float = Field(default=0.1, env="ONE_CLASS_SVM_NU")
    one_class_svm_kernel: str = Field(default="rbf", env="ONE_CLASS_SVM_KERNEL")
    one_class_svm_gamma: str = Field(default="scale", env="ONE_CLASS_SVM_GAMMA")
    
    # Ensemble settings
    ensemble_voting: str = Field(default="soft", env="ENSEMBLE_VOTING")
    ensemble_weights: List[float] = Field(default=[0.4, 0.3, 0.3], env="ENSEMBLE_WEIGHTS")

@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()

# Model configurations
MODEL_CONFIGS = {
    "isolation_forest": {
        "name": "Isolation Forest",
        "description": "Tree-based anomaly detection algorithm",
        "parameters": {
            "contamination": 0.1,
            "n_estimators": 100,
            "max_samples": "auto",
            "max_features": 1.0,
            "random_state": 42
        },
        "suitable_for": ["general_anomalies", "outlier_detection"]
    },
    "one_class_svm": {
        "name": "One-Class SVM",
        "description": "Support Vector Machine for novelty detection",
        "parameters": {
            "nu": 0.1,
            "kernel": "rbf",
            "gamma": "scale"
        },
        "suitable_for": ["high_dimensional_data", "non_linear_patterns"]
    },
    "local_outlier_factor": {
        "name": "Local Outlier Factor",
        "description": "Local density-based anomaly detection",
        "parameters": {
            "n_neighbors": 20,
            "contamination": 0.1,
            "novelty": True
        },
        "suitable_for": ["density_based_anomalies", "local_outliers"]
    },
    "ensemble": {
        "name": "Ensemble Detector",
        "description": "Combination of multiple algorithms",
        "parameters": {
            "base_detectors": ["isolation_forest", "one_class_svm"],
            "voting": "soft",
            "weights": [0.6, 0.4]
        },
        "suitable_for": ["robust_detection", "high_accuracy"]
    }
}

# Feature extraction configuration
FEATURE_CONFIG = {
    "text_features": {
        "enabled": True,
        "max_features": 10000,
        "ngram_range": (1, 2),
        "min_df": 2,
        "max_df": 0.95,
        "stop_words": "english"
    },
    "temporal_features": {
        "enabled": True,
        "time_windows": [1, 5, 15, 60],  # minutes
        "aggregations": ["count", "mean", "std", "max"]
    },
    "categorical_features": {
        "enabled": True,
        "encoding": "one_hot",
        "handle_unknown": "ignore"
    },
    "numerical_features": {
        "enabled": True,
        "scaling": "standard",
        "handle_missing": "median"
    }
}

# Logging configuration
LOGGING_CONFIG = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "default": {
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        },
        "detailed": {
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(module)s - %(funcName)s - %(lineno)d - %(message)s"
        }
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "level": "INFO",
            "formatter": "default",
            "stream": "ext://sys.stdout"
        },
        "file": {
            "class": "logging.handlers.RotatingFileHandler",
            "level": "DEBUG",
            "formatter": "detailed",
            "filename": "logs/ai_service.log",
            "maxBytes": 10485760,  # 10MB
            "backupCount": 5
        }
    },
    "loggers": {
        "": {
            "level": "INFO",
            "handlers": ["console", "file"],
            "propagate": False
        },
        "uvicorn": {
            "level": "INFO",
            "handlers": ["console"],
            "propagate": False
        }
    }
} 