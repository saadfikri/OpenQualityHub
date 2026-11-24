-- ============================================================
-- Open Quality Hub — Complete Database Schema
-- Enterprise Quality Engineering & Test Management Platform
-- ============================================================

-- Auto-update updated_at on row modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- ORGANIZATIONS (tenant root)
-- ============================================================
CREATE TABLE organizations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    description   TEXT,
    logo_url      VARCHAR(500),
    plan          VARCHAR(50) NOT NULL DEFAULT 'FREE',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    version       BIGINT NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_organizations_slug UNIQUE (slug),
    CONSTRAINT chk_organizations_plan CHECK (plan IN ('FREE', 'TEAM', 'ENTERPRISE'))
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_is_active ON organizations(is_active) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Default organization for bootstrapping
INSERT INTO organizations (id, name, slug, description, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', 'Default organization', 'FREE');

-- ============================================================
-- TEAMS
-- ============================================================
CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_teams_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE UNIQUE INDEX uq_teams_name ON teams (name) WHERE NOT is_deleted;
CREATE INDEX idx_teams_is_deleted ON teams(is_deleted);
CREATE INDEX idx_teams_name_lower ON teams(lower(name));
CREATE INDEX idx_teams_organization_id ON teams(organization_id);

CREATE TRIGGER trg_teams_updated_at
    BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    display_name    VARCHAR(200),
    avatar_url      VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_roles_name_org UNIQUE (name, organization_id)
);

CREATE INDEX idx_roles_organization_id ON roles(organization_id);

CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- PERMISSIONS
-- ============================================================
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    module      VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- ============================================================
-- ROLE_PERMISSIONS (join table)
-- ============================================================
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- USER_ROLES (join table)
-- ============================================================
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- PROJECTS
-- ============================================================
CREATE TABLE projects (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    team_id       UUID NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version       BIGINT NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_projects_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uq_projects_name_team ON projects (name, team_id) WHERE NOT is_deleted;
CREATE INDEX idx_projects_team_id ON projects(team_id);
CREATE INDEX idx_projects_status ON projects(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_projects_is_deleted ON projects(is_deleted);

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- APPLICATIONS
-- ============================================================
CREATE TABLE applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    repository_url  VARCHAR(500),
    technology_stack VARCHAR(500),
    status          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_applications_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_applications_name_project UNIQUE (name, project_id),
    CONSTRAINT chk_applications_status CHECK (status IN ('ACTIVE', 'DEPRECATED', 'ARCHIVED'))
);

CREATE INDEX idx_applications_project_id ON applications(project_id);

CREATE TRIGGER trg_applications_updated_at
    BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ENVIRONMENTS
-- ============================================================
CREATE TABLE environments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    env_type        VARCHAR(50) NOT NULL,
    url             VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_environments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_environments_name_org UNIQUE (name, organization_id),
    CONSTRAINT chk_environments_type CHECK (env_type IN ('DEVELOPMENT', 'INTEGRATION', 'STAGING', 'PRE_PRODUCTION', 'PRODUCTION'))
);

CREATE INDEX idx_environments_organization_id ON environments(organization_id);

CREATE TRIGGER trg_environments_updated_at
    BEFORE UPDATE ON environments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- RELEASES
-- ============================================================
CREATE TABLE releases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL,
    version         VARCHAR(100) NOT NULL,
    name            VARCHAR(255),
    description     TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    target_date     DATE,
    actual_date     DATE,
    release_notes   TEXT,
    db_version      BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_releases_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_releases_version_project UNIQUE (version, project_id),
    CONSTRAINT chk_releases_status CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'READY_FOR_QA', 'QA_IN_PROGRESS', 'APPROVED', 'RELEASED', 'CANCELLED'))
);

CREATE INDEX idx_releases_project_id ON releases(project_id);
CREATE INDEX idx_releases_status ON releases(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_releases_target_date ON releases(target_date) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_releases_updated_at
    BEFORE UPDATE ON releases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- BUILDS
-- ============================================================
CREATE TABLE builds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL,
    release_id      UUID,
    build_number    VARCHAR(100) NOT NULL,
    commit_sha      VARCHAR(40),
    branch          VARCHAR(255),
    status          VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    build_url       VARCHAR(500),
    started_at      TIMESTAMP WITH TIME ZONE,
    finished_at     TIMESTAMP WITH TIME ZONE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_builds_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_builds_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE SET NULL,
    CONSTRAINT chk_builds_status CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_builds_project_id ON builds(project_id);
CREATE INDEX idx_builds_release_id ON builds(release_id);
CREATE INDEX idx_builds_commit_sha ON builds(commit_sha) WHERE commit_sha IS NOT NULL;

CREATE TRIGGER trg_builds_updated_at
    BEFORE UPDATE ON builds
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TEST RUNS
-- ============================================================
CREATE TABLE test_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL,
    build_id        UUID,
    release_id      UUID,
    name            VARCHAR(255) NOT NULL,
    source          VARCHAR(50) NOT NULL,
    environment     VARCHAR(100),
    commit_sha      VARCHAR(40),
    branch          VARCHAR(255),
    total_tests     INTEGER NOT NULL DEFAULT 0,
    passed          INTEGER NOT NULL DEFAULT 0,
    failed          INTEGER NOT NULL DEFAULT 0,
    skipped         INTEGER NOT NULL DEFAULT 0,
    duration_ms     BIGINT,
    pass_rate       NUMERIC(5,2) GENERATED ALWAYS AS (
                        CASE WHEN total_tests > 0
                             THEN ROUND((passed::NUMERIC / total_tests) * 100, 2)
                             ELSE 0
                        END
                    ) STORED,
    executed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_test_runs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_test_runs_build FOREIGN KEY (build_id) REFERENCES builds(id) ON DELETE SET NULL,
    CONSTRAINT fk_test_runs_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE SET NULL,
    CONSTRAINT chk_test_runs_source CHECK (source IN ('PLAYWRIGHT', 'JUNIT')),
    CONSTRAINT chk_test_runs_counts CHECK (
        total_tests >= 0 AND passed >= 0 AND failed >= 0 AND skipped >= 0
    ),
    CONSTRAINT chk_test_runs_total CHECK (
        total_tests = passed + failed + skipped
    )
);

