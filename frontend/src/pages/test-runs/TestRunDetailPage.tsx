import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { testRunsApi } from '@/api/test-runs';
import { failureAnalysisApi, type FailureAnalysisData } from '@/api/failure-analysis';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import {
  ArrowLeft, Brain, CheckCircle2, XCircle, AlertTriangle,
  ThumbsUp, ThumbsDown, Loader2, Sparkles,
} from 'lucide-react';
import type { TestCase } from '@/types/api';

const statusBadge: Record<TestCase['status'], 'success' | 'danger' | 'warning' | 'default'> = {
  PASSED: 'success',
  FAILED: 'danger',
  ERROR: 'danger',
  SKIPPED: 'warning',
};

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

const CATEGORY_CONFIG: Record<string, { label: string; color: string; icon: typeof AlertTriangle }> = {
  CODE_REGRESSION: { label: 'Code Regression', color: 'bg-red-50 text-red-700 border-red-200', icon: XCircle },
  FLAKY_TEST: { label: 'Flaky Test', color: 'bg-amber-50 text-amber-700 border-amber-200', icon: AlertTriangle },
  ENVIRONMENT_ISSUE: { label: 'Environment Issue', color: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)] border-[var(--oqh-blue)]/20', icon: AlertTriangle },
  DATA_ISSUE: { label: 'Data Issue', color: 'bg-purple-50 text-purple-700 border-purple-200', icon: AlertTriangle },
  CONFIGURATION_ERROR: { label: 'Configuration Error', color: 'bg-orange-50 text-orange-700 border-orange-200', icon: AlertTriangle },
  DEPENDENCY_FAILURE: { label: 'Dependency Failure', color: 'bg-indigo-50 text-indigo-700 border-indigo-200', icon: AlertTriangle },
  TIMEOUT: { label: 'Timeout', color: 'bg-[#F7F9FC] text-[var(--oqh-text)] border-[var(--oqh-border)]', icon: AlertTriangle },
  ASSERTION_MISMATCH: { label: 'Assertion Mismatch', color: 'bg-pink-50 text-pink-700 border-pink-200', icon: XCircle },
  UNKNOWN: { label: 'Unknown', color: 'bg-[#F7F9FC] text-[var(--oqh-text-muted)] border-[var(--oqh-border)]', icon: AlertTriangle },
};

