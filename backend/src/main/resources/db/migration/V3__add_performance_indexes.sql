-- Performance indexes for frequently queried columns

-- Defect queries: status, severity, project
CREATE INDEX IF NOT EXISTS idx_defects_status ON defects (status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_defects_severity ON defects (severity) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_defects_project_status ON defects (project_id, status) WHERE is_deleted = false;

-- Test runs: project, executed_at, pass_rate
CREATE INDEX IF NOT EXISTS idx_test_runs_project ON test_runs (project_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_test_runs_executed_at ON test_runs (executed_at DESC) WHERE is_deleted = false;

-- Users: email lookup (soft-delete aware)
CREATE INDEX IF NOT EXISTS idx_users_email_active ON users (email) WHERE is_deleted = false;

-- Integration configs: enabled flag
CREATE INDEX IF NOT EXISTS idx_integration_configs_enabled ON integration_configs (enabled) WHERE is_deleted = false;

-- QA services: active flag
CREATE INDEX IF NOT EXISTS idx_qa_services_active ON qa_services (is_active) WHERE is_deleted = false;

-- Environments: active flag
CREATE INDEX IF NOT EXISTS idx_environments_active ON environments (is_active) WHERE is_deleted = false;

-- Quality scores: project + calculated_at for dashboard
CREATE INDEX IF NOT EXISTS idx_quality_scores_project ON quality_scores (project_id, calculated_at DESC) WHERE is_deleted = false;

-- Test identities: flaky flag
CREATE INDEX IF NOT EXISTS idx_test_identities_flaky ON test_identities (is_flaky) WHERE is_flaky = true;