CREATE INDEX idx_test_runs_project_id ON test_runs(project_id);
CREATE INDEX idx_test_runs_executed_at ON test_runs(executed_at DESC);
CREATE INDEX idx_test_runs_source ON test_runs(source);
CREATE INDEX idx_test_runs_is_deleted ON test_runs(is_deleted);
CREATE INDEX idx_test_runs_project_executed ON test_runs(project_id, executed_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_test_runs_build_id ON test_runs(build_id) WHERE build_id IS NOT NULL;
CREATE INDEX idx_test_runs_release_id ON test_runs(release_id) WHERE release_id IS NOT NULL;

CREATE TRIGGER trg_test_runs_updated_at
    BEFORE UPDATE ON test_runs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TEST CASES
-- ============================================================
CREATE TABLE test_cases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_run_id     UUID NOT NULL,
    name            VARCHAR(500) NOT NULL,
    class_name      VARCHAR(500),
    suite_name      VARCHAR(500),
    status          VARCHAR(50) NOT NULL,
    duration_ms     BIGINT,
    error_message   TEXT,
    stack_trace     TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_test_cases_test_run FOREIGN KEY (test_run_id) REFERENCES test_runs(id) ON DELETE CASCADE,
    CONSTRAINT chk_test_cases_status CHECK (status IN ('PASSED', 'FAILED', 'SKIPPED', 'ERROR')),
    CONSTRAINT chk_test_cases_duration CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE INDEX idx_test_cases_test_run_id ON test_cases(test_run_id);
CREATE INDEX idx_test_cases_status ON test_cases(status);
CREATE INDEX idx_test_cases_run_status ON test_cases(test_run_id, status);

CREATE TRIGGER trg_test_cases_updated_at
    BEFORE UPDATE ON test_cases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TEST IDENTITIES (stable fingerprint for flaky detection)
-- ============================================================
CREATE TABLE test_identities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id),
    fingerprint         VARCHAR(255) NOT NULL,
    class_name          VARCHAR(500),
    test_name           VARCHAR(500) NOT NULL,
    suite_name          VARCHAR(500),
    first_seen_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_executions    INTEGER NOT NULL DEFAULT 0,
    total_passed        INTEGER NOT NULL DEFAULT 0,
    total_failed        INTEGER NOT NULL DEFAULT 0,
    total_skipped       INTEGER NOT NULL DEFAULT 0,
    flaky_score         NUMERIC(5,2) DEFAULT 0.00,
    is_flaky            BOOLEAN NOT NULL DEFAULT FALSE,
    failure_classification VARCHAR(50) DEFAULT 'UNKNOWN',
    last_status         VARCHAR(20),
    avg_duration_ms     BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_test_identity_fingerprint UNIQUE (project_id, fingerprint)
);

CREATE INDEX idx_test_identities_project ON test_identities(project_id);
CREATE INDEX idx_test_identities_flaky ON test_identities(project_id, is_flaky) WHERE is_flaky = TRUE;
CREATE INDEX idx_test_identities_score ON test_identities(project_id, flaky_score DESC);

CREATE TRIGGER set_test_identities_updated_at
    BEFORE UPDATE ON test_identities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TEST EXECUTIONS (links test_case to test_identity for history)
-- ============================================================
CREATE TABLE test_executions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_identity_id    UUID NOT NULL REFERENCES test_identities(id),
    test_run_id         UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    test_case_id        UUID REFERENCES test_cases(id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL,
    duration_ms         BIGINT,
    error_message       TEXT,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    environment         VARCHAR(255),
    branch              VARCHAR(255),
    commit_sha          VARCHAR(40),
    executed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_test_executions_identity ON test_executions(test_identity_id);
CREATE INDEX idx_test_executions_run ON test_executions(test_run_id);
CREATE INDEX idx_test_executions_identity_time ON test_executions(test_identity_id, executed_at DESC);

-- ============================================================
-- DEFECTS
-- ============================================================
CREATE TABLE defects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL,
    release_id      UUID,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    severity        VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority        INTEGER NOT NULL DEFAULT 3,
    component       VARCHAR(255),
    assigned_to     UUID,
    reported_by     UUID,
    resolution      TEXT,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    external_id     VARCHAR(255),
    external_url    VARCHAR(500),
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_defects_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_defects_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE SET NULL,
    CONSTRAINT fk_defects_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_defects_reported_by FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_defects_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_defects_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED')),
    CONSTRAINT chk_defects_priority CHECK (priority BETWEEN 1 AND 5)
);

CREATE INDEX idx_defects_project_id ON defects(project_id);
CREATE INDEX idx_defects_release_id ON defects(release_id);
CREATE INDEX idx_defects_severity ON defects(severity) WHERE is_deleted = FALSE;
CREATE INDEX idx_defects_status ON defects(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_defects_assigned_to ON defects(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_defects_project_status ON defects(project_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_defects_assigned ON defects(assigned_to) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_defects_updated_at
    BEFORE UPDATE ON defects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- QUALITY GATES
-- ============================================================
CREATE TABLE quality_gates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_quality_gates_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_quality_gates_name_org UNIQUE (name, organization_id)
);

CREATE INDEX idx_quality_gates_organization_id ON quality_gates(organization_id);

CREATE TRIGGER trg_quality_gates_updated_at
    BEFORE UPDATE ON quality_gates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- QUALITY GATE CONDITIONS
-- ============================================================
CREATE TABLE quality_gate_conditions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quality_gate_id   UUID NOT NULL,
    metric_key        VARCHAR(100) NOT NULL,
    operator          VARCHAR(10) NOT NULL,
    threshold_value   NUMERIC(10,2) NOT NULL,
    is_critical       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_qgc_quality_gate FOREIGN KEY (quality_gate_id) REFERENCES quality_gates(id) ON DELETE CASCADE,
    CONSTRAINT chk_qgc_operator CHECK (operator IN ('LT', 'GT', 'LTE', 'GTE', 'EQ'))
);

CREATE INDEX idx_qgc_quality_gate_id ON quality_gate_conditions(quality_gate_id);

CREATE TRIGGER trg_qgc_updated_at
    BEFORE UPDATE ON quality_gate_conditions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- QUALITY GATE EVALUATIONS
-- ============================================================
CREATE TABLE quality_gate_evaluations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quality_gate_id     UUID NOT NULL,
    project_id          UUID NOT NULL,
    release_id          UUID,
    status              VARCHAR(50) NOT NULL,
    evaluated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    evaluated_by        UUID,
    notes               TEXT,
    evidence            JSONB DEFAULT '{}',
    override_reason     TEXT,
    override_by         UUID,
    override_at         TIMESTAMP WITH TIME ZONE,
    override_expires_at TIMESTAMP WITH TIME ZONE,
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_qge_quality_gate FOREIGN KEY (quality_gate_id) REFERENCES quality_gates(id),
    CONSTRAINT fk_qge_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_qge_release FOREIGN KEY (release_id) REFERENCES releases(id),
    CONSTRAINT fk_qge_evaluated_by FOREIGN KEY (evaluated_by) REFERENCES users(id),
    CONSTRAINT fk_qge_override_by FOREIGN KEY (override_by) REFERENCES users(id),
    CONSTRAINT chk_qge_status CHECK (status IN ('PASSED', 'FAILED', 'WARNING', 'INSUFFICIENT_DATA'))
);

