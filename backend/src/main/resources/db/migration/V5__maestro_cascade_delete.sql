-- Add ON DELETE CASCADE to maestro_flow_results FK references
-- Prevents FK violations when test_runs or test_cases are hard-deleted

ALTER TABLE maestro_flow_results
    DROP CONSTRAINT IF EXISTS maestro_flow_results_test_run_id_fkey,
    ADD CONSTRAINT maestro_flow_results_test_run_id_fkey
        FOREIGN KEY (test_run_id) REFERENCES test_runs(id) ON DELETE CASCADE;

ALTER TABLE maestro_flow_results
    DROP CONSTRAINT IF EXISTS maestro_flow_results_test_case_id_fkey,
    ADD CONSTRAINT maestro_flow_results_test_case_id_fkey
        FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE;