export function TestRunDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: run, isLoading } = useQuery({
    queryKey: ['test-run', id],
    queryFn: () => testRunsApi.getById(id!),
    enabled: !!id,
  });

  function formatDuration(ms: number | null) {
    if (ms === null) return '-';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  }

  if (isLoading) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  if (!run) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Test run not found</div>;
  }

  return (
    <div>
      <Button variant="ghost" onClick={() => navigate('/test-runs')} className="mb-4">
        <ArrowLeft className="h-4 w-4" />
        Back to Test Runs
      </Button>

      <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 mb-6 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[var(--oqh-text)]">{run.name}</h1>
            <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
              {run.projectName} &middot; <Badge variant={sourceBadgeVariant(run.source)}>{run.source}</Badge>
              {run.environment && <> &middot; {run.environment}</>}
              {run.branch && <> &middot; <span className="font-mono text-xs bg-[#F7F9FC] px-1.5 py-0.5 rounded">{run.branch}</span></>}
              {run.commitSha && <> &middot; <span className="font-mono text-xs text-[#9BA8B9]">{run.commitSha.substring(0, 8)}</span></>}
            </p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-bold" style={{ color: run.passRate >= 90 ? '#16a34a' : run.passRate >= 70 ? '#d97706' : '#dc2626' }}>
              {run.passRate}%
            </p>
            <p className="text-sm text-[var(--oqh-text-muted)]">pass rate</p>
          </div>
        </div>

        <div className="grid grid-cols-4 gap-4 mt-6 pt-6 border-t border-[var(--oqh-border)]">
          <div className="text-center">
            <p className="text-2xl font-bold text-[var(--oqh-text)]">{run.totalTests}</p>
            <p className="text-sm text-[var(--oqh-text-muted)]">Total</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-green-600">{run.passed}</p>
            <p className="text-sm text-[var(--oqh-text-muted)]">Passed</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-red-600">{run.failed}</p>
            <p className="text-sm text-[var(--oqh-text-muted)]">Failed</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-[var(--oqh-text-muted)]">{run.skipped}</p>
            <p className="text-sm text-[var(--oqh-text-muted)]">Skipped</p>
          </div>
        </div>
      </div>

      {/* AI Failure Analysis Section */}
      {run.failed > 0 && (
        <FailureAnalysisSection testRunId={id!} />
      )}

      <div className="bg-white rounded-xl border border-[var(--oqh-border)] overflow-hidden shadow-[var(--oqh-shadow)]">
        <div className="p-4 border-b border-[var(--oqh-border)]">
          <h2 className="font-semibold text-[var(--oqh-text)]">Test Cases ({run.testCases.length})</h2>
        </div>
        <table className="min-w-full divide-y divide-[var(--oqh-border)]">
          <thead className="bg-[#F7F9FC]">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Test</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Suite</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Duration</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--oqh-border)]">
            {run.testCases.map((tc) => (
              <tr key={tc.id} className="hover:bg-[#F7F9FC]">
                <td className="px-6 py-4">
                  <div className="font-medium text-[var(--oqh-text)] text-sm">{tc.name}</div>
                  {tc.className && <div className="text-xs text-[var(--oqh-text-muted)]">{tc.className}</div>}
                  {tc.errorMessage && (
                    <div className="mt-1 text-xs text-red-600 font-mono bg-red-50 p-2 rounded max-w-lg truncate">
                      {tc.errorMessage}
                    </div>
                  )}
                </td>
                <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{tc.suiteName ?? '-'}</td>
                <td className="px-6 py-4">
                  <Badge variant={statusBadge[tc.status]}>{tc.status}</Badge>
                </td>
                <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{formatDuration(tc.durationMs)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function FailureAnalysisSection({ testRunId }: { testRunId: string }) {
  const queryClient = useQueryClient();
  const [feedbackComment, setFeedbackComment] = useState('');

  const { data: analysis, isLoading, error } = useQuery<FailureAnalysisData>({
    queryKey: ['failure-analysis', testRunId],
    queryFn: () => failureAnalysisApi.getByTestRun(testRunId),
    retry: false,
    // Poll every 2s while AI analysis is in progress
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return (status === 'ANALYZING' || status === 'PENDING') ? 2000 : false;
    },
  });

  const { data: aiStatus } = useQuery({
    queryKey: ['failure-analysis-status'],
    queryFn: failureAnalysisApi.getStatus,
  });

  const triggerMutation = useMutation({
    mutationFn: () => failureAnalysisApi.trigger(testRunId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['failure-analysis', testRunId] });
    },
  });

  const feedbackMutation = useMutation({
    mutationFn: ({ feedback }: { feedback: string }) =>
      failureAnalysisApi.submitFeedback(analysis!.id, feedback, feedbackComment || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['failure-analysis', testRunId] });
      setFeedbackComment('');
    },
  });

  // No analysis yet
  if (error || (!isLoading && !analysis)) {
    return (
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 mb-6 shadow-[var(--oqh-shadow)]">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-purple-50 text-purple-700">
              <Brain className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-[var(--oqh-text)]">AI Failure Analysis</h3>
              <p className="text-xs text-[var(--oqh-text-muted)]">
                {aiStatus?.available
                  ? 'Analyze test failures with AI to identify root causes'
                  : 'AI analysis unavailable (ANTHROPIC_API_KEY not configured)'}
              </p>
            </div>
          </div>
          <button
            onClick={() => triggerMutation.mutate()}
            disabled={triggerMutation.isPending}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors"
          >
            {triggerMutation.isPending ? (
              <><Loader2 className="h-4 w-4 animate-spin" /> Analyzing...</>
            ) : (
              <><Sparkles className="h-4 w-4" /> Analyze Failures</>
            )}
          </button>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 mb-6 shadow-[var(--oqh-shadow)]">
        <div className="flex items-center gap-2 text-[var(--oqh-text-muted)]">
          <Loader2 className="h-4 w-4 animate-spin" /> Loading analysis...
        </div>
      </div>
    );
  }

  if (!analysis) return null;

  // Show spinner while AI is processing asynchronously
  if (analysis.status === 'ANALYZING' || analysis.status === 'PENDING') {
    return (
      <div className="bg-white rounded-xl border border-purple-200 p-6 mb-6 shadow-[var(--oqh-shadow)]">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-purple-50 text-purple-700">
            <Brain className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h3 className="text-sm font-semibold text-[var(--oqh-text)]">AI Failure Analysis</h3>
            <div className="flex items-center gap-2 mt-1 text-sm text-purple-600">
              <Loader2 className="h-4 w-4 animate-spin" />
              AI is analyzing test failures... This typically takes 5-15 seconds.
            </div>
          </div>
        </div>
      </div>
    );
  }

  const categoryConfig = analysis.failureCategory ? CATEGORY_CONFIG[analysis.failureCategory] : null;
  const CategoryIcon = categoryConfig?.icon || AlertTriangle;

  return (
    <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 mb-6 shadow-[var(--oqh-shadow)]">
      <div className="flex items-center gap-3 mb-4">
        <div className="p-2.5 rounded-lg bg-purple-50 text-purple-700">
          <Brain className="h-5 w-5" />
        </div>
        <div className="flex-1">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)]">AI Failure Analysis</h3>
          <div className="flex items-center gap-2 mt-0.5">
            {analysis.status === 'COMPLETED' && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-emerald-50 text-emerald-700">
                COMPLETED
              </span>
            )}
            {analysis.status === 'FAILED' && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-red-50 text-red-700">
                ANALYSIS FAILED
              </span>
            )}
            {analysis.status === 'SKIPPED' && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-[#F7F9FC] text-[var(--oqh-text-muted)]">
                SKIPPED (no API key)
              </span>
            )}
            {analysis.status === 'ANALYZING' && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)] flex items-center gap-1">
                <Loader2 className="h-3 w-3 animate-spin" /> ANALYZING
              </span>
            )}
            {analysis.aiModel && (
              <span className="text-[10px] text-[#9BA8B9]">{analysis.aiModel}</span>
            )}
            {analysis.confidenceScore != null && (
              <span className="text-[10px] text-[#9BA8B9]">
                Confidence: {(analysis.confidenceScore * 100).toFixed(0)}%
              </span>
            )}
          </div>
        </div>
        {/* Feedback status */}
        {analysis.humanFeedback !== 'PENDING' && (
          <div className={`flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${
            analysis.humanFeedback === 'ACCEPTED' ? 'bg-emerald-50 text-emerald-700' :
            analysis.humanFeedback === 'REJECTED' ? 'bg-red-50 text-red-700' :
            'bg-amber-50 text-amber-700'
          }`}>
            {analysis.humanFeedback === 'ACCEPTED' && <CheckCircle2 className="h-3.5 w-3.5" />}
            {analysis.humanFeedback === 'REJECTED' && <XCircle className="h-3.5 w-3.5" />}
            {analysis.humanFeedback}
          </div>
        )}
      </div>

      {analysis.status === 'COMPLETED' && (
        <div className="space-y-4">
          {/* Category badge */}
          {categoryConfig && (
            <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border ${categoryConfig.color}`}>
              <CategoryIcon className="h-4 w-4" />
              <span className="text-sm font-medium">{categoryConfig.label}</span>
            </div>
          )}

          {/* Summary */}
          {analysis.summary && (
            <div>
              <h4 className="text-xs font-semibold text-[var(--oqh-text-muted)] uppercase mb-1">Summary</h4>
              <p className="text-sm text-[var(--oqh-text)]">{analysis.summary}</p>
            </div>
          )}

          {/* Root cause */}
          {analysis.rootCause && (
            <div>
              <h4 className="text-xs font-semibold text-[var(--oqh-text-muted)] uppercase mb-1">Root Cause</h4>
              <div className="text-sm text-[var(--oqh-text)] bg-[#F7F9FC] p-3 rounded-lg whitespace-pre-wrap">
                {analysis.rootCause}
              </div>
            </div>
          )}

          {/* Suggested fix */}
          {analysis.suggestedFix && (
            <div>
              <h4 className="text-xs font-semibold text-[var(--oqh-text-muted)] uppercase mb-1">Suggested Fix</h4>
              <div className="text-sm text-[var(--oqh-text)] bg-[var(--oqh-active-bg)] p-3 rounded-lg whitespace-pre-wrap">
                {analysis.suggestedFix}
              </div>
            </div>
          )}

          {/* Feedback section */}
          {analysis.humanFeedback === 'PENDING' && (
            <div className="border-t border-[var(--oqh-border)]/50 pt-4">
              <h4 className="text-xs font-semibold text-[var(--oqh-text-muted)] uppercase mb-2">Your Feedback</h4>
              <div className="flex items-center gap-3">
                <input
                  type="text"
                  value={feedbackComment}
                  onChange={(e) => setFeedbackComment(e.target.value)}
                  placeholder="Optional comment..."
                  className="flex-1 border border-[var(--oqh-border)] rounded-lg px-3 py-1.5 text-sm"
                />
                <button
                  onClick={() => feedbackMutation.mutate({ feedback: 'ACCEPTED' })}
                  disabled={feedbackMutation.isPending}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-emerald-700 bg-emerald-50 rounded-lg hover:bg-emerald-100 transition-colors"
                >
                  <ThumbsUp className="h-4 w-4" /> Accept
                </button>
                <button
                  onClick={() => feedbackMutation.mutate({ feedback: 'PARTIALLY_ACCEPTED' })}
                  disabled={feedbackMutation.isPending}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-amber-700 bg-amber-50 rounded-lg hover:bg-amber-100 transition-colors"
                >
                  Partial
                </button>
                <button
                  onClick={() => feedbackMutation.mutate({ feedback: 'REJECTED' })}
                  disabled={feedbackMutation.isPending}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-700 bg-red-50 rounded-lg hover:bg-red-100 transition-colors"
                >
                  <ThumbsDown className="h-4 w-4" /> Reject
                </button>
              </div>
            </div>
          )}

          {/* Show existing feedback */}
          {analysis.humanFeedback !== 'PENDING' && analysis.feedbackComment && (
            <div className="border-t border-[var(--oqh-border)]/50 pt-3">
              <p className="text-xs text-[var(--oqh-text-muted)]">
                Feedback by {analysis.feedbackBy || 'Unknown'}: "{analysis.feedbackComment}"
              </p>
            </div>
          )}

          {/* Token usage */}
          {(analysis.promptTokens || analysis.completionTokens) && (
            <div className="text-[10px] text-[#9BA8B9] pt-2 border-t border-gray-50">
              Tokens: {analysis.promptTokens} in / {analysis.completionTokens} out
            </div>
          )}
        </div>
      )}

      {analysis.status === 'FAILED' && (
        <div className="mt-2 p-3 bg-red-50 text-red-700 text-sm rounded-lg">
          Analysis failed. You can try again.
          <button
            onClick={() => triggerMutation.mutate()}
            disabled={triggerMutation.isPending}
            className="ml-2 underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}
    </div>
  );
}
