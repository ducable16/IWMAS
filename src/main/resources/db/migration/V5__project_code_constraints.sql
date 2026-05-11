-- Normalize existing codes: uppercase, strip invalid chars
UPDATE projects
SET code = UPPER(REGEXP_REPLACE(TRIM(COALESCE(code, '')), '[^A-Z0-9-]', '', 'g'))
WHERE code IS NOT NULL;

-- Shorten column to 10 chars (Hibernate ddl-auto:update won't shrink it automatically)
ALTER TABLE projects ALTER COLUMN code TYPE varchar(10);

-- DB-level format check (second line of defense after Java validation)
ALTER TABLE projects
    ADD CONSTRAINT chk_project_code CHECK (code IS NULL OR code ~ '^[A-Z0-9-]{2,10}$');
