-- Create additional databases if needed
CREATE DATABASE incident_auth;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE incident_platform TO postgres;
GRANT ALL PRIVILEGES ON DATABASE incident_auth TO postgres;

-- Create extensions
\c incident_platform;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c incident_auth;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; 