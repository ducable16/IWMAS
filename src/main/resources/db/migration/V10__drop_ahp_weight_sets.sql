-- ⚠️ Flyway is NOT on the classpath (see V7/V8 note). These statements are NOT run automatically —
-- run them ONCE by hand against each real database. Hibernate ddl-auto:update only adds schema; it
-- never drops removed tables, so this cleanup is manual.
--
-- Why: the entire AHP / recommendation module (com.iwas.recommendation) was removed. The
-- `ahp_weight_sets` table it owned is now dead weight and is dropped here.

DROP TABLE IF EXISTS ahp_weight_sets;
