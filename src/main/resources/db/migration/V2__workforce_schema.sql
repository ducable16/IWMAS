-- ================================================================
-- V2: Workforce Management System Schema
-- Drops old Roamtrip tables and creates new workforce tables
-- ================================================================

-- Drop old tables (in dependency order)
DROP TABLE IF EXISTS ai_messages CASCADE;
DROP TABLE IF EXISTS ai_sessions CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS issues CASCADE;
DROP TABLE IF EXISTS workflow_transitions CASCADE;
DROP TABLE IF EXISTS workflow_statuses CASCADE;
DROP TABLE IF EXISTS workflows CASCADE;
DROP TABLE IF EXISTS sprints CASCADE;
DROP TABLE IF EXISTS project_members CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS organization_members CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
DROP TABLE IF EXISTS tenants CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;

-- ================================================================
-- Alter users table to match new schema
-- ================================================================

ALTER TABLE users
    DROP COLUMN IF EXISTS gender;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username varchar(100),
    ADD COLUMN IF NOT EXISTS phone varchar(20),
    ADD COLUMN IF NOT EXISTS avatar_url varchar(500),
    ADD COLUMN IF NOT EXISTS department_id bigint,
    ADD COLUMN IF NOT EXISTS position varchar(100),
    ADD COLUMN IF NOT EXISTS role varchar(50) NOT NULL DEFAULT 'TEAM_MEMBER',
    ADD COLUMN IF NOT EXISTS is_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS last_login_at timestamp;

-- Migrate existing users: set is_active = is_verified logic
-- Old is_active meant "email verified" so we use it for is_verified
UPDATE users SET is_verified = is_active WHERE is_verified = false;
-- Reset is_active to true (means employed, not email-verified)
UPDATE users SET is_active = true WHERE is_verified = true;

ALTER TABLE users
    ADD CONSTRAINT IF NOT EXISTS uq_user_username UNIQUE (username),
    ADD CONSTRAINT IF NOT EXISTS ck_user_role CHECK (role IN ('ADMIN','HR','PROJECT_MANAGER','TEAM_MEMBER'));

CREATE INDEX IF NOT EXISTS idx_user_department ON users(department_id);

-- ================================================================
-- Departments
-- ================================================================

CREATE TABLE IF NOT EXISTS departments (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    name varchar(100) NOT NULL,
    description text,
    manager_id bigint
);

CREATE INDEX IF NOT EXISTS idx_department_manager ON departments(manager_id);

-- ================================================================
-- Skills
-- ================================================================

CREATE TABLE IF NOT EXISTS skills (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    name varchar(100) NOT NULL,
    category varchar(100),
    description text
);

CREATE INDEX IF NOT EXISTS idx_skill_category ON skills(category);

