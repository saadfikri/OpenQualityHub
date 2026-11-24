import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { auditApi } from '@/api/audit';
import { Badge } from '@/components/ui/Badge';
import { ScrollText } from 'lucide-react';
import type { Page, AuditEventItem } from '@/types/api';

const ACTION_VARIANT: Record<string, 'success' | 'danger' | 'info' | 'warning' | 'default'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'danger',
  LOGIN: 'default',
  LOGOUT: 'default',
  IMPORT: 'warning',
  EXPORT: 'warning',
  EVALUATE: 'info',
  OVERRIDE: 'warning',
};

export function AuditTrailPage() {
  const [page, setPage] = useState(0);
  const [actionFilter, setActionFilter] = useState('');
  const [entityFilter, setEntityFilter] = useState('');

  const params: Record<string, string> = {
    page: String(page),
    size: '20',
    sort: 'createdAt,desc',
  };
  if (actionFilter) params.action = actionFilter;
  if (entityFilter) params.entityType = entityFilter;

  const { data, isLoading } = useQuery<Page<AuditEventItem>>({
    queryKey: ['audit', page, actionFilter, entityFilter],
    queryFn: () => auditApi.getEvents(params),
  });

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading audit trail...</div>;
  }

  const events = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)] flex items-center gap-2">
            <ScrollText className="h-7 w-7 text-[var(--oqh-blue)]" />
            Audit Trail
          </h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Track all changes and actions across the platform.</p>
        </div>
        <div className="flex gap-3">
          <select
            value={actionFilter}
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            className="text-sm border border-[var(--oqh-border)] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)]"
          >
            <option value="">All Actions</option>
            {['CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'IMPORT', 'EXPORT'].map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
          <select
            value={entityFilter}
            onChange={(e) => { setEntityFilter(e.target.value); setPage(0); }}
            className="text-sm border border-[var(--oqh-border)] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)]"
          >
            <option value="">All Entities</option>
            {['TestRun', 'Defect', 'Project', 'Team', 'Release', 'User', 'Integration'].map((e) => (
              <option key={e} value={e}>{e}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] overflow-hidden">
        <table className="min-w-full divide-y divide-[var(--oqh-border)]">
          <thead className="bg-[#F7F9FC]">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Timestamp</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Action</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Entity</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--oqh-border)]">
            {events.map((ev) => (
              <tr key={ev.id} className="hover:bg-[#F7F9FC] transition-colors">
                <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)] whitespace-nowrap">
                  {new Date(ev.createdAt).toLocaleString()}
                </td>
                <td className="px-6 py-4 text-sm text-[var(--oqh-text)] whitespace-nowrap">
                  {ev.userName ?? 'System'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <Badge variant={ACTION_VARIANT[ev.action] ?? 'default'}>{ev.action}</Badge>
                </td>
                <td className="px-6 py-4 text-sm text-[var(--oqh-text)] whitespace-nowrap">
                  {ev.entityType}
                  {ev.entityId && (
                    <span className="ml-1 text-xs text-[#9BA8B9] font-mono">{ev.entityId.substring(0, 8)}</span>
                  )}
                </td>
                <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)] max-w-md truncate">
                  {ev.details || '-'}
                </td>
              </tr>
            ))}
            {events.length === 0 && (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-[var(--oqh-text-muted)]">
                  No audit events found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-[var(--oqh-text-muted)]">Page {data.number + 1} of {data.totalPages}</p>
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
