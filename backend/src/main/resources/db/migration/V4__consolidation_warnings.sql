-- GateForge: persist consolidation warnings on migration plans
ALTER TABLE migration_plans ADD COLUMN IF NOT EXISTS consolidation_warnings_json TEXT;
