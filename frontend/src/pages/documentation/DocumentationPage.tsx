import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
} from 'recharts';
import {
  FileText, Heart, AlertTriangle, XCircle, HelpCircle, UserX, Clock,
  RefreshCw, ChevronRight, Eye,
} from 'lucide-react';
import { documentationApi } from '@/api/documentation';
import type { Page, DocumentationAssetItem, DocumentationDashboard } from '@/types/api';

const HEALTH_CONFIG: Record<string, { color: string; bg: string; icon: typeof Heart }> = {
  HEALTHY: { color: 'text-emerald-700', bg: 'bg-emerald-100', icon: Heart },
  WARNING: { color: 'text-amber-700', bg: 'bg-amber-100', icon: AlertTriangle },
  CRITICAL: { color: 'text-red-700', bg: 'bg-red-100', icon: XCircle },
  UNKNOWN: { color: 'text-[var(--oqh-text-muted)]', bg: 'bg-[#F7F9FC]', icon: HelpCircle },
};

const DOC_TYPE_LABELS: Record<string, string> = {
  API_SPEC: 'API Spec',
  USER_GUIDE: 'User Guide',
  RUNBOOK: 'Runbook',
  ARCHITECTURE: 'Architecture',
  TEST_PLAN: 'Test Plan',
  RELEASE_NOTES: 'Release Notes',
  ONBOARDING: 'Onboarding',
  POLICY: 'Policy',
  WIKI: 'Wiki',
  OTHER: 'Other',
};

const HEALTH_CHART_COLORS: Record<string, string> = {
  HEALTHY: '#10b981',
  WARNING: '#f59e0b',
  CRITICAL: '#ef4444',
  UNKNOWN: '#9ca3af',
};

