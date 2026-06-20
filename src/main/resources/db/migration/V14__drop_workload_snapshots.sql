-- ⚠️ Flyway is NOT on the classpath (see V7/V8/V12 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update only adds schema; it
-- never drops removed tables, so this cleanup is manual.
--
-- Why: the workload snapshot feature was removed. The daily snapshot scheduler, the snapshot
-- entity/repository/DTO and the GET /api/workload/team + POST /api/workload/snapshots endpoints are
-- gone. Member workload is now computed live on demand (the /realtime endpoints), and the daily
-- OVERLOAD_WARNING is sent by OverloadAlertScheduler without persisting anything. The
-- workload_snapshots table (created in V2, reworked in V7/V13) is therefore dead and dropped.

DROP TABLE IF EXISTS workload_snapshots;
