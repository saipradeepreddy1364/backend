CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    submission_count INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS code_submissions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    code TEXT NOT NULL,
    language VARCHAR(50) DEFAULT 'java',
    class_name VARCHAR(255),
    method_name VARCHAR(255),
    input TEXT,
    output TEXT,
    error TEXT,
    success BOOLEAN,
    compilation_time BIGINT,
    execution_time BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);