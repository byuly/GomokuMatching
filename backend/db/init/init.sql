-- db/init/01_create_tables.sql
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create the main database schema
CREATE SCHEMA IF NOT EXISTS gomoku;

-- Set the search path
SET search_path TO gomoku, public;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA gomoku TO gomoku_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA gomoku TO gomoku_user;