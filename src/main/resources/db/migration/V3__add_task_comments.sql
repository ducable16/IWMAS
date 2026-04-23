-- V3: Add task_comments table
CREATE TABLE task_comments
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT       NOT NULL,
    author_id  BIGINT       NOT NULL,
    content    TEXT         NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT,
    updated_at DATETIME(6),
    updated_by BIGINT,

    CONSTRAINT fk_tc_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_tc_author FOREIGN KEY (author_id) REFERENCES users (id),

    INDEX idx_tc_task (task_id),
    INDEX idx_tc_author (author_id)
);