CREATE INDEX idx_qge_quality_gate_id ON quality_gate_evaluations(quality_gate_id);
CREATE INDEX idx_qge_project_id ON quality_gate_evaluations(project_id);
CREATE INDEX idx_qge_release_id ON quality_gate_evaluations(release_id);

CREATE TRIGGER trg_qge_updated_at
    BEFORE UPDATE ON quality_gate_evaluations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- QUALITY SCORES
-- ============================================================
CREATE TABLE quality_scores (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id              UUID NOT NULL,
    release_id              UUID,
    overall_score           NUMERIC(5,2),
    test_reliability_score  NUMERIC(5,2),
    defect_score            NUMERIC(5,2),
    risk_coverage_score     NUMERIC(5,2),
    automation_score        NUMERIC(5,2),
    gate_compliance_score   NUMERIC(5,2),
    calculated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    weights                 JSONB NOT NULL DEFAULT '{"testReliability":0.30,"defect":0.25,"riskCoverage":0.20,"automation":0.15,"gateCompliance":0.10}',
    details                 JSONB DEFAULT '{}',
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_quality_scores_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_quality_scores_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE SET NULL,
    CONSTRAINT chk_quality_scores_overall CHECK (overall_score IS NULL OR (overall_score >= 0 AND overall_score <= 100)),
    CONSTRAINT chk_quality_scores_reliability CHECK (test_reliability_score IS NULL OR (test_reliability_score >= 0 AND test_reliability_score <= 100)),
    CONSTRAINT chk_quality_scores_defect CHECK (defect_score IS NULL OR (defect_score >= 0 AND defect_score <= 100)),
    CONSTRAINT chk_quality_scores_risk CHECK (risk_coverage_score IS NULL OR (risk_coverage_score >= 0 AND risk_coverage_score <= 100)),
    CONSTRAINT chk_quality_scores_automation CHECK (automation_score IS NULL OR (automation_score >= 0 AND automation_score <= 100)),
    CONSTRAINT chk_quality_scores_gate CHECK (gate_compliance_score IS NULL OR (gate_compliance_score >= 0 AND gate_compliance_score <= 100))
);

CREATE INDEX idx_quality_scores_project_id ON quality_scores(project_id);
CREATE INDEX idx_quality_scores_release_id ON quality_scores(release_id);
CREATE INDEX idx_quality_scores_calculated_at ON quality_scores(calculated_at DESC);

CREATE TRIGGER trg_quality_scores_updated_at
    BEFORE UPDATE ON quality_scores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- AUDIT EVENTS (append-only)
-- ============================================================
CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    action          VARCHAR(50) NOT NULL,
    details         JSONB DEFAULT '{}',
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_audit_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_events_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'IMPORT', 'EVALUATE', 'OVERRIDE', 'EXPORT'))
);

CREATE INDEX idx_audit_events_organization_id ON audit_events(organization_id);
CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events(action);

-- ============================================================
-- QA SERVICE CATALOG
-- ============================================================
CREATE TABLE qa_services (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(50) NOT NULL,
    estimated_hours NUMERIC(6,1),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_qa_services_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_qa_services_name_org UNIQUE (name, organization_id),
    CONSTRAINT chk_qa_services_category CHECK (category IN (
        'TEST_PLANNING', 'TEST_EXECUTION', 'TEST_AUTOMATION',
        'PERFORMANCE_TESTING', 'SECURITY_TESTING', 'ACCESSIBILITY_TESTING',
        'REGRESSION_TESTING', 'EXPLORATORY_TESTING', 'TEST_ENVIRONMENT_SETUP',
        'CONSULTATION'
    ))
);

CREATE INDEX idx_qa_services_organization ON qa_services(organization_id);
CREATE INDEX idx_qa_services_category ON qa_services(category) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_qa_services_updated_at
    BEFORE UPDATE ON qa_services
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- SLA CONFIGURATIONS
-- ============================================================
CREATE TABLE sla_configurations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    priority_level          INTEGER NOT NULL,
    response_time_hours     INTEGER NOT NULL,
    resolution_time_hours   INTEGER NOT NULL,
    business_hours_only     BOOLEAN NOT NULL DEFAULT TRUE,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_sla_configurations_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_sla_configurations_priority_org UNIQUE (priority_level, organization_id),
    CONSTRAINT chk_sla_priority CHECK (priority_level BETWEEN 1 AND 5),
    CONSTRAINT chk_sla_response_time CHECK (response_time_hours > 0),
    CONSTRAINT chk_sla_resolution_time CHECK (resolution_time_hours > 0)
);

CREATE INDEX idx_sla_configurations_organization ON sla_configurations(organization_id);

CREATE TRIGGER trg_sla_configurations_updated_at
    BEFORE UPDATE ON sla_configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- QA SERVICE REQUESTS (8-state workflow)
