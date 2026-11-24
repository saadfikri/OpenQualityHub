import { useQuery } from '@tanstack/react-query';
import { qaRequestsApi } from '@/api/qa-services';
import { Badge } from '@/components/ui/Badge';
import { Link } from 'react-router-dom';
import type { Page, QaServiceRequestItem } from '@/types/api';

const STATUS_COLORS: Record<string, 'default' | 'success' | 'warning' | 'danger'> = {
  SUBMITTED: 'default',
  TRIAGED: 'default',
  ASSIGNED: 'warning',
  IN_PROGRESS: 'warning',
  ON_HOLD: 'danger',
  COMPLETED: 'success',
  CLOSED: 'success',
  CANCELLED: 'danger',
};

const PRIORITY_LABELS: Record<number, { label: string; color: string }> = {
  1: { label: 'P1', color: 'text-red-600 bg-red-50' },
  2: { label: 'P2', color: 'text-orange-600 bg-orange-50' },
  3: { label: 'P3', color: 'text-amber-600 bg-amber-50' },
  4: { label: 'P4', color: 'text-[var(--oqh-blue)] bg-[var(--oqh-active-bg)]' },
  5: { label: 'P5', color: 'text-[var(--oqh-text-muted)] bg-[#F7F9FC]' },
};

export function QaRequestsPage() {
  const { data, isLoading, error } = useQuery<Page<QaServiceRequestItem>>({
    queryKey: ['qa-requests'],
    queryFn: () => qaRequestsApi.list({ size: '50', sort: 'createdAt,desc' }),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load QA requests.
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[var(--oqh-text)]">QA Service Requests</h1>
        <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
          Cross-team QA service request management
        </p>
      </div>

      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)]">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-[#F7F9FC] text-left text-[var(--oqh-text-muted)]">
                <th className="px-6 py-3 font-medium">Request</th>
                <th className="px-6 py-3 font-medium">Service</th>
                <th className="px-6 py-3 font-medium">Requesting Team</th>
                <th className="px-6 py-3 font-medium text-center">Priority</th>
                <th className="px-6 py-3 font-medium text-center">Status</th>
                <th className="px-6 py-3 font-medium">Assigned To</th>
                <th className="px-6 py-3 font-medium text-right">SLA Deadline</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]/50">
              {data.content.map((req) => {
                const p = PRIORITY_LABELS[req.effectivePriority] || PRIORITY_LABELS[3];
                return (
                  <tr key={req.id} className="hover:bg-[#F7F9FC]">
                    <td className="px-6 py-3">
                      <Link to={`/qa-requests/${req.id}`} className="hover:text-[var(--oqh-blue)]">
                        <span className="font-mono text-xs text-[#9BA8B9] mr-2">{req.requestNumber}</span>
                        <span className="font-medium text-[var(--oqh-text)]">{req.title}</span>
                      </Link>
                    </td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)]">{req.qaServiceName}</td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)]">{req.requestingTeamName}</td>
                    <td className="px-6 py-3 text-center">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold ${p.color}`}>
                        {p.label}
                        {req.priorityOverride != null && (
                          <span className="ml-1 text-[10px]" title={`Override: ${req.overrideReason}`}>*</span>
                        )}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-center">
                      <Badge variant={STATUS_COLORS[req.status] || 'default'}>
                        {req.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)]">
                      {req.assignedUserName || <span className="text-[#9BA8B9]">Unassigned</span>}
                    </td>
                    <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)] text-xs">
                      {req.resolutionDeadline
                        ? new Date(req.resolutionDeadline).toLocaleDateString('en', {
                            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                          })
                        : '-'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {data.content.length === 0 && (
          <div className="text-center py-12 text-[var(--oqh-text-muted)]">No QA requests found</div>
        )}
      </div>
    </div>
  );
}
