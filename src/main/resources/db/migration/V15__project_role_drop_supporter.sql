-- ⚠️ Flyway is NOT on the classpath (see V7/V8/V12 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update never renames stored enum
-- values nor alters the CHECK constraint, so this transition is manual.
--
-- Why: the in-project role set is reduced to two values. The old `LEAD` role is renamed to
-- `PROJECT_MANAGER` (it has always been the PM's own membership — see ProjectService.createProject /
-- changeManager — and now matches UserRole.PROJECT_MANAGER), and `SUPPORTER` is dropped entirely.
-- Existing supporters are folded into plain `MEMBER`.

-- 1. Migrate existing rows to the new value set.
UPDATE project_members SET role_in_project = 'PROJECT_MANAGER' WHERE role_in_project = 'LEAD';
UPDATE project_members SET role_in_project = 'MEMBER'          WHERE role_in_project = 'SUPPORTER';

-- 2. Swap the CHECK constraint to the new value set.
ALTER TABLE project_members DROP CONSTRAINT IF EXISTS ck_pm_role;
ALTER TABLE project_members ADD CONSTRAINT ck_pm_role CHECK (role_in_project IN ('PROJECT_MANAGER','MEMBER'));
