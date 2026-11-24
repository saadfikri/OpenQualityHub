import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api/dashboard';
import { trendsApi } from '@/api/trends';
import {
  Users, FolderKanban, TestTube2, TrendingUp, Bug, ShieldCheck, BarChart3, Activity,
} from 'lucide-react';
import {
  LineChart, Line, BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';
import { Badge } from '@/components/ui/Badge';

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#f59e0b', LOW: '#6b7280',
};
const STATUS_COLORS: Record<string, string> = {
  OPEN: '#ef4444', IN_PROGRESS: '#3b82f6', RESOLVED: '#10b981', CLOSED: '#6b7280', REOPENED: '#f59e0b',
};

const PERIOD_OPTIONS = [
  { label: '7d', value: 7 },
  { label: '30d', value: 30 },
  { label: '90d', value: 90 },
] as const;

const formatDate = (v: string) =>
  new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' });

export function DashboardPage() {
  const [trendDays, setTrendDays] = useState(30);

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.get,
  });

  const { data: passRateTrend } = useQuery({
    queryKey: ['trends', 'pass-rate', trendDays],
    queryFn: () => trendsApi.passRate(trendDays),
  });

  const { data: defectTrend } = useQuery({
    queryKey: ['trends', 'defects', trendDays],
    queryFn: () => trendsApi.defects(trendDays),
  });

  const { data: volumeTrend } = useQuery({
    queryKey: ['trends', 'test-volume', trendDays],
    queryFn: () => trendsApi.testVolume(trendDays),
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
        Failed to load dashboard data.
      </div>
    );
  }

  const stats = [
    { name: 'Teams', value: data.summary.totalTeams, icon: Users, color: 'bg-[var(--oqh-blue)]' },
    { name: 'Projects', value: data.summary.totalProjects, icon: FolderKanban, color: 'bg-emerald-500' },
    { name: 'Test Runs', value: data.summary.totalTestRuns, icon: TestTube2, color: 'bg-violet-500' },
    { name: 'Avg Pass Rate', value: `${data.summary.averagePassRate.toFixed(1)}%`, icon: TrendingUp, color: 'bg-amber-500' },
    { name: 'Open Defects', value: data.summary.openDefects, icon: Bug, color: 'bg-red-500' },
    { name: 'Flaky Tests', value: data.summary.flakyTestCount, icon: Activity, color: 'bg-orange-500' },
    { name: 'Quality Score', value: data.summary.averageQualityScore.toFixed(1), icon: ShieldCheck, color: 'bg-teal-500' },
  ];

  const defectsBySeverity = data.defectsBySeverity.filter((d) => d.count > 0);
  const defectsByStatus = data.defectsByStatus.filter((d) => d.count > 0);

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Quality Command Center</h1>
        <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
          Real-time quality engineering metrics across all projects
        </p>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 gap-4 mb-8">
        {stats.map((stat) => (
          <div key={stat.name} className="bg-white rounded-xl border border-[var(--oqh-border)] p-5 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
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

      {/* Trend Analytics */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Trend Analytics</h2>
            <p className="text-sm text-[var(--oqh-text-muted)]">Quality metrics over time</p>
          </div>
          <div className="flex items-center gap-1 bg-[#F7F9FC] rounded-lg p-1">
            {PERIOD_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setTrendDays(opt.value)}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-all ${
                  trendDays === opt.value
                    ? 'bg-white text-[var(--oqh-text)] shadow-[var(--oqh-shadow)]'
                    : 'text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)]'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Pass Rate Trend (Area) */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30">
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp className="h-5 w-5 text-green-500" />
              <h3 className="text-base font-semibold text-[var(--oqh-text)]">Pass Rate Trend</h3>
            </div>
            {passRateTrend && passRateTrend.dataPoints.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={passRateTrend.dataPoints}>
                  <defs>
                    <linearGradient id="passRateGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} tickFormatter={formatDate} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} />
                  <Tooltip
                    labelFormatter={(v) => new Date(v as string).toLocaleDateString()}
                    formatter={(value) => [`${Number(value).toFixed(1)}%`, 'Pass Rate']}
                  />
                  <Area
                    type="monotone"
                    dataKey="passRate"
                    stroke="#10b981"
                    strokeWidth={2}
                    fill="url(#passRateGradient)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-[#9BA8B9] text-center py-16">No data for this period</p>
            )}
          </div>

          {/* Defect Trend (Stacked Bar) */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30">
            <div className="flex items-center gap-2 mb-4">
              <Bug className="h-5 w-5 text-red-500" />
              <h3 className="text-base font-semibold text-[var(--oqh-text)]">Defect Trend</h3>
            </div>
            {defectTrend && defectTrend.dataPoints.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={defectTrend.dataPoints}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} tickFormatter={formatDate} />
                  <YAxis tick={{ fontSize: 10 }} allowDecimals={false} />
                  <Tooltip labelFormatter={(v) => new Date(v as string).toLocaleDateString()} />
                  <Legend />
                  <Bar dataKey="opened" stackId="a" fill="#ef4444" name="Opened" />
                  <Bar dataKey="resolved" stackId="a" fill="#10b981" name="Resolved" radius={[2, 2, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-[#9BA8B9] text-center py-16">No data for this period</p>
            )}
          </div>

          {/* Test Volume (Line) */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30">
            <div className="flex items-center gap-2 mb-4">
              <BarChart3 className="h-5 w-5 text-[var(--oqh-blue)]" />
              <h3 className="text-base font-semibold text-[var(--oqh-text)]">Test Volume</h3>
            </div>
            {volumeTrend && volumeTrend.dataPoints.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <LineChart data={volumeTrend.dataPoints}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} tickFormatter={formatDate} />
                  <YAxis tick={{ fontSize: 10 }} allowDecimals={false} />
                  <Tooltip
                    labelFormatter={(v) => new Date(v as string).toLocaleDateString()}
                    formatter={(value) => [Number(value), 'Tests']}
                  />
                  <Line
                    type="monotone"
                    dataKey="count"
                    stroke="#3b82f6"
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 4 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-[#9BA8B9] text-center py-16">No data for this period</p>
            )}
          </div>
        </div>
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Pass Rate Trend */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <div className="flex items-center gap-2 mb-4">
            <Activity className="h-5 w-5 text-[#9BA8B9]" />
            <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Pass Rate Trend (30 days)</h2>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={data.passRateTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11 }}
                tickFormatter={(v) => new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
              />
              <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
              <Tooltip
                labelFormatter={(v) => new Date(v as string).toLocaleDateString()}
                formatter={(value) => [`${Number(value).toFixed(1)}%`, 'Pass Rate']}
              />
              <Line
                type="monotone"
                dataKey="passRate"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Test Volume Trend */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="h-5 w-5 text-[#9BA8B9]" />
            <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Test Volume (30 days)</h2>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data.passRateTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11 }}
                tickFormatter={(v) => new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
              />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip
                labelFormatter={(v) => new Date(v as string).toLocaleDateString()}
              />
              <Bar dataKey="passed" stackId="a" fill="#10b981" name="Passed" />
              <Bar dataKey="failed" stackId="a" fill="#ef4444" name="Failed" radius={[2, 2, 0, 0]} />
              <Legend />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Defects row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Defects by Severity */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Defects by Severity</h2>
          {defectsBySeverity.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No defects found</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={defectsBySeverity}
                  dataKey="count"
                  nameKey="label"
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  label={({ name, value }) => `${name}: ${value}`}
                >
                  {defectsBySeverity.map((entry) => (
                    <Cell key={entry.label} fill={SEVERITY_COLORS[entry.label] || '#6b7280'} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Defects by Status */}
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Defects by Status</h2>
          {defectsByStatus.length === 0 ? (
            <p className="text-sm text-[var(--oqh-text-muted)] text-center py-8">No defects found</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={defectsByStatus} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis dataKey="label" type="category" tick={{ fontSize: 11 }} width={100} />
                <Tooltip />
                <Bar dataKey="count" name="Defects" radius={[0, 4, 4, 0]}>
                  {defectsByStatus.map((entry) => (
                    <Cell key={entry.label} fill={STATUS_COLORS[entry.label] || '#6b7280'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Project Health Table */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] mb-8 shadow-[var(--oqh-shadow)]">
        <div className="p-6 border-b border-[var(--oqh-border)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Project Health</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-[#F7F9FC] text-left text-[var(--oqh-text-muted)]">
                <th className="px-6 py-3 font-medium">Project</th>
                <th className="px-6 py-3 font-medium">Team</th>
                <th className="px-6 py-3 font-medium text-right">Pass Rate</th>
                <th className="px-6 py-3 font-medium text-right">Quality Score</th>
                <th className="px-6 py-3 font-medium text-right">Open Defects</th>
                <th className="px-6 py-3 font-medium text-right">Test Runs</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]/50">
              {data.projectHealth.map((project) => (
                <tr key={project.projectId} className="hover:bg-[#F7F9FC]">
                  <td className="px-6 py-3 font-medium text-[var(--oqh-text)]">{project.projectName}</td>
                  <td className="px-6 py-3 text-[var(--oqh-text-muted)]">{project.teamName}</td>
                  <td className="px-6 py-3 text-right">
                    {project.passRate != null ? (
                      <span className={project.passRate >= 85 ? 'text-green-600' : project.passRate >= 70 ? 'text-amber-600' : 'text-red-600'}>
                        {project.passRate.toFixed(1)}%
                      </span>
                    ) : (
                      <span className="text-[#9BA8B9]">N/A</span>
                    )}
                  </td>
                  <td className="px-6 py-3 text-right">
                    {project.qualityScore != null ? (
                      <span className="font-medium">{project.qualityScore.toFixed(1)}</span>
                    ) : (
                      <span className="text-[#9BA8B9]">N/A</span>
                    )}
                  </td>
                  <td className="px-6 py-3 text-right">
                    {project.openDefects > 0 ? (
                      <Badge variant={project.openDefects > 3 ? 'danger' : 'warning'}>{project.openDefects}</Badge>
                    ) : (
                      <Badge variant="success">0</Badge>
                    )}
                  </td>
                  <td className="px-6 py-3 text-right text-[var(--oqh-text-muted)]">{project.totalTestRuns}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Test Runs */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)]">
        <div className="p-6 border-b border-[var(--oqh-border)]">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Recent Test Runs</h2>
        </div>
        <div className="divide-y divide-[var(--oqh-border)]/50">
          {data.recentTestRuns.map((run) => (
            <div key={run.id} className="px-6 py-4 flex items-center justify-between">
              <div>
                <p className="font-medium text-[var(--oqh-text)]">{run.name}</p>
                <p className="text-sm text-[var(--oqh-text-muted)]">
                  {run.projectName} &middot; {run.source} &middot;{' '}
                  {new Date(run.executedAt).toLocaleDateString()}
                </p>
              </div>
              <div className="flex items-center gap-4 text-sm">
                <span className="text-green-600 font-medium">{run.passed} passed</span>
                {run.failed > 0 && <span className="text-red-600 font-medium">{run.failed} failed</span>}
                {run.skipped > 0 && <span className="text-[#9BA8B9]">{run.skipped} skipped</span>}
                <span className={`font-bold ${run.passRate >= 85 ? 'text-green-600' : run.passRate >= 70 ? 'text-amber-600' : 'text-red-600'}`}>
                  {run.passRate.toFixed(1)}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