-- ============================================================
CREATE TABLE qa_service_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL,
    request_number      VARCHAR(20) NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    qa_service_id       UUID NOT NULL,
    requesting_team_id  UUID NOT NULL,
    requesting_user_id  UUID NOT NULL,
    project_id          UUID,
    assigned_team_id    UUID,
    assigned_user_id    UUID,
    status              VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    impact              VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    urgency             VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    priority_score      INTEGER NOT NULL DEFAULT 3,
    priority_override   INTEGER,
    override_reason     TEXT,
    sla_configuration_id UUID,
    response_deadline   TIMESTAMP WITH TIME ZONE,
    resolution_deadline TIMESTAMP WITH TIME ZONE,
    responded_at        TIMESTAMP WITH TIME ZONE,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    estimated_hours     NUMERIC(6,1),
    actual_hours        NUMERIC(6,1),
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_qa_requests_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_qa_requests_service FOREIGN KEY (qa_service_id) REFERENCES qa_services(id),
    CONSTRAINT fk_qa_requests_requesting_team FOREIGN KEY (requesting_team_id) REFERENCES teams(id),
    CONSTRAINT fk_qa_requests_requesting_user FOREIGN KEY (requesting_user_id) REFERENCES users(id),
    CONSTRAINT fk_qa_requests_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_qa_requests_assigned_team FOREIGN KEY (assigned_team_id) REFERENCES teams(id),
    CONSTRAINT fk_qa_requests_assigned_user FOREIGN KEY (assigned_user_id) REFERENCES users(id),
    CONSTRAINT fk_qa_requests_sla FOREIGN KEY (sla_configuration_id) REFERENCES sla_configurations(id),
    CONSTRAINT uq_qa_requests_number_org UNIQUE (request_number, organization_id),
    CONSTRAINT chk_qa_requests_status CHECK (status IN (
        'SUBMITTED', 'TRIAGED', 'ASSIGNED', 'IN_PROGRESS',
        'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'
    )),
    CONSTRAINT chk_qa_requests_impact CHECK (impact IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_qa_requests_urgency CHECK (urgency IN ('IMMEDIATE', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_qa_requests_priority CHECK (priority_score BETWEEN 1 AND 5),
    CONSTRAINT chk_qa_requests_priority_override CHECK (priority_override IS NULL OR priority_override BETWEEN 1 AND 5)
);

CREATE INDEX idx_qa_requests_organization ON qa_service_requests(organization_id);
CREATE INDEX idx_qa_requests_status ON qa_service_requests(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_qa_requests_assigned_user ON qa_service_requests(assigned_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_qa_requests_assigned_team ON qa_service_requests(assigned_team_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_qa_requests_requesting_team ON qa_service_requests(requesting_team_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_qa_requests_service ON qa_service_requests(qa_service_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_qa_requests_sla ON qa_service_requests(sla_configuration_id) WHERE sla_configuration_id IS NOT NULL;
CREATE INDEX idx_qa_requests_priority ON qa_service_requests(priority_score) WHERE is_deleted = FALSE AND status NOT IN ('CLOSED', 'CANCELLED');
CREATE INDEX idx_qa_requests_deadlines ON qa_service_requests(resolution_deadline) WHERE is_deleted = FALSE AND status NOT IN ('CLOSED', 'CANCELLED', 'COMPLETED');

CREATE TRIGGER trg_qa_requests_updated_at
    BEFORE UPDATE ON qa_service_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE SEQUENCE qa_request_number_seq START WITH 1 INCREMENT BY 1;

-- ============================================================
-- QA REQUEST ACTIVITIES (timeline / comments)
-- ============================================================
CREATE TABLE qa_request_activities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID NOT NULL,
    user_id         UUID,
    activity_type   VARCHAR(50) NOT NULL,
    old_value       VARCHAR(255),
    new_value       VARCHAR(255),
    comment         TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_qa_request_activities_request FOREIGN KEY (request_id) REFERENCES qa_service_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_qa_request_activities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_qa_request_activity_type CHECK (activity_type IN (
        'STATUS_CHANGE', 'ASSIGNMENT', 'PRIORITY_CHANGE', 'PRIORITY_OVERRIDE',
        'COMMENT', 'SLA_BREACH', 'SLA_WARNING', 'HOURS_LOGGED'
    ))
);

CREATE INDEX idx_qa_request_activities_request ON qa_request_activities(request_id);
CREATE INDEX idx_qa_request_activities_created ON qa_request_activities(request_id, created_at DESC);

-- ============================================================
-- SLA EVALUATIONS
-- ============================================================
CREATE TABLE sla_evaluations (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id                UUID NOT NULL,
    sla_configuration_id      UUID NOT NULL,
    response_met              BOOLEAN,
    resolution_met            BOOLEAN,
    response_breach_minutes   BIGINT,
    resolution_breach_minutes BIGINT,
    evaluated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_sla_evaluations_request FOREIGN KEY (request_id) REFERENCES qa_service_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_sla_evaluations_sla FOREIGN KEY (sla_configuration_id) REFERENCES sla_configurations(id)
);

CREATE INDEX idx_sla_evaluations_request ON sla_evaluations(request_id);
CREATE INDEX idx_sla_evaluations_sla ON sla_evaluations(sla_configuration_id);

-- ============================================================
-- CAPACITY ALLOCATIONS
-- ============================================================
CREATE TABLE capacity_allocations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL,
    user_id         UUID,
    week_start      DATE NOT NULL,
    total_hours     NUMERIC(5,1) NOT NULL DEFAULT 40.0,
    allocated_hours NUMERIC(5,1) NOT NULL DEFAULT 0.0,
    available_hours NUMERIC(5,1) GENERATED ALWAYS AS (total_hours - allocated_hours) STORED,
    notes           TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_capacity_allocations_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_capacity_allocations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_capacity_total_hours CHECK (total_hours >= 0),
    CONSTRAINT chk_capacity_allocated_hours CHECK (allocated_hours >= 0 AND allocated_hours <= total_hours)
);

CREATE UNIQUE INDEX uq_capacity_allocation_user_week ON capacity_allocations(user_id, week_start) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_capacity_allocation_team_week ON capacity_allocations(team_id, week_start) WHERE user_id IS NULL;
CREATE INDEX idx_capacity_allocations_team ON capacity_allocations(team_id);
CREATE INDEX idx_capacity_allocations_user ON capacity_allocations(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_capacity_allocations_week ON capacity_allocations(week_start);

CREATE TRIGGER trg_capacity_allocations_updated_at
    BEFORE UPDATE ON capacity_allocations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- DOCUMENTATION ASSETS
-- ============================================================
CREATE TABLE documentation_assets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL,
    project_id          UUID,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    doc_type            VARCHAR(50) NOT NULL,
    url                 VARCHAR(2000),
    repo_path           VARCHAR(1000),
    category            VARCHAR(50) NOT NULL,
    owner_user_id       UUID,
    health_status       VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_checked_at     TIMESTAMP WITH TIME ZONE,
    last_reviewed_at    TIMESTAMP WITH TIME ZONE,
    review_cycle_days   INTEGER NOT NULL DEFAULT 90,
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_doc_assets_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_doc_assets_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_doc_assets_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_doc_assets_type CHECK (doc_type IN (
        'API_SPEC', 'USER_GUIDE', 'RUNBOOK', 'ARCHITECTURE', 'TEST_PLAN',
        'RELEASE_NOTES', 'ONBOARDING', 'POLICY', 'WIKI', 'OTHER'
    )),
    CONSTRAINT chk_doc_assets_category CHECK (category IN (
        'TECHNICAL', 'PROCESS', 'USER_FACING', 'COMPLIANCE', 'INTERNAL'
    )),
    CONSTRAINT chk_doc_assets_health CHECK (health_status IN (
        'HEALTHY', 'WARNING', 'CRITICAL', 'UNKNOWN'
    )),
    CONSTRAINT chk_doc_assets_source CHECK (url IS NOT NULL OR repo_path IS NOT NULL)
);

CREATE INDEX idx_doc_assets_organization ON documentation_assets(organization_id);
CREATE INDEX idx_doc_assets_project ON documentation_assets(project_id) WHERE project_id IS NOT NULL;
CREATE INDEX idx_doc_assets_health ON documentation_assets(health_status) WHERE is_deleted = FALSE;
CREATE INDEX idx_doc_assets_owner ON documentation_assets(owner_user_id) WHERE owner_user_id IS NOT NULL;
CREATE INDEX idx_doc_assets_type ON documentation_assets(doc_type) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_doc_assets_updated_at
    BEFORE UPDATE ON documentation_assets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- DOCUMENTATION CHECKS
-- ============================================================
CREATE TABLE documentation_checks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID NOT NULL,
    check_type      VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    message         TEXT,
    details         JSONB DEFAULT '{}',
    executed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_doc_checks_asset FOREIGN KEY (asset_id) REFERENCES documentation_assets(id) ON DELETE CASCADE,
    CONSTRAINT chk_doc_checks_type CHECK (check_type IN (
        'BROKEN_LINK', 'MISSING_OWNER', 'STALE_REVIEW', 'API_SPEC_VALIDATION', 'CONTENT_FRESHNESS'
    )),
    CONSTRAINT chk_doc_checks_status CHECK (status IN ('PASS', 'WARNING', 'FAIL'))
);

CREATE INDEX idx_doc_checks_asset ON documentation_checks(asset_id);
CREATE INDEX idx_doc_checks_executed ON documentation_checks(asset_id, executed_at DESC);

-- ============================================================
-- DOCUMENTATION REVIEWS
-- ============================================================
CREATE TABLE documentation_reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID NOT NULL,
    reviewer_id     UUID,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date        DATE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    comment         TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_doc_reviews_asset FOREIGN KEY (asset_id) REFERENCES documentation_assets(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_doc_reviews_status CHECK (status IN ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_doc_reviews_asset ON documentation_reviews(asset_id);
CREATE INDEX idx_doc_reviews_reviewer ON documentation_reviews(reviewer_id) WHERE reviewer_id IS NOT NULL;
CREATE INDEX idx_doc_reviews_status ON documentation_reviews(status) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_doc_reviews_updated_at
    BEFORE UPDATE ON documentation_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- RELEASE READINESS REPORTS
-- ============================================================
CREATE TABLE release_readiness_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    release_id      UUID NOT NULL,
    overall_score   NUMERIC(5,2) NOT NULL,
    gate_status     VARCHAR(20) NOT NULL,
    summary         TEXT,
    metrics         JSONB NOT NULL DEFAULT '{}',
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    generated_by    UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_rrr_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE,
    CONSTRAINT fk_rrr_generated_by FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_rrr_gate_status CHECK (gate_status IN ('PASS', 'WARN', 'FAIL')),
    CONSTRAINT chk_rrr_score CHECK (overall_score >= 0 AND overall_score <= 100)
);

CREATE INDEX idx_rrr_release_id ON release_readiness_reports(release_id);
CREATE INDEX idx_rrr_generated_at ON release_readiness_reports(release_id, generated_at DESC);

CREATE TRIGGER trg_rrr_updated_at
    BEFORE UPDATE ON release_readiness_reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- RELEASE GATE RESULTS
-- ============================================================
CREATE TABLE release_gate_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id       UUID NOT NULL,
    condition_id    UUID NOT NULL,
    metric_key      VARCHAR(100) NOT NULL,
    actual_value    NUMERIC(10,2) NOT NULL,
    expected_value  NUMERIC(10,2) NOT NULL,
    operator        VARCHAR(10) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    is_critical     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_rgr_report FOREIGN KEY (report_id) REFERENCES release_readiness_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_rgr_condition FOREIGN KEY (condition_id) REFERENCES quality_gate_conditions(id) ON DELETE CASCADE,
    CONSTRAINT chk_rgr_status CHECK (status IN ('PASS', 'FAIL')),
    CONSTRAINT chk_rgr_operator CHECK (operator IN ('LT', 'GT', 'LTE', 'GTE', 'EQ'))
);

CREATE INDEX idx_rgr_report_id ON release_gate_results(report_id);

-- ============================================================
-- RELEASE SIGN-OFFS
-- ============================================================
CREATE TABLE release_sign_offs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    release_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    role            VARCHAR(100) NOT NULL,
    decision        VARCHAR(20) NOT NULL,
    comment         TEXT,
    decided_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_rso_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE,
    CONSTRAINT fk_rso_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_rso_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_rso_release_user ON release_sign_offs(release_id, user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_rso_release_id ON release_sign_offs(release_id);

CREATE TRIGGER trg_rso_updated_at
    BEFORE UPDATE ON release_sign_offs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- INTEGRATION CONFIGS
-- ============================================================
CREATE TABLE integration_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    project_id      UUID,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    provider        VARCHAR(50) NOT NULL,
    webhook_secret  VARCHAR(500),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    config          JSONB NOT NULL DEFAULT '{}',
    last_event_at   TIMESTAMP WITH TIME ZONE,
    event_count     BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_ic_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ic_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT chk_ic_provider CHECK (provider IN ('GITHUB', 'GITLAB', 'JENKINS', 'JIRA', 'GENERIC'))
);

CREATE INDEX idx_ic_organization ON integration_configs(organization_id);
CREATE INDEX idx_ic_project ON integration_configs(project_id);
CREATE INDEX idx_ic_provider ON integration_configs(provider);
CREATE UNIQUE INDEX uq_ic_name_org ON integration_configs(name, organization_id) WHERE NOT is_deleted;

-- ============================================================
-- WEBHOOK EVENTS (append-only)
-- ============================================================
CREATE TABLE webhook_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id    UUID NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    provider          VARCHAR(50) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    payload           JSONB NOT NULL DEFAULT '{}',
    headers           JSONB NOT NULL DEFAULT '{}',
    processing_error  TEXT,
    processed_at      TIMESTAMP WITH TIME ZONE,
    received_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_we_integration FOREIGN KEY (integration_id) REFERENCES integration_configs(id) ON DELETE CASCADE,
    CONSTRAINT chk_we_status CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'SKIPPED', 'IGNORED'))
);

CREATE INDEX idx_we_integration ON webhook_events(integration_id);
CREATE INDEX idx_we_received_at ON webhook_events(integration_id, received_at DESC);
CREATE INDEX idx_we_status ON webhook_events(status);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    type        VARCHAR(20) NOT NULL DEFAULT 'INFO',
    link        VARCHAR(500),
    is_read     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE NOT is_read;

-- ============================================================
-- TEST DATA TEMPLATES
-- ============================================================
CREATE TABLE test_data_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    project_id      UUID,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    schema          JSONB NOT NULL DEFAULT '{}',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_tdt_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tdt_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

CREATE INDEX idx_tdt_organization ON test_data_templates(organization_id);
CREATE INDEX idx_tdt_project ON test_data_templates(project_id);
CREATE UNIQUE INDEX uq_tdt_name_org ON test_data_templates(name, organization_id) WHERE NOT is_deleted;

-- ============================================================
-- DATA FIELDS
-- ============================================================
CREATE TABLE data_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    field_type      VARCHAR(30) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    nullable        BOOLEAN NOT NULL DEFAULT FALSE,
    faker_generator VARCHAR(200),
    constraints     JSONB NOT NULL DEFAULT '{}',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_df_template FOREIGN KEY (template_id) REFERENCES test_data_templates(id) ON DELETE CASCADE,
    CONSTRAINT chk_df_field_type CHECK (field_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'EMAIL', 'PHONE', 'UUID', 'ENUM', 'REGEX_PATTERN'))
);

