"""
AI Service for Incident Platform
Provides anomaly detection capabilities using machine learning models
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from starlette.responses import Response
import logging
import asyncio
from datetime import datetime
from typing import List, Dict, Any, Optional

from models.log_event import LogEvent, AnomalyResult
from services.anomaly_detector import AnomalyDetectorService
from services.model_manager import ModelManager
from config.settings import get_settings

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Prometheus metrics
REQUEST_COUNT = Counter('ai_service_requests_total', 'Total requests', ['method', 'endpoint'])
REQUEST_DURATION = Histogram('ai_service_request_duration_seconds', 'Request duration')
ANOMALY_PREDICTIONS = Counter('ai_service_anomaly_predictions_total', 'Anomaly predictions', ['result'])
MODEL_INFERENCE_TIME = Histogram('ai_service_model_inference_seconds', 'Model inference time')

# Initialize FastAPI app
app = FastAPI(
    title="AI Service - Incident Platform",
    description="AI-powered anomaly detection service for log analysis",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global services
settings = get_settings()
model_manager: ModelManager = None
anomaly_detector: AnomalyDetectorService = None

@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    global model_manager, anomaly_detector
    
    try:
        logger.info("Starting AI Service...")
        
        # Initialize model manager
        model_manager = ModelManager()
        await model_manager.initialize()
        
        # Initialize anomaly detector
        anomaly_detector = AnomalyDetectorService(model_manager)
        await anomaly_detector.initialize()
        
        logger.info("AI Service started successfully")
        
    except Exception as e:
        logger.error(f"Failed to start AI Service: {e}")
        raise

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    logger.info("Shutting down AI Service...")
    
    if model_manager:
        await model_manager.cleanup()
    
    logger.info("AI Service shutdown complete")

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    REQUEST_COUNT.labels(method="GET", endpoint="/health").inc()
    
    try:
        # Check if services are initialized
        if not model_manager or not anomaly_detector:
            raise HTTPException(status_code=503, detail="Services not initialized")
        
        # Check model status
        model_status = await model_manager.get_health_status()
        
        return {
            "status": "healthy",
            "timestamp": datetime.utcnow().isoformat(),
            "service": "ai-service",
            "version": "1.0.0",
            "models": model_status
        }
        
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        raise HTTPException(status_code=503, detail=f"Service unhealthy: {str(e)}")

@app.post("/predict/anomaly", response_model=AnomalyResult)
async def predict_anomaly(log_event: LogEvent):
    """Predict if a log event is anomalous"""
    REQUEST_COUNT.labels(method="POST", endpoint="/predict/anomaly").inc()
    
    with REQUEST_DURATION.time():
        try:
            logger.debug(f"Processing anomaly prediction for log: {log_event.log_id}")
            
            # Perform anomaly detection
            with MODEL_INFERENCE_TIME.time():
                result = await anomaly_detector.predict_anomaly(log_event)
            
            # Update metrics
            ANOMALY_PREDICTIONS.labels(result="anomaly" if result.is_anomaly else "normal").inc()
            
            logger.debug(f"Anomaly prediction completed: {result.anomaly_score}")
            return result
            
        except Exception as e:
            logger.error(f"Anomaly prediction failed: {e}")
            raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

@app.post("/predict/batch", response_model=List[AnomalyResult])
async def predict_batch_anomalies(log_events: List[LogEvent]):
    """Predict anomalies for multiple log events"""
    REQUEST_COUNT.labels(method="POST", endpoint="/predict/batch").inc()
    
    with REQUEST_DURATION.time():
        try:
            logger.info(f"Processing batch anomaly prediction for {len(log_events)} logs")
            
            # Process batch
            results = await anomaly_detector.predict_batch_anomalies(log_events)
            
            # Update metrics
            for result in results:
                ANOMALY_PREDICTIONS.labels(result="anomaly" if result.is_anomaly else "normal").inc()
            
            logger.info(f"Batch prediction completed: {len(results)} results")
            return results
            
        except Exception as e:
            logger.error(f"Batch prediction failed: {e}")
            raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")

@app.get("/models/status")
async def get_model_status():
    """Get status of ML models"""
    REQUEST_COUNT.labels(method="GET", endpoint="/models/status").inc()
    
    try:
        if not model_manager:
            raise HTTPException(status_code=503, detail="Model manager not initialized")
        
        status = await model_manager.get_detailed_status()
        return status
        
    except Exception as e:
        logger.error(f"Failed to get model status: {e}")
        raise HTTPException(status_code=500, detail=f"Status check failed: {str(e)}")

@app.post("/models/retrain")
async def retrain_models(background_tasks: BackgroundTasks):
    """Trigger model retraining"""
    REQUEST_COUNT.labels(method="POST", endpoint="/models/retrain").inc()
    
    try:
        if not model_manager:
            raise HTTPException(status_code=503, detail="Model manager not initialized")
        
        # Start retraining in background
        background_tasks.add_task(model_manager.retrain_models)
        
        return {
            "status": "accepted",
            "message": "Model retraining started",
            "timestamp": datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Failed to start model retraining: {e}")
        raise HTTPException(status_code=500, detail=f"Retraining failed: {str(e)}")

@app.get("/models/config")
async def get_model_config():
    """Get model configuration"""
    REQUEST_COUNT.labels(method="GET", endpoint="/models/config").inc()
    
    try:
        if not anomaly_detector:
            raise HTTPException(status_code=503, detail="Anomaly detector not initialized")
        
        config = await anomaly_detector.get_model_config()
        return config
        
    except Exception as e:
        logger.error(f"Failed to get model config: {e}")
        raise HTTPException(status_code=500, detail=f"Config retrieval failed: {str(e)}")

@app.post("/models/config")
async def update_model_config(config: Dict[str, Any]):
    """Update model configuration"""
    REQUEST_COUNT.labels(method="POST", endpoint="/models/config").inc()
    
    try:
        if not anomaly_detector:
            raise HTTPException(status_code=503, detail="Anomaly detector not initialized")
        
        await anomaly_detector.update_model_config(config)
        
        return {
            "status": "success",
            "message": "Model configuration updated",
            "timestamp": datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Failed to update model config: {e}")
        raise HTTPException(status_code=500, detail=f"Config update failed: {str(e)}")

@app.get("/metrics")
async def get_metrics():
    """Prometheus metrics endpoint"""
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "AI Service - Incident Platform",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "predict_anomaly": "/predict/anomaly",
            "predict_batch": "/predict/batch",
            "model_status": "/models/status",
            "retrain": "/models/retrain",
            "config": "/models/config",
            "metrics": "/metrics",
            "docs": "/docs"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    ) 