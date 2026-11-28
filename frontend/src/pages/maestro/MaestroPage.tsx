import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { maestroApi } from '@/api/maestro';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Smartphone, List } from 'lucide-react';

function platformBadgeVariant(platform: string): 'info' | 'success' | 'default' {
  switch (platform) {
    case 'IOS': return 'info';
    case 'ANDROID': return 'success';
    case 'WEB': return 'default';
    default: return 'default';
  }
}

export default function MaestroPage() {
  const navigate = useNavigate();

  const { data: dashboard, isLoading, error } = useQuery({
    queryKey: ['maestro-dashboard'],
    queryFn: maestroApi.getDashboard,
  });

  if (isLoading) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load Maestro dashboard data.
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Maestro Mobile Testing</h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Mobile automation results across iOS, Android, and Web</p>
        </div>
        <Button onClick={() => navigate('/maestro/flows')}>
          <List className="h-4 w-4" />
          View All Flows
        </Button>
      </div>

      {dashboard && (
        <>
          <div className="grid grid-cols-3 gap-4 mb-8 sm:grid-cols-6">
            {[
              { label: 'Total Flows', value: dashboard.totalFlows },
              { label: 'Passed', value: dashboard.passedFlows },
              { label: 'Failed', value: dashboard.failedFlows },
              { label: 'Pass Rate', value: `${dashboard.passRate ?? 0}%` },
              { label: 'Devices', value: dashboard.uniqueDevices },
              { label: 'Unique Flows', value: dashboard.uniqueFlowNames },
            ].map(({ label, value }) => (
              <div
                key={label}
                className="bg-white rounded-xl border border-[var(--oqh-border)] p-5 shadow-[var(--oqh-shadow)]"
              >
                <p className="text-sm text-[var(--oqh-text-muted)]">{label}</p>
                <p className="text-2xl font-bold text-[var(--oqh-text)] mt-1">{value}</p>
              </div>
            ))}
          </div>

          {dashboard.deviceMatrix.devices.length > 0 && (
            <div className="bg-white rounded-xl border border-[var(--oqh-border)] overflow-hidden shadow-[var(--oqh-shadow)]">
              <div className="p-4 border-b border-[var(--oqh-border)]">
                <h2 className="font-semibold text-[var(--oqh-text)] flex items-center gap-2">
                  <Smartphone className="h-4 w-4 text-[var(--oqh-blue)]" />
                  Device Matrix
                </h2>
              </div>
              <table className="min-w-full divide-y divide-[var(--oqh-border)]">
                <thead className="bg-[#F7F9FC]">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Device</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Platform</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">OS Version</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Count</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--oqh-border)]">
                  {dashboard.deviceMatrix.devices.map((entry, idx) => (
                    <tr key={idx} className="hover:bg-[#F7F9FC] transition-colors">
                      <td className="px-6 py-4 text-sm font-medium text-[var(--oqh-text)]">{entry.deviceName}</td>
                      <td className="px-6 py-4">
                        <Badge variant={platformBadgeVariant(entry.platform)}>{entry.platform}</Badge>
                      </td>
                      <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{entry.osVersion}</td>
                      <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{entry.status}</td>
                      <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{entry.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
