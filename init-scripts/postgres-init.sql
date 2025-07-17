-- Create additional databases if needed
CREATE DATABASE auth_service;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE incident_platform TO postgres;
GRANT ALL PRIVILEGES ON DATABASE auth_service TO postgres;

-- Create extensions
\c incident_platform;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c auth_service;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; 