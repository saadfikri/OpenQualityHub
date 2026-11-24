import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft, RefreshCw, CheckCircle2, AlertTriangle, XCircle,
  ShieldCheck, ShieldX, Clock, ChevronDown, ChevronUp, Send,
} from 'lucide-react';
import { releasesApi } from '@/api/releases';
import type { ReadinessReport, SignOff, Page } from '@/types/api';

const GATE_BADGE: Record<string, { bg: string; text: string; icon: typeof CheckCircle2 }> = {
  PASS: { bg: 'bg-emerald-100', text: 'text-emerald-700', icon: CheckCircle2 },
  WARN: { bg: 'bg-amber-100', text: 'text-amber-700', icon: AlertTriangle },
  FAIL: { bg: 'bg-red-100', text: 'text-red-700', icon: XCircle },
};

const OPERATOR_LABELS: Record<string, string> = {
  GTE: '\u2265', GT: '>', LTE: '\u2264', LT: '<', EQ: '=',
};

const METRIC_LABELS: Record<string, string> = {
  pass_rate: 'Pass Rate',
  critical_defects: 'Critical Defects',
  high_defects: 'High Defects',
  open_defects: 'Open Defects',
  build_success_rate: 'Build Success Rate',
  total_test_runs: 'Total Test Runs',
};

function ScoreRing({ score, size = 120 }: { score: number; size?: number }) {
  const r = (size - 12) / 2;
  const circumference = 2 * Math.PI * r;
  const pct = Math.min(100, Math.max(0, score));
  const offset = circumference - (pct / 100) * circumference;
  const color = pct >= 80 ? '#10b981' : pct >= 60 ? '#f59e0b' : '#ef4444';

  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg className="-rotate-90" width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#e5e7eb" strokeWidth="8" />
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth="8"
          strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round" />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-3xl font-bold" style={{ color }}>{Math.round(pct)}</span>
        <span className="text-xs text-[#9BA8B9]">/ 100</span>
      </div>
    </div>
  );
}

