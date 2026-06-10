-- GateForge: persist apply-time prerequisites on migration plans
ALTER TABLE migration_plans ADD COLUMN IF NOT EXISTS prerequisites_json TEXT;
