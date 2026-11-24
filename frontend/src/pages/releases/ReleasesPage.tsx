import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
} from 'recharts';
import {
  Rocket, CheckCircle2, AlertTriangle, XCircle, ArrowRight,
  Calendar, ShieldCheck, ShieldX, HelpCircle,
} from 'lucide-react';
import { releasesApi } from '@/api/releases';
import type { ReleaseDashboard, ReleaseReadinessSummary } from '@/types/api';

const GATE_CONFIG: Record<string, { color: string; bg: string; text: string; icon: typeof CheckCircle2 }> = {
  PASS: { color: '#10b981', bg: 'bg-emerald-100', text: 'text-emerald-700', icon: CheckCircle2 },
  WARN: { color: '#f59e0b', bg: 'bg-amber-100', text: 'text-amber-700', icon: AlertTriangle },
  FAIL: { color: '#ef4444', bg: 'bg-red-100', text: 'text-red-700', icon: XCircle },
};

const STATUS_LABELS: Record<string, string> = {
  PLANNING: 'Planning',
  IN_PROGRESS: 'In Progress',
  READY_FOR_QA: 'Ready for QA',
  QA_IN_PROGRESS: 'QA In Progress',
  APPROVED: 'Approved',
  RELEASED: 'Released',
  CANCELLED: 'Cancelled',
};

function ScoreGauge({ score }: { score: number | null }) {
  if (score === null) {
    return (
      <div className="flex items-center justify-center w-16 h-16 rounded-full border-4 border-[var(--oqh-border)]">
        <span className="text-xs text-[#9BA8B9]">N/A</span>
      </div>
    );
  }
  const pct = Math.min(100, Math.max(0, score));
  const color = pct >= 80 ? '#10b981' : pct >= 60 ? '#f59e0b' : '#ef4444';
  const circumference = 2 * Math.PI * 26;
  const offset = circumference - (pct / 100) * circumference;

  return (
    <div className="relative w-16 h-16">
      <svg className="w-16 h-16 -rotate-90" viewBox="0 0 60 60">
        <circle cx="30" cy="30" r="26" fill="none" stroke="#e5e7eb" strokeWidth="5" />
        <circle cx="30" cy="30" r="26" fill="none" stroke={color} strokeWidth="5"
          strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round" />
      </svg>
      <span className="absolute inset-0 flex items-center justify-center text-sm font-bold" style={{ color }}>
        {Math.round(pct)}
      </span>
    </div>
  );
}