CREATE INDEX idx_df_template ON data_fields(template_id);
CREATE UNIQUE INDEX uq_df_name_template ON data_fields(name, template_id) WHERE NOT is_deleted;

-- ============================================================
-- TEST DATA PROFILES
-- ============================================================
CREATE TABLE test_data_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    profile_type    VARCHAR(30) NOT NULL,
    description     TEXT,
    overrides       JSONB NOT NULL DEFAULT '{}',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_tdp_template FOREIGN KEY (template_id) REFERENCES test_data_templates(id) ON DELETE CASCADE,
    CONSTRAINT chk_tdp_profile_type CHECK (profile_type IN ('POSITIVE', 'NEGATIVE', 'EDGE_CASE', 'BOUNDARY', 'LOCALIZATION', 'PERFORMANCE'))
);

CREATE INDEX idx_tdp_template ON test_data_profiles(template_id);
CREATE UNIQUE INDEX uq_tdp_name_template ON test_data_profiles(name, template_id) WHERE NOT is_deleted;

-- ============================================================
-- TEST DATA GENERATIONS
-- ============================================================
CREATE TABLE test_data_generations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL,
    profile_id      UUID,
    row_count       INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    export_format   VARCHAR(10) NOT NULL DEFAULT 'JSON',
    generated_data  JSONB,
    error_message   TEXT,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_tdg_template FOREIGN KEY (template_id) REFERENCES test_data_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_tdg_profile FOREIGN KEY (profile_id) REFERENCES test_data_profiles(id) ON DELETE SET NULL,
    CONSTRAINT chk_tdg_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_tdg_export_format CHECK (export_format IN ('JSON', 'CSV'))
);

