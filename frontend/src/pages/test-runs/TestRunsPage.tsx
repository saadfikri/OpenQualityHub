import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { testRunsApi } from '@/api/test-runs';
import { projectsApi } from '@/api/projects';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { TestTube2, Upload, Trash2, Download } from 'lucide-react';
import { exportApi } from '@/api/export';

function sourceBadgeVariant(source: string): 'info' | 'success' | 'warning' | 'default' {
  switch (source) {
    case 'JUNIT': return 'info';
    case 'PLAYWRIGHT': return 'success';
    case 'WEBDRIVERIO': return 'warning';
    case 'CYPRESS': return 'success';
    case 'TESTNG': return 'info';
    default: return 'default';
  }
}

export function TestRunsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [importOpen, setImportOpen] = useState(false);
  const [importForm, setImportForm] = useState({
    projectId: '',
    name: '',
    environment: '',
    source: 'junit' as 'junit' | 'playwright' | 'webdriverio' | 'cypress' | 'testng',
    file: null as File | null,
  });

  const { data, isLoading } = useQuery({
    queryKey: ['test-runs'],
    queryFn: () => testRunsApi.getAll(),
  });

  const { data: projects } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectsApi.getAll(0, 100),
  });

  const importMutation = useMutation({
    mutationFn: () => {
      if (!importForm.file || !importForm.projectId || !importForm.name) {
        throw new Error('Missing required fields');
      }
      const importFns = {
        junit: testRunsApi.importJUnit,
        playwright: testRunsApi.importPlaywright,
        webdriverio: testRunsApi.importWebdriverIO,
        cypress: testRunsApi.importCypress,
        testng: testRunsApi.importTestNG,
      };
      const fn = importFns[importForm.source];
      return fn(importForm.projectId, importForm.name, importForm.file, importForm.environment || undefined);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-runs'] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setImportOpen(false);
      setImportForm({ projectId: '', name: '', environment: '', source: 'junit' as 'junit' | 'playwright' | 'webdriverio' | 'cypress' | 'testng', file: null });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: testRunsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['test-runs'] }),
  });

  function formatDuration(ms: number | null) {
    if (ms === null) return '-';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  }

  if (isLoading) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Test Runs</h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Import and track test execution results</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => exportApi.testRunsCsv()}>
            <Download className="h-4 w-4" />
            Export CSV
          </Button>
          <Button onClick={() => setImportOpen(true)} disabled={!projects?.content.length}>
            <Upload className="h-4 w-4" />
            Import Results
          </Button>
        </div>
      </div>

      {data?.content.length === 0 ? (
        <EmptyState
          icon={<TestTube2 className="h-12 w-12" />}
          title="No test runs yet"
          description="Import your first JUnit XML, Playwright JSON, or WebdriverIO JSON results."
          action={
            projects?.content.length ? (
              <Button onClick={() => setImportOpen(true)}>
                <Upload className="h-4 w-4" />
                Import Results
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] overflow-hidden shadow-[var(--oqh-shadow)]">
          <table className="min-w-full divide-y divide-[var(--oqh-border)]">
            <thead className="bg-[#F7F9FC]">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Run</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Source</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Results</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Pass Rate</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Duration</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]">
              {data?.content.map((run) => (
                <tr
                  key={run.id}
                  className="hover:bg-[#F7F9FC] cursor-pointer"
                  onClick={() => navigate(`/test-runs/${run.id}`)}
                >
                  <td className="px-6 py-4">
                    <div className="font-medium text-[var(--oqh-text)]">{run.name}</div>
                    <div className="text-sm text-[var(--oqh-text-muted)]">
                      {run.projectName}
                      {run.branch && <> &middot; <span className="font-mono text-xs">{run.branch}</span></>}
                      {run.commitSha && <> &middot; <span className="font-mono text-xs text-[#9BA8B9]">{run.commitSha.substring(0, 8)}</span></>}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant={sourceBadgeVariant(run.source)}>{run.source}</Badge>
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <span className="text-green-600">{run.passed}</span>
                    {' / '}
                    <span className="text-red-600">{run.failed}</span>
                    {' / '}
                    <span className="text-[var(--oqh-text-muted)]">{run.skipped}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`text-sm font-semibold ${run.passRate >= 90 ? 'text-green-600' : run.passRate >= 70 ? 'text-yellow-600' : 'text-red-600'}`}>
                      {run.passRate}%
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{formatDuration(run.durationMs)}</td>
                  <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{new Date(run.executedAt).toLocaleDateString()}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (confirm('Delete this test run?')) deleteMutation.mutate(run.id);
                      }}
                      className="p-1.5 text-[#9BA8B9] hover:text-red-600 rounded"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={importOpen} onClose={() => setImportOpen(false)} title="Import Test Results" size="lg">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            importMutation.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <label htmlFor="source" className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Source Format</label>
            <select
              id="source"
              value={importForm.source}
              onChange={(e) => setImportForm({ ...importForm, source: e.target.value as 'junit' | 'playwright' | 'webdriverio' | 'cypress' | 'testng' })}
              className="block w-full rounded-lg border border-[var(--oqh-border)] px-3 py-2 text-sm focus:border-[var(--oqh-blue)] focus:outline-none focus:ring-1 focus:ring-[var(--oqh-blue)]"
            >
              <option value="junit">JUnit XML</option>
              <option value="playwright">Playwright JSON</option>
              <option value="webdriverio">WebdriverIO JSON</option>
              <option value="cypress">Cypress (Mochawesome) JSON</option>
              <option value="testng">TestNG XML</option>
            </select>
          </div>
          <div>
            <label htmlFor="projectId" className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Project</label>
            <select
              id="projectId"
              required
              value={importForm.projectId}
              onChange={(e) => setImportForm({ ...importForm, projectId: e.target.value })}
              className="block w-full rounded-lg border border-[var(--oqh-border)] px-3 py-2 text-sm focus:border-[var(--oqh-blue)] focus:outline-none focus:ring-1 focus:ring-[var(--oqh-blue)]"
            >
              <option value="">Select a project</option>
              {projects?.content.map((p) => (
                <option key={p.id} value={p.id}>{p.name} ({p.teamName})</option>
              ))}
            </select>
          </div>
          <Input
            id="run-name"
            label="Run Name"
            required
            value={importForm.name}
            onChange={(e) => setImportForm({ ...importForm, name: e.target.value })}
            placeholder="e.g. Sprint 42 - Regression"
          />
          <Input
            id="environment"
            label="Environment (optional)"
            value={importForm.environment}
            onChange={(e) => setImportForm({ ...importForm, environment: e.target.value })}
            placeholder="e.g. staging, production"
          />
          <div>
            <label htmlFor="file" className="block text-sm font-medium text-[var(--oqh-text)] mb-1">
              Results File ({importForm.source === 'junit' || importForm.source === 'testng' ? 'XML' : 'JSON'})
            </label>
            <input
              id="file"
              type="file"
              required
              accept={importForm.source === 'junit' || importForm.source === 'testng' ? '.xml' : '.json'}
              onChange={(e) => setImportForm({ ...importForm, file: e.target.files?.[0] ?? null })}
              className="block w-full text-sm text-[var(--oqh-text-muted)] file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-[var(--oqh-active-bg)] file:text-[var(--oqh-blue)] hover:file:bg-[var(--oqh-active-bg)]"
            />
          </div>
          {importMutation.isError && (
            <p className="text-sm text-red-600">Failed to import results. Please check the file format.</p>
          )}
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" type="button" onClick={() => setImportOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={importMutation.isPending}>
              {importMutation.isPending ? 'Importing...' : 'Import'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
