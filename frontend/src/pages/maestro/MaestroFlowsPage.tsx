import { Fragment, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { maestroApi } from '@/api/maestro';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { Smartphone } from 'lucide-react';
import type { MaestroFlowResult } from '@/types/api';

const STATUS_VARIANT: Record<MaestroFlowResult['status'], 'success' | 'danger' | 'warning' | 'default'> = {
  PASSED: 'success',
  FAILED: 'danger',
  ERROR: 'warning',
  SKIPPED: 'default',
  CANCELLED: 'default',
};

function platformBadgeVariant(platform: string | null): 'info' | 'success' | 'default' {
  switch (platform) {
    case 'IOS': return 'info';
    case 'ANDROID': return 'success';
    case 'WEB': return 'default';
    default: return 'default';
  }
}

function formatDuration(ms: number | null) {
  if (ms === null) return '-';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

export default function MaestroFlowsPage() {
  const [platform, setPlatform] = useState<string | undefined>();
  const [page, setPage] = useState(0);
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['maestro-flows', page, platform],
    queryFn: () => maestroApi.getFlowResults(page, 20, platform),
  });

  function handlePlatformChange(value: string) {
    setPlatform(value === '' ? undefined : value);
    setPage(0);
  }

  if (isLoading) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load Maestro flow results.
      </div>
    );
  }

  const flows = data?.content ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Maestro Flow Results</h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Mobile automation flow execution results by device and platform</p>
        </div>
        <div className="flex gap-3">
          <select
            value={platform ?? ''}
            onChange={(e) => handlePlatformChange(e.target.value)}
            className="text-sm border border-[var(--oqh-border)] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)]"
          >
            <option value="">All Platforms</option>
            <option value="IOS">iOS</option>
            <option value="ANDROID">Android</option>
            <option value="WEB">Web</option>
          </select>
        </div>
      </div>

      {flows.length === 0 ? (
        <EmptyState
          icon={<Smartphone className="h-12 w-12" />}
          title="No flow results yet"
          description="Upload a Maestro test results file to see flow execution data here."
        />
      ) : (
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] overflow-hidden shadow-[var(--oqh-shadow)]">
          <table className="min-w-full divide-y divide-[var(--oqh-border)]">
            <thead className="bg-[#F7F9FC]">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Flow Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Device</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Platform</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">OS</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Duration</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Retries</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]">
              {flows.map((flow) => (
                <Fragment key={flow.id}>
                  <tr
                    className="hover:bg-[#F7F9FC] transition-colors cursor-pointer"
                    onClick={() => setExpandedRow(expandedRow === flow.id ? null : flow.id)}
                  >
                    <td className="px-6 py-4">
                      <div className="font-medium text-[var(--oqh-text)] text-sm">{flow.flowName}</div>
                      {flow.flowFile && (
                        <div className="text-xs text-[#9BA8B9] font-mono">{flow.flowFile}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text)]">
                      {flow.deviceName ?? '-'}
                      {flow.deviceModel && flow.deviceModel !== flow.deviceName && (
                        <div className="text-xs text-[var(--oqh-text-muted)]">{flow.deviceModel}</div>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {flow.devicePlatform ? (
                        <Badge variant={platformBadgeVariant(flow.devicePlatform)}>
                          {flow.devicePlatform}
                        </Badge>
                      ) : (
                        <span className="text-sm text-[var(--oqh-text-muted)]">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">
                      {flow.osVersion ?? '-'}
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={STATUS_VARIANT[flow.status]}>{flow.status}</Badge>
                    </td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">
                      {formatDuration(flow.durationMs)}
                    </td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">
                      {flow.retryCount > 0 ? (
                        <span className="text-amber-600 font-medium">{flow.retryCount}</span>
                      ) : (
                        <span>0</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)] whitespace-nowrap">
                      {flow.createdAt ? new Date(flow.createdAt).toLocaleDateString() : '-'}
                    </td>
                  </tr>
                  {expandedRow === flow.id && flow.errorMessage && (
                    <tr className="bg-red-50">
                      <td colSpan={8} className="px-6 py-3">
                        <div className="text-xs font-semibold text-red-700 mb-1 uppercase tracking-wider">Error</div>
                        <div className="text-sm text-red-700 font-mono whitespace-pre-wrap">{flow.errorMessage}</div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-6">
          <p className="text-sm text-[var(--oqh-text-muted)]">
            Page {data.number + 1} of {data.totalPages}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={data.first}
              className="px-3 py-1.5 text-sm font-medium rounded-md border border-[var(--oqh-border)] text-[var(--oqh-text)] hover:bg-[#F7F9FC] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last}
              className="px-3 py-1.5 text-sm font-medium rounded-md border border-[var(--oqh-border)] text-[var(--oqh-text)] hover:bg-[#F7F9FC] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
