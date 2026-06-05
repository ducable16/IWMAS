-- ⚠️ Flyway is NOT on the classpath (see V7 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update can neither drop the
-- old constraint nor create a partial index, so this step is manual.
--
-- Why: project code must be unique only among *active* (non-soft-deleted) projects. The original
-- global UNIQUE(code) constraint (created by Hibernate from the Project entity, mirrored in V2)
-- ignored is_deleted, so a soft-deleted project kept its code reserved forever: the app-level
-- checks all filter is_deleted = false, saw the code as free, and let creation proceed — only for
-- the DB to reject the INSERT with a duplicate key. The entity no longer declares a global unique
-- constraint (so ddl-auto won't recreate it); this partial unique index enforces the correct
-- "unique among active projects" rule and lets a deleted project's code be reused.

ALTER TABLE projects DROP CONSTRAINT uq_project_code;

CREATE UNIQUE INDEX uq_project_code_active
    ON projects (code)
    WHERE is_deleted = false;