CREATE TABLE IF NOT EXISTS employee_skills (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    user_id bigint NOT NULL REFERENCES users(id),
    skill_id bigint NOT NULL REFERENCES skills(id),
    level varchar(20) NOT NULL,
    years_of_experience decimal(4,1),
    note text,
    CONSTRAINT uq_employee_skill UNIQUE (user_id, skill_id),
    CONSTRAINT ck_skill_level CHECK (level IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT'))
);

CREATE INDEX IF NOT EXISTS idx_employee_skill_user ON employee_skills(user_id);
CREATE INDEX IF NOT EXISTS idx_employee_skill_skill ON employee_skills(skill_id);

-- ================================================================
-- Projects
-- ================================================================

CREATE TABLE IF NOT EXISTS projects (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    name varchar(200) NOT NULL,
    code varchar(50),
    description text,
    status varchar(50) NOT NULL DEFAULT 'PLANNING',
    priority varchar(20) DEFAULT 'MEDIUM',
    start_date date,
    end_date date,
    actual_end_date date,
    manager_id bigint NOT NULL REFERENCES users(id),
    CONSTRAINT uq_project_code UNIQUE (code),
    CONSTRAINT ck_project_status CHECK (status IN ('PLANNING','IN_PROGRESS','ON_HOLD','COMPLETED','CANCELLED')),
    CONSTRAINT ck_project_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_project_manager ON projects(manager_id);
CREATE INDEX IF NOT EXISTS idx_project_status ON projects(status);

CREATE TABLE IF NOT EXISTS project_members (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint NOT NULL REFERENCES projects(id),
    user_id bigint NOT NULL REFERENCES users(id),
    role_in_project varchar(50) NOT NULL DEFAULT 'MEMBER',
    allocated_effort_percent int,
    join_date date,
    leave_date date,
    note text,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id),
    CONSTRAINT ck_pm_role CHECK (role_in_project IN ('LEAD','MEMBER','SUPPORTER'))
);

CREATE INDEX IF NOT EXISTS idx_pm_project ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_pm_user ON project_members(user_id);

-- ================================================================
-- Tasks
-- ================================================================

CREATE TABLE IF NOT EXISTS tasks (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint NOT NULL REFERENCES projects(id),
    title varchar(300) NOT NULL,
    description text,
    type varchar(50) NOT NULL DEFAULT 'FEATURE',
    status varchar(50) NOT NULL DEFAULT 'TODO',
    priority varchar(20) NOT NULL DEFAULT 'MEDIUM',
    estimated_hours decimal(6,1),
    actual_hours decimal(6,1),
    start_date date,
    due_date date,
    completed_at timestamp,
    assignee_id bigint REFERENCES users(id),
    reporter_id bigint NOT NULL REFERENCES users(id),
    CONSTRAINT ck_task_type CHECK (type IN ('FEATURE','BUG','RESEARCH','TESTING','DOCUMENTATION')),
    CONSTRAINT ck_task_status CHECK (status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE','CANCELLED')),
    CONSTRAINT ck_task_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_task_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON tasks(assignee_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON tasks(due_date);

CREATE TABLE IF NOT EXISTS task_skill_requirements (
    id bigserial PRIMARY KEY,
    task_id bigint NOT NULL REFERENCES tasks(id),
    skill_id bigint NOT NULL REFERENCES skills(id),
    minimum_level varchar(20) NOT NULL DEFAULT 'INTERMEDIATE',
    is_required boolean DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    CONSTRAINT uq_task_skill UNIQUE (task_id, skill_id),
    CONSTRAINT ck_tsr_level CHECK (minimum_level IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT'))
);

CREATE INDEX IF NOT EXISTS idx_tsr_task ON task_skill_requirements(task_id);
CREATE INDEX IF NOT EXISTS idx_tsr_skill ON task_skill_requirements(skill_id);

CREATE TABLE IF NOT EXISTS task_status_history (
    id bigserial PRIMARY KEY,
    task_id bigint NOT NULL REFERENCES tasks(id),
    old_status varchar(50),
    new_status varchar(50) NOT NULL,
    changed_by bigint NOT NULL REFERENCES users(id),
    note text,
    changed_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tsh_task ON task_status_history(task_id);
CREATE INDEX IF NOT EXISTS idx_tsh_changed_at ON task_status_history(changed_at);

-- ================================================================
-- Time Logs
-- ================================================================

CREATE TABLE IF NOT EXISTS time_logs (
    id bigserial PRIMARY KEY,
    is_active boolean NOT NULL DEFAULT true,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    task_id bigint NOT NULL REFERENCES tasks(id),
    user_id bigint NOT NULL REFERENCES users(id),
    log_date date NOT NULL,
    hours_spent decimal(4,1) NOT NULL,
    description text,
    CONSTRAINT uq_time_log UNIQUE (task_id, user_id, log_date)
);

CREATE INDEX IF NOT EXISTS idx_timelog_task ON time_logs(task_id);
CREATE INDEX IF NOT EXISTS idx_timelog_user ON time_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_timelog_date ON time_logs(log_date);

-- ================================================================
-- Notifications (new schema)
-- ================================================================

CREATE TABLE IF NOT EXISTS notifications (
    id bigserial PRIMARY KEY,
    recipient_id bigint NOT NULL REFERENCES users(id),
    type varchar(100) NOT NULL,
    title varchar(300) NOT NULL,
    content text,
    reference_type varchar(50),
    reference_id bigint,
    is_read boolean NOT NULL DEFAULT false,
    read_at timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_type CHECK (type IN (
        'TASK_ASSIGNED','TASK_OVERDUE','TASK_STATUS_CHANGED',
        'OVERLOAD_WARNING','BURNOUT_ALERT','PROJECT_ADDED','DEADLINE_REMINDER'
    ))
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notifications(recipient_id, is_read);

-- ================================================================
-- Workload Snapshots
-- ================================================================

CREATE TABLE IF NOT EXISTS workload_snapshots (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id),
    snapshot_date date NOT NULL,
    total_allocated_hours decimal(6,1),
    total_actual_hours decimal(6,1),
    capacity_used_percent decimal(5,2),
    project_count int DEFAULT 0,
    active_task_count int DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_workload_snapshot UNIQUE (user_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_ws_user ON workload_snapshots(user_id);
CREATE INDEX IF NOT EXISTS idx_ws_date ON workload_snapshots(snapshot_date);

-- ================================================================
-- Burnout Logs
-- ================================================================

CREATE TABLE IF NOT EXISTS burnout_logs (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id),
    evaluated_at timestamp NOT NULL DEFAULT now(),
    risk_score int NOT NULL,
    risk_level varchar(20) NOT NULL,
    overdue_task_count int DEFAULT 0,
    capacity_used_avg decimal(5,2),
    is_alert_sent boolean DEFAULT false,
    note text,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT ck_burnout_risk_level CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_burnout_user ON burnout_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_burnout_evaluated_at ON burnout_logs(evaluated_at);
