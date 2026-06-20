-- ⚠️ Flyway is NOT on the classpath (see V7/V8/V12 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update only adds the new
-- `workload_percent` column from the entity; it never drops or renames the old ones.
--
-- Why: the workload model exposed two tightness ratios — `near_term_percent` (deadlines within a
-- 10-workday window) and `overall_percent` (all deadlines). The split was ambiguous: true
-- infeasibility already surfaces as a WILL_SLIP badge, so two load numbers added confusion without
-- adding signal. They are collapsed into a single `workload_percent` = cumulative demand / capacity
-- over ALL deadlines (the former `overall_percent`). The near-term window is removed entirely.

ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS near_term_percent;
ALTER TABLE workload_snapshots RENAME COLUMN overall_percent TO workload_percent;