CREATE INDEX idx_tdg_template ON test_data_generations(template_id);
CREATE INDEX idx_tdg_profile ON test_data_generations(profile_id);
CREATE INDEX idx_tdg_status ON test_data_generations(status);

-- ============================================================
-- FAILURE ANALYSES (AI-powered)
-- ============================================================
CREATE TABLE failure_analyses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_run_id         UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_category    VARCHAR(50),
    summary             TEXT,
    root_cause          TEXT,
    suggested_fix       TEXT,
    confidence_score    DECIMAL(5,2),
    ai_model            VARCHAR(100),
    prompt_tokens       INTEGER,
    completion_tokens   INTEGER,
    analysis_data       JSONB NOT NULL DEFAULT '{}',
    human_feedback      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    feedback_comment    TEXT,
    feedback_by         VARCHAR(255),
    feedback_at         TIMESTAMP WITH TIME ZONE,
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_fa_test_run FOREIGN KEY (test_run_id) REFERENCES test_runs(id) ON DELETE CASCADE,
    CONSTRAINT chk_fa_status CHECK (status IN ('PENDING', 'ANALYZING', 'COMPLETED', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_fa_category CHECK (failure_category IS NULL OR failure_category IN (
        'CODE_REGRESSION', 'FLAKY_TEST', 'ENVIRONMENT_ISSUE', 'DATA_ISSUE',
        'CONFIGURATION_ERROR', 'DEPENDENCY_FAILURE', 'TIMEOUT', 'ASSERTION_MISMATCH', 'UNKNOWN'
    )),
    CONSTRAINT chk_fa_feedback CHECK (human_feedback IN ('PENDING', 'ACCEPTED', 'REJECTED', 'PARTIALLY_ACCEPTED'))
);

CREATE INDEX idx_fa_test_run ON failure_analyses(test_run_id);
CREATE INDEX idx_fa_status ON failure_analyses(status);
CREATE INDEX idx_fa_feedback ON failure_analyses(human_feedback);
CREATE INDEX idx_fa_category ON failure_analyses(failure_category);
CREATE UNIQUE INDEX uq_fa_test_run ON failure_analyses(test_run_id) WHERE NOT is_deleted;

-- ============================================================
-- CONTRIBUTORS (auto-captured from webhooks)
-- ============================================================
CREATE TABLE contributors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    external_id     VARCHAR(100) NOT NULL,
    provider        VARCHAR(50) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    display_name    VARCHAR(500),
    email           VARCHAR(500),
    avatar_url      VARCHAR(2000),
    total_commits   INTEGER NOT NULL DEFAULT 0,
    total_pipelines INTEGER NOT NULL DEFAULT 0,
    total_merges    INTEGER NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    linked_user_id  UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_contrib_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_contrib_user FOREIGN KEY (linked_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_contrib_provider CHECK (provider IN ('GITLAB', 'GITHUB', 'JENKINS', 'GENERIC'))
);

CREATE UNIQUE INDEX uq_contributor_ext ON contributors (organization_id, provider, external_id) WHERE NOT is_deleted;
CREATE INDEX idx_contributor_username ON contributors (username);
CREATE INDEX idx_contributor_org ON contributors (organization_id) WHERE NOT is_deleted;

-- ============================================================
-- CONTRIBUTOR ACTIVITIES
-- ============================================================
CREATE TABLE contributor_activities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contributor_id  UUID NOT NULL,
    activity_type   VARCHAR(50) NOT NULL,
    project_id      UUID,
    branch          VARCHAR(500),
    commit_sha      VARCHAR(100),
    commit_message  TEXT,
    pipeline_id     VARCHAR(100),
    pipeline_status VARCHAR(50),
    event_data      JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_ca_contributor FOREIGN KEY (contributor_id) REFERENCES contributors(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT chk_ca_type CHECK (activity_type IN ('COMMIT', 'PIPELINE_TRIGGER', 'MERGE', 'TAG_PUSH', 'REVIEW'))
);

CREATE INDEX idx_ca_contributor ON contributor_activities (contributor_id);
CREATE INDEX idx_ca_project ON contributor_activities (project_id);
CREATE INDEX idx_ca_created ON contributor_activities (created_at DESC);

-- ============================================================
-- FULL-TEXT SEARCH INDEXES
-- ============================================================
CREATE INDEX idx_projects_search ON projects USING gin(to_tsvector('english', name || ' ' || COALESCE(description, ''))) WHERE NOT is_deleted;
CREATE INDEX idx_test_runs_search ON test_runs USING gin(to_tsvector('english', name || ' ' || COALESCE(environment, ''))) WHERE NOT is_deleted;
CREATE INDEX idx_defects_search ON defects USING gin(to_tsvector('english', title || ' ' || COALESCE(description, ''))) WHERE NOT is_deleted;
CREATE INDEX idx_teams_search ON teams USING gin(to_tsvector('english', name || ' ' || COALESCE(description, ''))) WHERE NOT is_deleted;
CREATE INDEX idx_documentation_assets_search ON documentation_assets USING gin(to_tsvector('english', title || ' ' || COALESCE(description, ''))) WHERE NOT is_deleted;

-- ============================================================
-- SEED: Permissions (all 50)
-- ============================================================
INSERT INTO permissions (id, name, description, module) VALUES
    -- Core
    (gen_random_uuid(), 'TEAM_READ', 'View teams', 'TEAM'),
    (gen_random_uuid(), 'TEAM_WRITE', 'Create and edit teams', 'TEAM'),
    (gen_random_uuid(), 'TEAM_DELETE', 'Delete teams', 'TEAM'),
    (gen_random_uuid(), 'PROJECT_READ', 'View projects', 'PROJECT'),
    (gen_random_uuid(), 'PROJECT_WRITE', 'Create and edit projects', 'PROJECT'),
    (gen_random_uuid(), 'PROJECT_DELETE', 'Delete projects', 'PROJECT'),
    (gen_random_uuid(), 'TEST_RUN_READ', 'View test runs', 'TEST_RUN'),
    (gen_random_uuid(), 'TEST_RUN_WRITE', 'Import and edit test runs', 'TEST_RUN'),
    (gen_random_uuid(), 'TEST_RUN_DELETE', 'Delete test runs', 'TEST_RUN'),
    (gen_random_uuid(), 'RELEASE_READ', 'View releases', 'RELEASE'),
    (gen_random_uuid(), 'RELEASE_WRITE', 'Create and manage releases', 'RELEASE'),
    (gen_random_uuid(), 'RELEASE_DELETE', 'Delete releases', 'RELEASE'),
    (gen_random_uuid(), 'DEFECT_READ', 'View defects', 'DEFECT'),
    (gen_random_uuid(), 'DEFECT_WRITE', 'Create and edit defects', 'DEFECT'),
    (gen_random_uuid(), 'DEFECT_DELETE', 'Delete defects', 'DEFECT'),
    (gen_random_uuid(), 'QUALITY_GATE_READ', 'View quality gates', 'QUALITY_GATE'),
    (gen_random_uuid(), 'QUALITY_GATE_WRITE', 'Create and manage quality gates', 'QUALITY_GATE'),
    (gen_random_uuid(), 'QUALITY_GATE_EVALUATE', 'Evaluate quality gates', 'QUALITY_GATE'),
    (gen_random_uuid(), 'QUALITY_GATE_OVERRIDE', 'Override quality gate decisions', 'QUALITY_GATE'),
    (gen_random_uuid(), 'USER_READ', 'View users', 'USER'),
    (gen_random_uuid(), 'USER_WRITE', 'Manage users', 'USER'),
    (gen_random_uuid(), 'ROLE_MANAGE', 'Manage roles and permissions', 'ROLE'),
    (gen_random_uuid(), 'AUDIT_READ', 'View audit logs', 'AUDIT'),
    (gen_random_uuid(), 'ORGANIZATION_MANAGE', 'Manage organization settings', 'ORGANIZATION'),
    -- QA Service Management
    (gen_random_uuid(), 'QA_SERVICE_READ', 'View QA service catalog', 'QA_SERVICE'),
    (gen_random_uuid(), 'QA_SERVICE_WRITE', 'Manage QA service catalog', 'QA_SERVICE'),
    (gen_random_uuid(), 'QA_REQUEST_READ', 'View QA service requests', 'QA_REQUEST'),
    (gen_random_uuid(), 'QA_REQUEST_WRITE', 'Create QA service requests', 'QA_REQUEST'),
    (gen_random_uuid(), 'QA_REQUEST_MANAGE', 'Triage, assign, and manage QA requests', 'QA_REQUEST'),
    (gen_random_uuid(), 'QA_REQUEST_OVERRIDE', 'Override priority on QA requests', 'QA_REQUEST'),
    (gen_random_uuid(), 'SLA_READ', 'View SLA configurations', 'SLA'),
    (gen_random_uuid(), 'SLA_WRITE', 'Manage SLA configurations', 'SLA'),
    (gen_random_uuid(), 'CAPACITY_READ', 'View capacity allocations', 'CAPACITY'),
    (gen_random_uuid(), 'CAPACITY_WRITE', 'Manage capacity allocations', 'CAPACITY'),
    -- Documentation
    (gen_random_uuid(), 'DOC_READ', 'View documentation assets', 'DOCUMENTATION'),
    (gen_random_uuid(), 'DOC_WRITE', 'Manage documentation assets', 'DOCUMENTATION'),
    (gen_random_uuid(), 'DOC_REVIEW', 'Review documentation assets', 'DOCUMENTATION'),
    (gen_random_uuid(), 'DOC_CHECK', 'Run documentation health checks', 'DOCUMENTATION'),
    -- Release Confidence
    (gen_random_uuid(), 'RELEASE_READINESS_READ', 'View release readiness reports', 'RELEASE_CONFIDENCE'),
    (gen_random_uuid(), 'RELEASE_READINESS_WRITE', 'Generate release readiness reports', 'RELEASE_CONFIDENCE'),
    (gen_random_uuid(), 'RELEASE_SIGNOFF', 'Sign off on releases', 'RELEASE_CONFIDENCE'),
    -- Integrations
    (gen_random_uuid(), 'INTEGRATION_READ', 'View integration configurations', 'INTEGRATIONS'),
    (gen_random_uuid(), 'INTEGRATION_WRITE', 'Create and update integrations', 'INTEGRATIONS'),
    (gen_random_uuid(), 'INTEGRATION_DELETE', 'Delete integration configurations', 'INTEGRATIONS'),
    -- Test Data
    (gen_random_uuid(), 'TEST_DATA_READ', 'View test data templates and generations', 'TEST_DATA'),
    (gen_random_uuid(), 'TEST_DATA_WRITE', 'Create and generate test data', 'TEST_DATA'),
    -- Failure Analysis
    (gen_random_uuid(), 'FAILURE_ANALYSIS_READ', 'View AI failure analysis results', 'FAILURE_ANALYSIS'),
    (gen_random_uuid(), 'FAILURE_ANALYSIS_WRITE', 'Trigger AI analysis and submit feedback', 'FAILURE_ANALYSIS'),
    -- Contributors
    (gen_random_uuid(), 'CONTRIBUTOR_READ', 'View contributors and activity history', 'CONTRIBUTORS'),
    (gen_random_uuid(), 'CONTRIBUTOR_WRITE', 'Manage contributor profiles and links', 'CONTRIBUTORS');

-- ============================================================
-- SEED: Default roles
-- ============================================================
INSERT INTO roles (id, organization_id, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'ADMIN', 'Full system administrator', TRUE),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'QA_MANAGER', 'Quality Assurance Manager', TRUE),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'QA_ENGINEER', 'Quality Assurance Engineer', TRUE),
    ('00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000001', 'DEVELOPER', 'Software Developer', TRUE),
    ('00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000001', 'VIEWER', 'Read-only access', TRUE);

