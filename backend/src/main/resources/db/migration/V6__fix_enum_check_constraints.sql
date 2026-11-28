-- Fix CHECK constraints to match all entity enum values

-- test_runs.source: was only PLAYWRIGHT/JUNIT, now includes all 6 sources
ALTER TABLE test_runs DROP CONSTRAINT IF EXISTS chk_test_runs_source;
ALTER TABLE test_runs ADD CONSTRAINT chk_test_runs_source
    CHECK (source IN ('PLAYWRIGHT', 'JUNIT', 'WEBDRIVERIO', 'CYPRESS', 'TESTNG', 'MAESTRO'));

-- integration_configs.provider: add MAESTRO
ALTER TABLE integration_configs DROP CONSTRAINT IF EXISTS chk_ic_provider;
ALTER TABLE integration_configs ADD CONSTRAINT chk_ic_provider
    CHECK (provider IN ('GITHUB', 'GITLAB', 'JENKINS', 'JIRA', 'MAESTRO', 'GENERIC'));

-- maestro_flow_results: add CHECK constraints for enum columns
ALTER TABLE maestro_flow_results ADD CONSTRAINT chk_maestro_flow_status
    CHECK (status IN ('PASSED', 'FAILED', 'ERROR', 'SKIPPED', 'CANCELLED'));

ALTER TABLE maestro_flow_results ADD CONSTRAINT chk_maestro_device_platform
    CHECK (device_platform IS NULL OR device_platform IN ('IOS', 'ANDROID', 'WEB'));
