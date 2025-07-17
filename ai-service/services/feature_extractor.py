"""
Feature extraction service for log events
Converts raw log events into numerical features for ML models
"""

import re
import hashlib
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import List, Dict, Any, Tuple
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import LabelEncoder, StandardScaler, OneHotEncoder
import logging

from models.log_event import LogEvent, LogLevel
from config.settings import get_settings, FEATURE_CONFIG

logger = logging.getLogger(__name__)

class FeatureExtractor:
    """Extract features from log events for anomaly detection"""
    
    def __init__(self):
        self.settings = get_settings()
        self.text_vectorizer = None
        self.label_encoders = {}
        self.scaler = StandardScaler()
        self.one_hot_encoders = {}
        self.feature_names = []
        self.is_fitted = False
        
        # Compiled regex patterns for efficient text processing
        self.patterns = {
            'ip_address': re.compile(r'\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b'),
            'email': re.compile(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'),
            'url': re.compile(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+'),
            'uuid': re.compile(r'\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b'),
            'number': re.compile(r'\b\d+\b'),
            'stack_trace': re.compile(r'at\s+[\w\.\$]+\([\w\.:]*\)'),
            'exception': re.compile(r'Exception|Error|Throwable', re.IGNORECASE)
        }
        
    async def initialize(self):
        """Initialize the feature extractor"""
        try:
            # Initialize text vectorizer
            if FEATURE_CONFIG['text_features']['enabled']:
                self.text_vectorizer = TfidfVectorizer(
                    max_features=FEATURE_CONFIG['text_features']['max_features'],
                    ngram_range=FEATURE_CONFIG['text_features']['ngram_range'],
                    min_df=FEATURE_CONFIG['text_features']['min_df'],
                    max_df=FEATURE_CONFIG['text_features']['max_df'],
                    stop_words=FEATURE_CONFIG['text_features']['stop_words'],
                    lowercase=True,
                    strip_accents='unicode'
                )
            
            logger.info("Feature extractor initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize feature extractor: {e}")
            raise
    
    async def fit(self, log_events: List[LogEvent]) -> None:
        """Fit the feature extractor on training data"""
        try:
            logger.info(f"Fitting feature extractor on {len(log_events)} log events")
            
            # Convert to DataFrame for easier processing
            df = self._log_events_to_dataframe(log_events)
            
            # Fit text features
            if FEATURE_CONFIG['text_features']['enabled'] and self.text_vectorizer:
                messages = df['message'].fillna('').astype(str)
                processed_messages = [self._preprocess_text(msg) for msg in messages]
                self.text_vectorizer.fit(processed_messages)
            
            # Fit categorical encoders
            if FEATURE_CONFIG['categorical_features']['enabled']:
                categorical_columns = ['service_name', 'hostname', 'log_level', 'logger']
                for col in categorical_columns:
                    if col in df.columns:
                        if FEATURE_CONFIG['categorical_features']['encoding'] == 'label':
                            encoder = LabelEncoder()
                            encoder.fit(df[col].fillna('unknown').astype(str))
                            self.label_encoders[col] = encoder
                        elif FEATURE_CONFIG['categorical_features']['encoding'] == 'one_hot':
                            encoder = OneHotEncoder(
                                handle_unknown=FEATURE_CONFIG['categorical_features']['handle_unknown'],
                                sparse=False
                            )
                            encoder.fit(df[[col]].fillna('unknown').astype(str))
                            self.one_hot_encoders[col] = encoder
            
            # Fit numerical scaler
            if FEATURE_CONFIG['numerical_features']['enabled']:
                numerical_features = self._extract_numerical_features(df)
                if len(numerical_features) > 0:
                    self.scaler.fit(numerical_features)
            
            self.is_fitted = True
            logger.info("Feature extractor fitted successfully")
            
        except Exception as e:
            logger.error(f"Failed to fit feature extractor: {e}")
            raise
    
    async def transform(self, log_events: List[LogEvent]) -> Tuple[np.ndarray, List[str]]:
        """Transform log events into feature vectors"""
        try:
            if not self.is_fitted:
                raise ValueError("Feature extractor must be fitted before transform")
            
            # Convert to DataFrame
            df = self._log_events_to_dataframe(log_events)
            
            # Extract different types of features
            features_list = []
            feature_names = []
            
            # Basic features
            basic_features, basic_names = self._extract_basic_features(df)
            features_list.append(basic_features)
            feature_names.extend(basic_names)
            
            # Text features
            if FEATURE_CONFIG['text_features']['enabled'] and self.text_vectorizer:
                text_features, text_names = self._extract_text_features(df)
                features_list.append(text_features)
                feature_names.extend(text_names)
            
            # Temporal features
            if FEATURE_CONFIG['temporal_features']['enabled']:
                temporal_features, temporal_names = self._extract_temporal_features(df)
                features_list.append(temporal_features)
                feature_names.extend(temporal_names)
            
            # Categorical features
            if FEATURE_CONFIG['categorical_features']['enabled']:
                cat_features, cat_names = self._extract_categorical_features(df)
                features_list.append(cat_features)
                feature_names.extend(cat_names)
            
            # Numerical features
            if FEATURE_CONFIG['numerical_features']['enabled']:
                num_features, num_names = self._extract_numerical_features_transformed(df)
                features_list.append(num_features)
                feature_names.extend(num_names)
            
            # Pattern-based features
            pattern_features, pattern_names = self._extract_pattern_features(df)
            features_list.append(pattern_features)
            feature_names.extend(pattern_names)
            
            # Combine all features
            if features_list:
                combined_features = np.hstack(features_list)
            else:
                combined_features = np.array([]).reshape(len(log_events), 0)
            
            self.feature_names = feature_names
            
            logger.debug(f"Extracted {combined_features.shape[1]} features from {len(log_events)} log events")
            return combined_features, feature_names
            
        except Exception as e:
            logger.error(f"Failed to transform log events: {e}")
            raise
    
    def _log_events_to_dataframe(self, log_events: List[LogEvent]) -> pd.DataFrame:
        """Convert log events to pandas DataFrame"""
        data = []
        for event in log_events:
            data.append({
                'log_id': event.log_id,
                'timestamp': event.timestamp,
                'service_name': event.service_name,
                'hostname': event.hostname,
                'log_level': event.log_level.value,
                'message': event.message,
                'thread': event.thread,
                'logger': event.logger,
                'method': event.method,
                'line_number': event.line_number,
                'exception': event.exception,
                'stack_trace': event.stack_trace,
                'user_id': event.user_id,
                'session_id': event.session_id,
                'request_id': event.request_id,
                'http_method': event.http_method,
                'http_status': event.http_status,
                'endpoint': event.endpoint,
                'duration_ms': event.duration_ms,
                'metadata': event.metadata or {}
            })
        return pd.DataFrame(data)
    
    def _extract_basic_features(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract basic statistical features"""
        features = []
        names = []
        
        # Log level severity (higher for more severe)
        log_level_severity = df['log_level'].map({
            'TRACE': 0, 'DEBUG': 1, 'INFO': 2, 
            'WARN': 3, 'ERROR': 4, 'FATAL': 5
        }).fillna(2).values
        features.append(log_level_severity.reshape(-1, 1))
        names.append('log_level_severity')
        
        # Message length
        message_lengths = df['message'].fillna('').str.len().values
        features.append(message_lengths.reshape(-1, 1))
        names.append('message_length')
        
        # Has exception
        has_exception = (~df['exception'].isna()).astype(int).values
        features.append(has_exception.reshape(-1, 1))
        names.append('has_exception')
        
        # Has stack trace
        has_stack_trace = (~df['stack_trace'].isna()).astype(int).values
        features.append(has_stack_trace.reshape(-1, 1))
        names.append('has_stack_trace')
        
        # HTTP status category (if available)
        if 'http_status' in df.columns:
            http_status_category = df['http_status'].fillna(0).apply(
                lambda x: 1 if 200 <= x < 300 else 2 if 300 <= x < 400 else 
                         3 if 400 <= x < 500 else 4 if 500 <= x else 0
            ).values
            features.append(http_status_category.reshape(-1, 1))
            names.append('http_status_category')
        
        if features:
            return np.hstack(features), names
        else:
            return np.array([]).reshape(len(df), 0), names
    
    def _extract_text_features(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract text-based features using TF-IDF"""
        if not self.text_vectorizer:
            return np.array([]).reshape(len(df), 0), []
        
        messages = df['message'].fillna('').astype(str)
        processed_messages = [self._preprocess_text(msg) for msg in messages]
        
        # Transform using fitted vectorizer
        text_features = self.text_vectorizer.transform(processed_messages).toarray()
        
        # Create feature names
        feature_names = [f'text_tfidf_{i}' for i in range(text_features.shape[1])]
        
        return text_features, feature_names
    
    def _extract_temporal_features(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract time-based features"""
        features = []
        names = []
        
        timestamps = pd.to_datetime(df['timestamp'])
        
        # Hour of day
        hour_of_day = timestamps.dt.hour.values
        features.append(hour_of_day.reshape(-1, 1))
        names.append('hour_of_day')
        
        # Day of week
        day_of_week = timestamps.dt.dayofweek.values
        features.append(day_of_week.reshape(-1, 1))
        names.append('day_of_week')
        
        # Is weekend
        is_weekend = (timestamps.dt.dayofweek >= 5).astype(int).values
        features.append(is_weekend.reshape(-1, 1))
        names.append('is_weekend')
        
        # Time since epoch (normalized)
        time_since_epoch = timestamps.astype(np.int64) // 10**9
        time_since_epoch_norm = (time_since_epoch - time_since_epoch.min()) / (time_since_epoch.max() - time_since_epoch.min() + 1e-8)
        features.append(time_since_epoch_norm.values.reshape(-1, 1))
        names.append('time_since_epoch_norm')
        
        if features:
            return np.hstack(features), names
        else:
            return np.array([]).reshape(len(df), 0), names
    
    def _extract_categorical_features(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract categorical features"""
        features = []
        names = []
        
        categorical_columns = ['service_name', 'hostname', 'log_level', 'logger']
        
        for col in categorical_columns:
            if col not in df.columns:
                continue
                
            values = df[col].fillna('unknown').astype(str)
            
            if FEATURE_CONFIG['categorical_features']['encoding'] == 'label':
                if col in self.label_encoders:
                    # Handle unknown categories
                    encoded_values = []
                    for val in values:
                        try:
                            encoded_values.append(self.label_encoders[col].transform([val])[0])
                        except ValueError:
                            encoded_values.append(-1)  # Unknown category
                    features.append(np.array(encoded_values).reshape(-1, 1))
                    names.append(f'{col}_encoded')
                    
            elif FEATURE_CONFIG['categorical_features']['encoding'] == 'one_hot':
                if col in self.one_hot_encoders:
                    encoded_features = self.one_hot_encoders[col].transform(values.values.reshape(-1, 1))
                    features.append(encoded_features)
                    n_categories = encoded_features.shape[1]
                    names.extend([f'{col}_onehot_{i}' for i in range(n_categories)])
        
        if features:
            return np.hstack(features), names
        else:
            return np.array([]).reshape(len(df), 0), names
    
    def _extract_numerical_features(self, df: pd.DataFrame) -> np.ndarray:
        """Extract numerical features for fitting"""
        features = []
        
        # Duration (if available)
        if 'duration_ms' in df.columns:
            duration = df['duration_ms'].fillna(0).values
            features.append(duration.reshape(-1, 1))
        
        # Line number (if available)
        if 'line_number' in df.columns:
            line_numbers = df['line_number'].fillna(0).values
            features.append(line_numbers.reshape(-1, 1))
        
        if features:
            return np.hstack(features)
        else:
            return np.array([]).reshape(len(df), 0)
    
    def _extract_numerical_features_transformed(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract and transform numerical features"""
        features = []
        names = []
        
        # Duration (if available)
        if 'duration_ms' in df.columns:
            duration = df['duration_ms'].fillna(0).values.reshape(-1, 1)
            if hasattr(self.scaler, 'scale_') and len(self.scaler.scale_) > len(names):
                duration_scaled = (duration - self.scaler.mean_[len(names)]) / self.scaler.scale_[len(names)]
                features.append(duration_scaled)
            else:
                features.append(duration)
            names.append('duration_ms_scaled')
        
        # Line number (if available)
        if 'line_number' in df.columns:
            line_numbers = df['line_number'].fillna(0).values.reshape(-1, 1)
            if hasattr(self.scaler, 'scale_') and len(self.scaler.scale_) > len(names):
                line_numbers_scaled = (line_numbers - self.scaler.mean_[len(names)]) / self.scaler.scale_[len(names)]
                features.append(line_numbers_scaled)
            else:
                features.append(line_numbers)
            names.append('line_number_scaled')
        
        if features:
            return np.hstack(features), names
        else:
            return np.array([]).reshape(len(df), 0), names
    
    def _extract_pattern_features(self, df: pd.DataFrame) -> Tuple[np.ndarray, List[str]]:
        """Extract pattern-based features from log messages"""
        features = []
        names = []
        
        messages = df['message'].fillna('').astype(str)
        
        # Pattern counts
        for pattern_name, pattern in self.patterns.items():
            pattern_counts = messages.apply(lambda x: len(pattern.findall(x))).values
            features.append(pattern_counts.reshape(-1, 1))
            names.append(f'pattern_{pattern_name}_count')
        
        # Message entropy (complexity measure)
        message_entropy = messages.apply(self._calculate_entropy).values
        features.append(message_entropy.reshape(-1, 1))
        names.append('message_entropy')
        
        if features:
            return np.hstack(features), names
        else:
            return np.array([]).reshape(len(df), 0), names
    
    def _preprocess_text(self, text: str) -> str:
        """Preprocess text for feature extraction"""
        if not text:
            return ""
        
        # Limit length
        if len(text) > self.settings.max_message_length:
            text = text[:self.settings.max_message_length]
        
        # Remove or replace patterns
        for pattern_name, pattern in self.patterns.items():
            if pattern_name in ['ip_address', 'email', 'url', 'uuid']:
                text = pattern.sub(f'<{pattern_name.upper()}>', text)
            elif pattern_name == 'number':
                text = pattern.sub('<NUMBER>', text)
        
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text)
        
        return text.strip()
    
    def _calculate_entropy(self, text: str) -> float:
        """Calculate Shannon entropy of text"""
        if not text:
            return 0.0
        
        # Count character frequencies
        char_counts = {}
        for char in text:
            char_counts[char] = char_counts.get(char, 0) + 1
        
        # Calculate entropy
        text_length = len(text)
        entropy = 0.0
        for count in char_counts.values():
            probability = count / text_length
            if probability > 0:
                entropy -= probability * np.log2(probability)
        
        return entropy
    
    def get_feature_importance(self) -> Dict[str, float]:
        """Get feature importance scores (placeholder for now)"""
        if not self.feature_names:
            return {}
        
        # This would be populated by the trained model
        # For now, return equal importance
        return {name: 1.0 / len(self.feature_names) for name in self.feature_names}
    
    async def save_state(self, filepath: str):
        """Save the feature extractor state"""
        # Implementation for saving the fitted extractors
        pass
    
    async def load_state(self, filepath: str):
        """Load the feature extractor state"""
        # Implementation for loading the fitted extractors
        pass 