export function ReleaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [showHistory, setShowHistory] = useState(false);
  const [signOffForm, setSignOffForm] = useState({ decision: '', comment: '' });

  const { data: report, isLoading: loadingReport } = useQuery<ReadinessReport>({
    queryKey: ['readiness', id],
    queryFn: () => releasesApi.getLatestReadiness(id!),
    enabled: !!id,
  });

  const { data: signOffs, isLoading: loadingSignOffs } = useQuery<SignOff[]>({
    queryKey: ['sign-offs', id],
    queryFn: () => releasesApi.getSignOffs(id!),
    enabled: !!id,
  });

  const { data: history } = useQuery<Page<ReadinessReport>>({
    queryKey: ['readiness-history', id],
    queryFn: () => releasesApi.getReadinessHistory(id!, { size: '10' }),
    enabled: !!id && showHistory,
  });

  const generateReport = useMutation({
    mutationFn: () => releasesApi.generateReadiness(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['readiness', id] });
      queryClient.invalidateQueries({ queryKey: ['readiness-history', id] });
      queryClient.invalidateQueries({ queryKey: ['release-dashboard'] });
    },
  });

  const submitSignOff = useMutation({
    mutationFn: (data: { decision: string; comment?: string }) =>
      releasesApi.signOff(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sign-offs', id] });
      queryClient.invalidateQueries({ queryKey: ['release-dashboard'] });
      setSignOffForm({ decision: '', comment: '' });
    },
  });

  if (loadingReport || loadingSignOffs) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (!report) {
    return (
      <div className="space-y-4">
        <Link to="/releases" className="inline-flex items-center gap-1 text-sm text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)]">
          <ArrowLeft className="h-4 w-4" /> Back to Releases
        </Link>
        <div className="bg-[#F7F9FC] border border-[var(--oqh-border)] rounded-xl p-8 text-center">
          <p className="text-[var(--oqh-text-muted)] mb-4">No readiness report generated yet for this release.</p>
          <button
            onClick={() => generateReport.mutate()}
            disabled={generateReport.isPending}
            className="inline-flex items-center gap-2 px-4 py-2 bg-[var(--oqh-blue)] text-white rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${generateReport.isPending ? 'animate-spin' : ''}`} />
            Generate Report
          </button>
        </div>
      </div>
    );
  }

  const gateBadge = GATE_BADGE[report.gateStatus];
  const GateIcon = gateBadge?.icon ?? AlertTriangle;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <Link to="/releases" className="inline-flex items-center gap-1 text-sm text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)] mb-2">
            <ArrowLeft className="h-4 w-4" /> Back to Releases
          </Link>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">
            {report.releaseVersion} {report.releaseName && `\u2014 ${report.releaseName}`}
          </h1>
        </div>
        <button
          onClick={() => generateReport.mutate()}
          disabled={generateReport.isPending}
          className="inline-flex items-center gap-2 px-4 py-2 bg-[var(--oqh-blue)] text-white rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 text-sm"
        >
          <RefreshCw className={`h-4 w-4 ${generateReport.isPending ? 'animate-spin' : ''}`} />
          Re-evaluate
        </button>
      </div>

      {/* Score + Gate Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6 flex flex-col items-center transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <ScoreRing score={report.overallScore} />
          <div className="mt-3 text-center">
            <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium ${gateBadge?.bg} ${gateBadge?.text}`}>
              <GateIcon className="h-4 w-4" />
              Gate: {report.gateStatus}
            </span>
          </div>
          <p className="text-xs text-[#9BA8B9] mt-2">
            {report.generatedAt && `Evaluated ${new Date(report.generatedAt).toLocaleString()}`}
          </p>
          {report.generatedByName && (
            <p className="text-xs text-[#9BA8B9]">by {report.generatedByName}</p>
          )}
        </div>

        {/* Metrics */}
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6 lg:col-span-2 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
          <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Metrics Snapshot</h2>
          {report.metrics ? (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <MetricCard label="Pass Rate" value={report.metrics.passRate != null ? `${report.metrics.passRate}%` : 'N/A'} />
              <MetricCard label="Test Runs" value={String(report.metrics.totalTestRuns)} />
              <MetricCard label="Open Defects" value={String(report.metrics.openDefects)}
                alert={report.metrics.openDefects > 0} />
              <MetricCard label="Critical Defects" value={String(report.metrics.criticalDefects)}
                alert={report.metrics.criticalDefects > 0} />
              <MetricCard label="High Defects" value={String(report.metrics.highDefects)}
                alert={report.metrics.highDefects > 0} />
              <MetricCard label="Build Success" value={report.metrics.buildSuccessRate != null ? `${report.metrics.buildSuccessRate}%` : 'N/A'} />
              <MetricCard label="Total Builds" value={String(report.metrics.totalBuilds)} />
              <MetricCard label="Successful Builds" value={String(report.metrics.successfulBuilds)} />
            </div>
          ) : (
            <p className="text-sm text-[#9BA8B9]">No metrics available.</p>
          )}
        </div>
      </div>

      {/* Gate Results */}
      <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
        <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Quality Gate Conditions</h2>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-[var(--oqh-text-muted)] uppercase tracking-wider border-b border-[var(--oqh-border)]/50">
                <th className="pb-2 pr-4">Metric</th>
                <th className="pb-2 pr-4">Condition</th>
                <th className="pb-2 pr-4">Actual</th>
                <th className="pb-2 pr-4">Status</th>
                <th className="pb-2">Critical</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]/50">
              {report.gateResults.map((gr) => {
                const passed = gr.status === 'PASS';
                return (
                  <tr key={gr.id}>
                    <td className="py-2.5 pr-4 font-medium text-[var(--oqh-text)]">
                      {METRIC_LABELS[gr.metricKey] ?? gr.metricKey}
                    </td>
                    <td className="py-2.5 pr-4 text-[var(--oqh-text-muted)] font-mono text-xs">
                      {OPERATOR_LABELS[gr.operator] ?? gr.operator} {gr.expectedValue}
                    </td>
                    <td className="py-2.5 pr-4 font-mono text-xs">
                      <span className={passed ? 'text-emerald-600' : 'text-red-600'}>{gr.actualValue}</span>
                    </td>
                    <td className="py-2.5 pr-4">
                      <span className={`inline-flex items-center gap-1 text-xs font-medium ${
                        passed ? 'text-emerald-600' : 'text-red-600'
                      }`}>
                        {passed ? <CheckCircle2 className="h-3.5 w-3.5" /> : <XCircle className="h-3.5 w-3.5" />}
                        {gr.status}
                      </span>
                    </td>
                    <td className="py-2.5">
                      {gr.critical && <span className="text-xs px-1.5 py-0.5 rounded bg-red-50 text-red-600 font-medium">Critical</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Sign-offs + Sign-off form */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Existing sign-offs */}
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
          <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Sign-Off Chain</h2>
          {signOffs && signOffs.length > 0 ? (
            <div className="space-y-3">
              {signOffs.map((so) => (
                <div key={so.id} className="flex items-start gap-3 p-3 rounded-lg bg-[#F7F9FC]">
                  {so.decision === 'APPROVED' ? (
                    <ShieldCheck className="h-5 w-5 text-emerald-500 mt-0.5" />
                  ) : (
                    <ShieldX className="h-5 w-5 text-red-500 mt-0.5" />
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-[var(--oqh-text)]">{so.userName}</span>
                      <span className="text-xs px-1.5 py-0.5 rounded bg-[#F7F9FC] text-[var(--oqh-text-muted)]">{so.role}</span>
                    </div>
                    <p className={`text-xs font-medium mt-0.5 ${
                      so.decision === 'APPROVED' ? 'text-emerald-600' : 'text-red-600'
                    }`}>
                      {so.decision}
                    </p>
                    {so.comment && <p className="text-xs text-[var(--oqh-text-muted)] mt-1">{so.comment}</p>}
                    <p className="text-xs text-[#9BA8B9] mt-1 flex items-center gap-1">
                      <Clock className="h-3 w-3" /> {new Date(so.decidedAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-[#9BA8B9]">No sign-offs yet.</p>
          )}
        </div>

        {/* Sign-off form */}
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
          <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Submit Sign-Off</h2>
          <div className="space-y-4">
            <div className="flex gap-3">
              <button
                onClick={() => setSignOffForm((f) => ({ ...f, decision: 'APPROVED' }))}
                className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
                  signOffForm.decision === 'APPROVED'
                    ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                    : 'border-[var(--oqh-border)] text-[var(--oqh-text-muted)] hover:border-emerald-300'
                }`}
              >
                <ShieldCheck className="h-4 w-4" /> Approve
              </button>
              <button
                onClick={() => setSignOffForm((f) => ({ ...f, decision: 'REJECTED' }))}
                className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
                  signOffForm.decision === 'REJECTED'
                    ? 'border-red-500 bg-red-50 text-red-700'
                    : 'border-[var(--oqh-border)] text-[var(--oqh-text-muted)] hover:border-red-300'
                }`}
              >
                <ShieldX className="h-4 w-4" /> Reject
              </button>
            </div>
            <textarea
              value={signOffForm.comment}
              onChange={(e) => setSignOffForm((f) => ({ ...f, comment: e.target.value }))}
              placeholder="Optional comment..."
              rows={3}
              className="w-full rounded-lg border border-[var(--oqh-border)] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-transparent resize-none"
            />
            <button
              onClick={() => {
                if (!signOffForm.decision) return;
                submitSignOff.mutate({
                  decision: signOffForm.decision,
                  comment: signOffForm.comment || undefined,
                });
              }}
              disabled={!signOffForm.decision || submitSignOff.isPending}
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-[var(--oqh-blue)] text-white rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium"
            >
              <Send className="h-4 w-4" />
              {submitSignOff.isPending ? 'Submitting...' : 'Submit Sign-Off'}
            </button>
          </div>
        </div>
      </div>

      {/* History toggle */}
      <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
        <button
          onClick={() => setShowHistory(!showHistory)}
          className="flex items-center gap-2 text-sm font-semibold text-[var(--oqh-text)] hover:text-[var(--oqh-blue)] transition-colors"
        >
          Report History
          {showHistory ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </button>
        {showHistory && history && (
          <div className="mt-4 space-y-2">
            {history.content.map((h) => (
              <div key={h.id} className="flex items-center justify-between p-3 rounded-lg bg-[#F7F9FC] text-sm">
                <div className="flex items-center gap-3">
                  <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
                    GATE_BADGE[h.gateStatus]?.bg} ${GATE_BADGE[h.gateStatus]?.text}`}>
                    {h.gateStatus}
                  </span>
                  <span className="font-mono text-[var(--oqh-text)]">Score: {h.overallScore}</span>
                </div>
                <span className="text-xs text-[#9BA8B9]">{new Date(h.generatedAt).toLocaleString()}</span>
              </div>
            ))}
            {history.content.length === 0 && (
              <p className="text-sm text-[#9BA8B9]">No history available.</p>
            )}
          </div>
        )}
      </div>

      {/* Summary */}
      <div className="bg-[#F7F9FC] border border-[var(--oqh-border)] rounded-xl p-4">
        <p className="text-sm text-[var(--oqh-text-muted)]">{report.summary}</p>
      </div>
    </div>
  );
}

function MetricCard({ label, value, alert }: { label: string; value: string; alert?: boolean }) {
  return (
    <div className="p-3 rounded-lg bg-[#F7F9FC]">
      <p className="text-xs text-[var(--oqh-text-muted)]">{label}</p>
      <p className={`text-lg font-bold mt-0.5 ${alert ? 'text-red-600' : 'text-[var(--oqh-text)]'}`}>{value}</p>
    </div>
  );
}
