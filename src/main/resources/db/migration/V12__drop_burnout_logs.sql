-- ⚠️ Flyway is NOT on the classpath (see V7/V8 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update only adds schema; it
-- never drops removed tables, so this cleanup is manual.
--
-- Why: the burnout-detection feature was never built — the `burnout_logs` table (created in V2) only
-- ever held hand-written seed data; no application code ever computed or wrote a row (no risk-score
-- logic, no evaluator, no scheduler). The entity, repository, DTO, REST endpoints, RiskLevel enum,
-- and the BURNOUT_ALERT notification type have all been removed. The table is dropped here so a
-- future burnout design can start clean without inheriting this dead schema.

DROP TABLE IF EXISTS burnout_logs;
