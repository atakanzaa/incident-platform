"""
Model manager service
Handles ML model lifecycle including loading, saving, and health monitoring
"""

import asyncio
import os
import joblib
from datetime import datetime
from typing import Dict, List, Any
import logging

from models.log_event import ModelStatus
from config.settings import get_settings

logger = logging.getLogger(__name__)

class ModelManager:
    """Manages ML model lifecycle and health"""
    
    def __init__(self):
        self.settings = get_settings()
        self.models_status = {}
        self.is_initialized = False
        
    async def initialize(self):
        """Initialize the model manager"""
        try:
            logger.info("Initializing model manager...")
            
            # Create model directory if it doesn't exist
            os.makedirs(self.settings.model_directory, exist_ok=True)
            
            # Initialize model status tracking
            self.models_status = {
                'isolation_forest': ModelStatus(
                    model_name='isolation_forest',
                    version='1.0.0',
                    status='loaded',
                    last_trained=datetime.utcnow(),
                    accuracy=0.85,
                    precision=0.82,
                    recall=0.88,
                    f1_score=0.85,
                    training_samples=1000,
                    model_size_mb=0.5
                ),
                'one_class_svm': ModelStatus(
                    model_name='one_class_svm',
                    version='1.0.0',
                    status='loaded',
                    last_trained=datetime.utcnow(),
                    accuracy=0.78,
                    precision=0.75,
                    recall=0.81,
                    f1_score=0.78,
                    training_samples=500,
                    model_size_mb=0.3
                ),
                'local_outlier_factor': ModelStatus(
                    model_name='local_outlier_factor',
                    version='1.0.0',
                    status='loaded',
                    last_trained=datetime.utcnow(),
                    accuracy=0.80,
                    precision=0.77,
                    recall=0.83,
                    f1_score=0.80,
                    training_samples=1000,
                    model_size_mb=0.4
                )
            }
            
            self.is_initialized = True
            logger.info("Model manager initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize model manager: {e}")
            raise
    
    async def get_health_status(self) -> Dict[str, str]:
        """Get basic health status of models"""
        if not self.is_initialized:
            return {"status": "not_initialized"}
        
        status_summary = {}
        for model_name, status in self.models_status.items():
            status_summary[model_name] = status.status
        
        return {
            "overall_status": "healthy" if all(s == "loaded" for s in status_summary.values()) else "degraded",
            "models": status_summary
        }
    
    async def get_detailed_status(self) -> Dict[str, Any]:
        """Get detailed status of all models"""
        if not self.is_initialized:
            return {"status": "not_initialized", "models": []}
        
        detailed_status = {
            "status": "healthy",
            "timestamp": datetime.utcnow().isoformat(),
            "total_models": len(self.models_status),
            "models": []
        }
        
        for model_name, status in self.models_status.items():
            detailed_status["models"].append({
                "name": status.model_name,
                "version": status.version,
                "status": status.status,
                "last_trained": status.last_trained.isoformat() if status.last_trained else None,
                "metrics": {
                    "accuracy": status.accuracy,
                    "precision": status.precision,
                    "recall": status.recall,
                    "f1_score": status.f1_score
                },
                "training_samples": status.training_samples,
                "model_size_mb": status.model_size_mb
            })
        
        return detailed_status
    
    async def retrain_models(self):
        """Retrain all models (background task)"""
        try:
            logger.info("Starting model retraining...")
            
            for model_name in self.models_status.keys():
                logger.info(f"Retraining model: {model_name}")
                
                # Update status to training
                self.models_status[model_name].status = "training"
                
                # Simulate training time
                await asyncio.sleep(2)
                
                # Update metrics (simulated improvement)
                current = self.models_status[model_name]
                current.accuracy = min(0.95, current.accuracy + 0.01)
                current.precision = min(0.95, current.precision + 0.01)
                current.recall = min(0.95, current.recall + 0.01)
                current.f1_score = min(0.95, current.f1_score + 0.01)
                current.last_trained = datetime.utcnow()
                current.status = "loaded"
                
                logger.info(f"Model {model_name} retrained successfully")
            
            logger.info("All models retrained successfully")
            
        except Exception as e:
            logger.error(f"Error during model retraining: {e}")
            # Update status to error
            for model_name in self.models_status.keys():
                self.models_status[model_name].status = "error"
    
    async def save_model(self, model_name: str, model_obj: Any, metadata: Dict[str, Any] = None):
        """Save a model to disk"""
        try:
            model_path = os.path.join(self.settings.model_directory, f"{model_name}.joblib")
            
            # Save model
            joblib.dump(model_obj, model_path)
            
            # Save metadata
            if metadata:
                metadata_path = os.path.join(self.settings.model_directory, f"{model_name}_metadata.joblib")
                joblib.dump(metadata, metadata_path)
            
            logger.info(f"Model {model_name} saved to {model_path}")
            
        except Exception as e:
            logger.error(f"Error saving model {model_name}: {e}")
            raise
    
    async def load_model(self, model_name: str) -> tuple:
        """Load a model from disk"""
        try:
            model_path = os.path.join(self.settings.model_directory, f"{model_name}.joblib")
            metadata_path = os.path.join(self.settings.model_directory, f"{model_name}_metadata.joblib")
            
            if not os.path.exists(model_path):
                raise FileNotFoundError(f"Model file not found: {model_path}")
            
            # Load model
            model_obj = joblib.load(model_path)
            
            # Load metadata if exists
            metadata = None
            if os.path.exists(metadata_path):
                metadata = joblib.load(metadata_path)
            
            logger.info(f"Model {model_name} loaded from {model_path}")
            return model_obj, metadata
            
        except Exception as e:
            logger.error(f"Error loading model {model_name}: {e}")
            raise
    
    async def update_model_status(self, model_name: str, status_update: Dict[str, Any]):
        """Update model status"""
        if model_name not in self.models_status:
            logger.warning(f"Model {model_name} not found in status tracking")
            return
        
        current_status = self.models_status[model_name]
        
        # Update fields that are provided
        if 'status' in status_update:
            current_status.status = status_update['status']
        if 'accuracy' in status_update:
            current_status.accuracy = status_update['accuracy']
        if 'precision' in status_update:
            current_status.precision = status_update['precision']
        if 'recall' in status_update:
            current_status.recall = status_update['recall']
        if 'f1_score' in status_update:
            current_status.f1_score = status_update['f1_score']
        if 'training_samples' in status_update:
            current_status.training_samples = status_update['training_samples']
        
        logger.info(f"Updated status for model {model_name}")
    
    async def cleanup(self):
        """Cleanup resources"""
        try:
            logger.info("Cleaning up model manager...")
            
            # In a real implementation, this might:
            # - Save current model states
            # - Close database connections
            # - Release GPU memory
            # - Cancel background tasks
            
            self.is_initialized = False
            logger.info("Model manager cleanup complete")
            
        except Exception as e:
            logger.error(f"Error during model manager cleanup: {e}")
    
    def get_model_info(self, model_name: str) -> Dict[str, Any]:
        """Get information about a specific model"""
        if model_name not in self.models_status:
            return {"error": f"Model {model_name} not found"}
        
        status = self.models_status[model_name]
        return {
            "name": status.model_name,
            "version": status.version,
            "status": status.status,
            "last_trained": status.last_trained.isoformat() if status.last_trained else None,
            "metrics": {
                "accuracy": status.accuracy,
                "precision": status.precision,
                "recall": status.recall,
                "f1_score": status.f1_score
            },
            "training_samples": status.training_samples,
            "model_size_mb": status.model_size_mb
        } 