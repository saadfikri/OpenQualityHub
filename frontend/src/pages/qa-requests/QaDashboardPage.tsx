import { useQuery } from '@tanstack/react-query';
import { qaRequestsApi, capacityApi } from '@/api/qa-services';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts';
import {
  Inbox, Clock, CheckCircle2, XCircle, AlertTriangle, Users,
} from 'lucide-react';
import type { QaRequestDashboard, CapacityOverview } from '@/types/api';

const STATUS_COLORS: Record<string, string> = {
  SUBMITTED: '#6b7280', TRIAGED: '#3b82f6', ASSIGNED: '#f59e0b',
  IN_PROGRESS: '#8b5cf6', ON_HOLD: '#ef4444', COMPLETED: '#10b981',
  CLOSED: '#059669', CANCELLED: '#9ca3af',
};

const UTILIZATION_COLORS: Record<string, string> = {
  LOW: '#10b981', MODERATE: '#3b82f6', HIGH: '#f59e0b',
  OVERLOADED: '#ef4444', NO_CAPACITY: '#6b7280',
};

export function QaDashboardPage() {
  const { data: dashboard, isLoading: loadingDash } = useQuery<QaRequestDashboard>({
    queryKey: ['qa-dashboard'],
    queryFn: qaRequestsApi.getDashboard,
  });

  const { data: capacity, isLoading: loadingCap } = useQuery<CapacityOverview>({
    queryKey: ['capacity-overview'],
    queryFn: () => capacityApi.getOverview(),
  });

  if (loadingDash || loadingCap) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (!dashboard || !capacity) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load QA dashboard data.
      </div>
    );
  }

  const stats = [
    { name: 'Total Requests', value: dashboard.totalRequests, icon: Inbox, color: 'bg-[var(--oqh-blue)]' },
    { name: 'Open', value: dashboard.openRequests, icon: Clock, color: 'bg-amber-500' },
    { name: 'In Progress', value: dashboard.inProgressRequests, icon: Users, color: 'bg-violet-500' },
    { name: 'Completed', value: dashboard.completedRequests, icon: CheckCircle2, color: 'bg-emerald-500' },
    { name: 'Cancelled', value: dashboard.cancelledRequests, icon: XCircle, color: 'bg-gray-500' },
    { name: 'SLA Breaches', value: dashboard.slaBreachCount, icon: AlertTriangle, color: 'bg-red-500' },
  ];

  const statusData = dashboard.requestsByStatus.filter((s) => s.count > 0);
  const serviceData = dashboard.requestsByService.filter((s) => s.count > 0);

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-[var(--oqh-text)]">QA Service Dashboard</h1>
        <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
          Cross-team QA request metrics and capacity overview
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 mb-8">
        {stats.map((stat) => (
          <div key={stat.name} className="bg-white rounded-xl border border-[var(--oqh-border)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <div className="flex items-center gap-3">
              <div className={`${stat.color} p-2.5 rounded-lg`}>
                <stat.icon className="h-5 w-5 text-white" />
              </div>
              <div>
                <p className="text-xs text-[var(--oqh-text-muted)]">{stat.name}</p>
                <p className="text-xl font-bold text-[var(--oqh-text)]">{stat.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Requests by Status */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Requests by Status</h2>
          {statusData.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No data</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={statusData}
                  dataKey="count"
                  nameKey="status"
                  cx="50%"
                  cy="50%"
                  outerRadius={90}
                  label={({ name, value }) => `${name}: ${value}`}
                >
                  {statusData.map((entry) => (
                    <Cell key={entry.status} fill={STATUS_COLORS[entry.status] || '#6b7280'} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Requests by Service */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Requests by Service</h2>
          {serviceData.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No data</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={serviceData} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis dataKey="serviceName" type="category" tick={{ fontSize: 10 }} width={140} />
                <Tooltip />
                <Bar dataKey="count" fill="#3b82f6" name="Requests" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Capacity Overview */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] mb-8 shadow-[var(--oqh-shadow)]">
        <div className="p-6 border-b border-[var(--oqh-border)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Team Capacity</h2>
          <p className="text-sm text-[var(--oqh-text-muted)] mt-1">Current week workload</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-[#F7F9FC] text-left text-[var(--oqh-text-muted)]">
                <th className="px-6 py-3 font-medium">Team</th>
                <th className="px-6 py-3 font-medium text-right">Total Hours</th>
                <th className="px-6 py-3 font-medium text-right">Allocated</th>
                <th className="px-6 py-3 font-medium text-right">Available</th>
                <th className="px-6 py-3 font-medium text-center">Active Requests</th>
                <th className="px-6 py-3 font-medium text-right">Est. Workload</th>
                <th className="px-6 py-3 font-medium text-center">Utilization</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]/50">
              {capacity.teams.map((team) => (
                <tr key={team.teamId} className="hover:bg-[#F7F9FC]">
                  <td className="px-6 py-3 font-medium text-[var(--oqh-text)]">{team.teamName}</td>
                  <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)]">{team.totalHours}h</td>
                  <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)]">{team.allocatedHours}h</td>
                  <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)]">{team.availableHours}h</td>
                  <td className="px-6 py-3 text-center text-[var(--oqh-text-muted)]">{team.activeRequests}</td>
                  <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)]">{team.estimatedWorkload}h</td>
                  <td className="px-6 py-3 text-center">
                    <span
                      className="inline-block px-2 py-0.5 rounded text-xs font-bold text-white"
                      style={{ backgroundColor: UTILIZATION_COLORS[team.utilizationLevel] || '#6b7280' }}
                    >
                      {team.utilizationLevel}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
