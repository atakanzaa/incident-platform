// Switch to incident_tracker database
db = db.getSiblingDB('incident_tracker');

// Create collections with indexes
db.createCollection('incidents');
db.createCollection('incident_summaries');

// Create indexes for incidents collection
db.incidents.createIndex({ "alertId": 1 }, { unique: true });
db.incidents.createIndex({ "serviceName": 1 });
db.incidents.createIndex({ "severity": 1 });
db.incidents.createIndex({ "status": 1 });
db.incidents.createIndex({ "createdAt": 1 });
db.incidents.createIndex({ "hostname": 1 });
db.incidents.createIndex({ "anomalyScore": 1 });
db.incidents.createIndex({ "correlationId": 1 });
db.incidents.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

// Create text index for search
db.incidents.createIndex({
    "title": "text",
    "description": "text",
    "anomalyReasons": "text"
});

// Create compound indexes
db.incidents.createIndex({ "serviceName": 1, "createdAt": 1 });
db.incidents.createIndex({ "serviceName": 1, "status": 1 });
db.incidents.createIndex({ "severity": 1, "createdAt": 1 });

// Create indexes for incident_summaries collection
db.incident_summaries.createIndex({ "windowStart": 1 });
db.incident_summaries.createIndex({ "windowEnd": 1 });
db.incident_summaries.createIndex({ "serviceName": 1 });
db.incident_summaries.createIndex({ "serviceName": 1, "windowStart": 1 });
db.incident_summaries.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

print('MongoDB incident_tracker database initialized successfully'); 