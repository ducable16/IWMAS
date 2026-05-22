-- Workload model v3 — serial scheduling simulation.
-- Documentation parity only: Flyway is not on the classpath, Hibernate
-- ddl-auto:update creates these columns from the entities at runtime.

-- Task: member-reported remaining effort + planned execution order.
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS reported_remaining_hours numeric(6, 1);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS remaining_reported_date date;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS execution_seq integer;

-- TimeLog: optional remaining-hours reported with the end-of-day entry.
ALTER TABLE time_logs ADD COLUMN IF NOT EXISTS remaining_hours numeric(6, 1);

-- workload_snapshots: schema reworked for the v3 model.
ALTER TABLE workload_snapshots ADD COLUMN IF NOT EXISTS predicted_late_task_count integer DEFAULT 0;
ALTER TABLE workload_snapshots ADD COLUMN IF NOT EXISTS near_term_percent numeric(7, 2);
ALTER TABLE workload_snapshots ADD COLUMN IF NOT EXISTS overall_percent numeric(7, 2);
-- Legacy v1/v2 columns no longer written (ddl-auto:update does not drop them):
--   total_allocated_hours, total_actual_hours, capacity_used_percent,
--   weekly_capacity_hours, weekly_load_hours, utilization_percent
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS total_allocated_hours;
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS total_actual_hours;
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS capacity_used_percent;
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS weekly_capacity_hours;
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS weekly_load_hours;
ALTER TABLE workload_snapshots DROP COLUMN IF EXISTS utilization_percent;
