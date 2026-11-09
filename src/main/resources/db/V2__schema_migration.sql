-- =============================================================================
-- Apex University ERP - Non-Destructive PostgreSQL Schema Migration Script
-- Safely adds Phase 2-7 columns and tables without dropping existing data
-- =============================================================================

-- 1. Extend existing users table
ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

UPDATE users SET status = 'APPROVED' WHERE status IS NULL;

-- 2. Extend existing students table
ALTER TABLE IF EXISTS students
    ADD COLUMN IF NOT EXISTS enrollment_number VARCHAR(64),
    ADD COLUMN IF NOT EXISTS department VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id);

UPDATE students SET status = 'APPROVED' WHERE status IS NULL;

-- 3. Extend existing courses table
ALTER TABLE IF EXISTS courses
    ADD COLUMN IF NOT EXISTS semester VARCHAR(64),
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000);

-- 4. Extend existing notices table
ALTER TABLE IF EXISTS notices
    ADD COLUMN IF NOT EXISTS content VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT TRUE;

UPDATE notices SET is_public = TRUE WHERE is_public IS NULL;

-- 5. Create departments table if not exists
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    head_of_department VARCHAR(255),
    building VARCHAR(255),
    established_year INTEGER,
    total_seats INTEGER
);

-- 6. Create faculty table if not exists
CREATE TABLE IF NOT EXISTS faculty (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    designation VARCHAR(255),
    department VARCHAR(255),
    qualification VARCHAR(255),
    specialization VARCHAR(255),
    office VARCHAR(255),
    phone VARCHAR(64),
    bio VARCHAR(1000)
);

-- 7. Create attendance table if not exists
CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    faculty_id BIGINT REFERENCES faculty(id) ON DELETE SET NULL,
    date DATE,
    present BOOLEAN DEFAULT TRUE,
    attended_classes INTEGER,
    total_classes INTEGER,
    remarks VARCHAR(255)
);

-- 8. Create marks table if not exists
CREATE TABLE IF NOT EXISTS marks (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    faculty_id BIGINT REFERENCES faculty(id) ON DELETE SET NULL,
    exam_type VARCHAR(128) NOT NULL,
    marks_obtained DOUBLE PRECISION NOT NULL,
    max_marks DOUBLE PRECISION NOT NULL,
    grade VARCHAR(16),
    semester VARCHAR(64),
    published_date DATE
);