-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000010', id FROM permissions;

-- QA_MANAGER: all except org management, user write, role manage, integration delete
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000011', id FROM permissions
WHERE name NOT IN ('ORGANIZATION_MANAGE', 'USER_WRITE', 'ROLE_MANAGE', 'INTEGRATION_DELETE');

-- QA_ENGINEER: reads, writes, evaluate — no deletes, overrides, or admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000012', id FROM permissions
WHERE name IN (
    'TEAM_READ', 'TEAM_WRITE', 'PROJECT_READ', 'PROJECT_WRITE',
    'TEST_RUN_READ', 'TEST_RUN_WRITE', 'RELEASE_READ', 'RELEASE_WRITE',
    'DEFECT_READ', 'DEFECT_WRITE', 'QUALITY_GATE_READ', 'QUALITY_GATE_WRITE',
    'QUALITY_GATE_EVALUATE', 'USER_READ', 'AUDIT_READ',
    'QA_SERVICE_READ', 'QA_REQUEST_READ', 'QA_REQUEST_WRITE', 'QA_REQUEST_MANAGE',
    'SLA_READ', 'CAPACITY_READ',
    'DOC_READ', 'DOC_WRITE', 'DOC_REVIEW',
    'RELEASE_READINESS_READ', 'RELEASE_READINESS_WRITE',
    'INTEGRATION_READ',
    'TEST_DATA_READ', 'TEST_DATA_WRITE',
    'FAILURE_ANALYSIS_READ', 'FAILURE_ANALYSIS_WRITE',
    'CONTRIBUTOR_READ'
);