export function DocumentationPage() {
  const [healthFilter, setHealthFilter] = useState<string>('');
  const queryClient = useQueryClient();

  const { data: dashboard, isLoading: loadingDash } = useQuery<DocumentationDashboard>({
    queryKey: ['doc-dashboard'],
    queryFn: documentationApi.getDashboard,
  });

  const { data: assetsPage, isLoading: loadingAssets } = useQuery<Page<DocumentationAssetItem>>({
    queryKey: ['documentation', healthFilter],
    queryFn: () => documentationApi.list(healthFilter ? { healthStatus: healthFilter, size: '50' } : { size: '50' }),
  });

  const runChecks = useMutation({
    mutationFn: (id: string) => documentationApi.runHealthChecks(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documentation'] });
      queryClient.invalidateQueries({ queryKey: ['doc-dashboard'] });
    },
  });

  if (loadingDash || loadingAssets) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (!dashboard || !assetsPage) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load documentation data.
      </div>
    );
  }

  const assets = assetsPage.content;

  const stats = [
    { name: 'Total Assets', value: dashboard.totalAssets, icon: FileText, color: 'bg-[var(--oqh-blue)]' },
    { name: 'Healthy', value: dashboard.healthyCount, icon: Heart, color: 'bg-emerald-500' },
    { name: 'Warning', value: dashboard.warningCount, icon: AlertTriangle, color: 'bg-amber-500' },
    { name: 'Critical', value: dashboard.criticalCount, icon: XCircle, color: 'bg-red-500' },
    { name: 'Unknown', value: dashboard.unknownCount, icon: HelpCircle, color: 'bg-gray-500' },
    { name: 'Missing Owner', value: dashboard.missingOwnerCount, icon: UserX, color: 'bg-orange-500' },
    { name: 'Pending Reviews', value: dashboard.pendingReviews, icon: Clock, color: 'bg-violet-500' },
  ];

  const healthPieData = [
    { name: 'Healthy', value: dashboard.healthyCount },
    { name: 'Warning', value: dashboard.warningCount },
    { name: 'Critical', value: dashboard.criticalCount },
    { name: 'Unknown', value: dashboard.unknownCount },
  ].filter((d) => d.value > 0);

  const typeBarData = dashboard.assetsByType.filter((t) => t.count > 0).map((t) => ({
    ...t,
    label: DOC_TYPE_LABELS[t.type] || t.type,
  }));

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Documentation Intelligence</h1>
        <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
          Track documentation health, ownership, and review compliance
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 gap-4 mb-8">
        {stats.map((stat) => (
          <div key={stat.name} className="bg-white rounded-xl border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <div className="flex items-center gap-3">
              <div className={`${stat.color} p-2 rounded-lg`}>
                <stat.icon className="h-4 w-4 text-white" />
              </div>
              <div>
                <p className="text-xs text-[var(--oqh-text-muted)]">{stat.name}</p>
                <p className="text-lg font-bold text-[var(--oqh-text)]">{stat.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Health Distribution</h2>
          {healthPieData.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No data</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={healthPieData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={85}
                  label={({ name, value }) => `${name}: ${value}`}
                >
                  {healthPieData.map((entry) => (
                    <Cell key={entry.name} fill={HEALTH_CHART_COLORS[entry.name.toUpperCase()] || '#6b7280'} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Assets by Type</h2>
          {typeBarData.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No data</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={typeBarData} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis dataKey="label" type="category" tick={{ fontSize: 10 }} width={100} />
                <Tooltip />
                <Bar dataKey="count" fill="#3b82f6" name="Assets" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Filter + Assets List */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)]">
        <div className="p-6 border-b border-[var(--oqh-border)] flex items-center justify-between">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Documentation Assets</h2>
          <div className="flex items-center gap-2">
            <select
              value={healthFilter}
              onChange={(e) => setHealthFilter(e.target.value)}
              className="text-sm border border-[var(--oqh-border)] rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)]"
            >
              <option value="">All Health Status</option>
              <option value="HEALTHY">Healthy</option>
              <option value="WARNING">Warning</option>
              <option value="CRITICAL">Critical</option>
              <option value="UNKNOWN">Unknown</option>
            </select>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-[#F7F9FC] text-left text-[var(--oqh-text-muted)]">
                <th className="px-6 py-3 font-medium">Title</th>
                <th className="px-6 py-3 font-medium">Type</th>
                <th className="px-6 py-3 font-medium">Category</th>
                <th className="px-6 py-3 font-medium">Health</th>
                <th className="px-6 py-3 font-medium">Owner</th>
                <th className="px-6 py-3 font-medium">Last Reviewed</th>
                <th className="px-6 py-3 font-medium text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]/50">
              {assets.map((asset) => {
                const health = HEALTH_CONFIG[asset.healthStatus] || HEALTH_CONFIG.UNKNOWN;
                const HealthIcon = health.icon;
                return (
                  <tr key={asset.id} className="hover:bg-[#F7F9FC]">
                    <td className="px-6 py-3">
                      <Link
                        to={`/documentation/${asset.id}`}
                        className="text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] font-medium flex items-center gap-1"
                      >
                        {asset.title}
                        <ChevronRight className="h-3 w-3" />
                      </Link>
                      {asset.projectName && (
                        <p className="text-xs text-[var(--oqh-text-muted)] mt-0.5">{asset.projectName}</p>
                      )}
                    </td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)]">
                      {DOC_TYPE_LABELS[asset.docType] || asset.docType}
                    </td>
                    <td className="px-6 py-3">
                      <span className="inline-block px-2 py-0.5 rounded text-xs font-medium bg-[#F7F9FC] text-[var(--oqh-text)]">
                        {asset.category}
                      </span>
                    </td>
                    <td className="px-6 py-3">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold ${health.bg} ${health.color}`}>
                        <HealthIcon className="h-3 w-3" />
                        {asset.healthStatus}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)]">
                      {asset.ownerName || (
                        <span className="text-orange-600 flex items-center gap-1">
                          <UserX className="h-3 w-3" /> Unassigned
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-3 text-[var(--oqh-text-muted)] text-xs">
                      {asset.lastReviewedAt
                        ? new Date(asset.lastReviewedAt).toLocaleDateString()
                        : <span className="text-amber-600">Never</span>
                      }
                    </td>
                    <td className="px-6 py-3 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => runChecks.mutate(asset.id)}
                          disabled={runChecks.isPending}
                          className="p-1.5 text-[#9BA8B9] hover:text-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-active-bg)] transition-colors"
                          title="Run health checks"
                        >
                          <RefreshCw className={`h-4 w-4 ${runChecks.isPending ? 'animate-spin' : ''}`} />
                        </button>
                        <Link
                          to={`/documentation/${asset.id}`}
                          className="p-1.5 text-[#9BA8B9] hover:text-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-active-bg)] transition-colors"
                          title="View details"
                        >
                          <Eye className="h-4 w-4" />
                        </Link>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {assets.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-[var(--oqh-text-muted)]">
                    No documentation assets found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
