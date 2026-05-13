CREATE TABLE IF NOT EXISTS task_attachments (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT        NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    file_key        VARCHAR(500)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    uploaded_by     BIGINT        NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP     DEFAULT NOW(),
    created_by      BIGINT,
    last_modified_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_task_attachments_task_id ON task_attachments(task_id);

CREATE TABLE IF NOT EXISTS project_documents (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT        NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    file_key        VARCHAR(500)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    uploaded_by     BIGINT        NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP     DEFAULT NOW(),
    created_by      BIGINT,
    last_modified_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_project_documents_project_id ON project_documents(project_id);
