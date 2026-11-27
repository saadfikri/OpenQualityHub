-- Maestro mobile testing integration: flow results with device matrix

CREATE TABLE maestro_flow_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         BIGINT       NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN      NOT NULL DEFAULT false,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    test_run_id     UUID         NOT NULL REFERENCES test_runs(id),
    test_case_id    UUID         REFERENCES test_cases(id),
    flow_name       VARCHAR(500) NOT NULL,
    flow_file       VARCHAR(500),
    device_name     VARCHAR(200),
    device_platform VARCHAR(20),
    os_version      VARCHAR(50),
    device_model    VARCHAR(200),
    device_locale   VARCHAR(20),
    app_id          VARCHAR(300),
    app_version     VARCHAR(100),
    screenshot_url  VARCHAR(1000),
    video_url       VARCHAR(1000),
    duration_ms     BIGINT,
    status          VARCHAR(20)  NOT NULL,
    error_message   TEXT,
    retry_count     INTEGER      NOT NULL DEFAULT 0
);

-- Indexes for device matrix queries
CREATE INDEX idx_maestro_flow_test_run ON maestro_flow_results (test_run_id) WHERE is_deleted = false;
CREATE INDEX idx_maestro_flow_platform ON maestro_flow_results (device_platform) WHERE is_deleted = false;
CREATE INDEX idx_maestro_flow_device ON maestro_flow_results (device_name, os_version) WHERE is_deleted = false;
CREATE INDEX idx_maestro_flow_status ON maestro_flow_results (status) WHERE is_deleted = false;
CREATE INDEX idx_maestro_flow_app ON maestro_flow_results (app_id, app_version) WHERE is_deleted = false;
CREATE INDEX idx_maestro_flow_name ON maestro_flow_results (flow_name) WHERE is_deleted = false;