function ReleaseCard({ release }: { release: ReleaseReadinessSummary }) {
  const gate = release.gateStatus ? GATE_CONFIG[release.gateStatus] : null;
  const GateIcon = gate?.icon ?? HelpCircle;

  return (
    <Link
      to={`/releases/${release.releaseId}`}
      className="block bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5 group"
    >
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm font-mono font-semibold text-[var(--oqh-text)]">{release.releaseVersion}</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-[#F7F9FC] text-[var(--oqh-text-muted)]">
              {STATUS_LABELS[release.releaseStatus] ?? release.releaseStatus}
            </span>
          </div>
          <p className="text-sm text-[var(--oqh-text-muted)] truncate">{release.releaseName}</p>
          <p className="text-xs text-[#9BA8B9] mt-1">{release.projectName}</p>
        </div>
        <ScoreGauge score={release.overallScore} />
      </div>

      <div className="mt-4 flex items-center justify-between text-xs text-[var(--oqh-text-muted)]">
        <div className="flex items-center gap-3">
          {gate && (
            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full ${gate.bg} ${gate.text} font-medium`}>
              <GateIcon className="h-3 w-3" />
              {release.gateStatus}
            </span>
          )}
          {release.approvedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-emerald-600">
              <ShieldCheck className="h-3.5 w-3.5" /> {release.approvedCount}
            </span>
          )}
          {release.rejectedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-red-600">
              <ShieldX className="h-3.5 w-3.5" /> {release.rejectedCount}
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
          {release.targetDate && (
            <span className="inline-flex items-center gap-1">
              <Calendar className="h-3 w-3" /> {release.targetDate}
            </span>
          )}
          <ArrowRight className="h-3.5 w-3.5 ml-2 opacity-0 group-hover:opacity-100 transition-opacity text-[var(--oqh-blue)]" />
        </div>
      </div>
    </Link>
  );
}

export function ReleasesPage() {
  const [statusFilter, setStatusFilter] = useState<string>('');

  const { data: dashboard, isLoading } = useQuery<ReleaseDashboard>({
    queryKey: ['release-dashboard'],
    queryFn: releasesApi.getDashboard,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load release dashboard.
      </div>
    );
  }

  const { activeReleases, totalActiveReleases, passingReleases, warningReleases, failingReleases } = dashboard;

  const pieData = [
    { name: 'Passing', value: passingReleases, color: '#10b981' },
    { name: 'Warning', value: warningReleases, color: '#f59e0b' },
    { name: 'Failing', value: failingReleases, color: '#ef4444' },
    { name: 'No Report', value: totalActiveReleases - passingReleases - warningReleases - failingReleases, color: '#d1d5db' },
  ].filter((d) => d.value > 0);

  const filtered = statusFilter
    ? activeReleases.filter((r) => r.releaseStatus === statusFilter)
    : activeReleases;

  const statuses = [...new Set(activeReleases.map((r) => r.releaseStatus))];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)] flex items-center gap-2">
            <Rocket className="h-6 w-6 text-[var(--oqh-blue)]" />
            Release Confidence
          </h1>
          <p className="text-sm text-[var(--oqh-text-muted)] mt-1">
            Quality gate status and readiness across active releases
          </p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <p className="text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Active</p>
          <p className="text-2xl font-bold text-[var(--oqh-text)] mt-1">{totalActiveReleases}</p>
        </div>
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <p className="text-xs font-medium text-emerald-600 uppercase tracking-wider">Passing</p>
          <p className="text-2xl font-bold text-emerald-600 mt-1">{passingReleases}</p>
        </div>
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <p className="text-xs font-medium text-amber-600 uppercase tracking-wider">Warning</p>
          <p className="text-2xl font-bold text-amber-600 mt-1">{warningReleases}</p>
        </div>
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <p className="text-xs font-medium text-red-600 uppercase tracking-wider">Failing</p>
          <p className="text-2xl font-bold text-red-600 mt-1">{failingReleases}</p>
        </div>
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 flex items-center justify-center">
          <ResponsiveContainer width={80} height={80}>
            <PieChart>
              <Pie data={pieData} dataKey="value" cx="50%" cy="50%" innerRadius={22} outerRadius={36} strokeWidth={0}>
                {pieData.map((d, i) => (
                  <Cell key={i} fill={d.color} />
                ))}
              </Pie>
              <Tooltip formatter={(v, name) => [`${v}`, `${name}`]} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2">
        <button
          onClick={() => setStatusFilter('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
            !statusFilter ? 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]' : 'bg-[#F7F9FC] text-[var(--oqh-text-muted)] hover:bg-[#F7F9FC]'
          }`}
        >
          All ({activeReleases.length})
        </button>
        {statuses.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s === statusFilter ? '' : s)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              statusFilter === s ? 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]' : 'bg-[#F7F9FC] text-[var(--oqh-text-muted)] hover:bg-[#F7F9FC]'
            }`}
          >
            {STATUS_LABELS[s] ?? s} ({activeReleases.filter((r) => r.releaseStatus === s).length})
          </button>
        ))}
      </div>

      {/* Release cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {filtered.map((release) => (
          <ReleaseCard key={release.releaseId} release={release} />
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="text-center py-12 text-[#9BA8B9]">
          No releases match the current filter.
        </div>
      )}
    </div>
  );
}