-- DEVELOPER: reads plus limited writes
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000013', id FROM permissions
WHERE name IN (
    'TEAM_READ', 'PROJECT_READ', 'TEST_RUN_READ', 'TEST_RUN_WRITE',
    'RELEASE_READ', 'DEFECT_READ', 'DEFECT_WRITE',
    'QUALITY_GATE_READ', 'USER_READ', 'AUDIT_READ',
    'QA_SERVICE_READ', 'QA_REQUEST_READ', 'QA_REQUEST_WRITE',
    'SLA_READ', 'CAPACITY_READ',
    'DOC_READ', 'DOC_WRITE',
    'RELEASE_READINESS_READ',
    'INTEGRATION_READ',
    'TEST_DATA_READ',
    'FAILURE_ANALYSIS_READ',
    'CONTRIBUTOR_READ'
);

-- VIEWER: reads only
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000014', id FROM permissions
WHERE name IN (
    'TEAM_READ', 'PROJECT_READ', 'TEST_RUN_READ', 'RELEASE_READ',
    'DEFECT_READ', 'QUALITY_GATE_READ', 'USER_READ', 'AUDIT_READ',
    'QA_SERVICE_READ', 'QA_REQUEST_READ', 'SLA_READ', 'CAPACITY_READ',
    'DOC_READ',
    'RELEASE_READINESS_READ',
    'CONTRIBUTOR_READ'
);
