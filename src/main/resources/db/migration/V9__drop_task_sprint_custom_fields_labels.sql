-- ⚠️ Flyway is NOT on the classpath (see V7/V8 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update only adds schema; it
-- never drops removed columns/tables, so this cleanup is manual.
--
-- Why: the `sprint` field, the `customFields` map, and the `labels` set were removed from the Task
-- entity (and from all task DTOs, the controller search params, and the task specification). The
-- leftover column and the auxiliary collection tables are now dead weight and are dropped here.

DROP INDEX IF EXISTS idx_task_sprint;

ALTER TABLE tasks DROP COLUMN IF EXISTS sprint;

DROP TABLE IF EXISTS task_custom_fields;

DROP TABLE IF EXISTS task_labels;
