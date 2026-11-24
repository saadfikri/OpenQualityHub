import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft, RefreshCw, Heart, AlertTriangle, XCircle, HelpCircle,
  CheckCircle2, Clock, ExternalLink, FolderGit2, UserX, Shield,
} from 'lucide-react';
import { documentationApi } from '@/api/documentation';
import type { DocumentationAssetItem } from '@/types/api';

const HEALTH_CONFIG: Record<string, { color: string; bg: string; icon: typeof Heart }> = {
  HEALTHY: { color: 'text-emerald-700', bg: 'bg-emerald-100', icon: Heart },
  WARNING: { color: 'text-amber-700', bg: 'bg-amber-100', icon: AlertTriangle },
  CRITICAL: { color: 'text-red-700', bg: 'bg-red-100', icon: XCircle },
  UNKNOWN: { color: 'text-[var(--oqh-text-muted)]', bg: 'bg-[#F7F9FC]', icon: HelpCircle },
};

const CHECK_STATUS_CONFIG: Record<string, { color: string; bg: string; icon: typeof CheckCircle2 }> = {
  PASS: { color: 'text-emerald-700', bg: 'bg-emerald-50', icon: CheckCircle2 },
  WARNING: { color: 'text-amber-700', bg: 'bg-amber-50', icon: AlertTriangle },
  FAIL: { color: 'text-red-700', bg: 'bg-red-50', icon: XCircle },
};

const REVIEW_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-[#F7F9FC] text-[var(--oqh-text)]',
  IN_REVIEW: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]',
  APPROVED: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-red-100 text-red-700',
  EXPIRED: 'bg-amber-100 text-amber-700',
};

const DOC_TYPE_LABELS: Record<string, string> = {
  API_SPEC: 'API Spec', USER_GUIDE: 'User Guide', RUNBOOK: 'Runbook',
  ARCHITECTURE: 'Architecture', TEST_PLAN: 'Test Plan', RELEASE_NOTES: 'Release Notes',
  ONBOARDING: 'Onboarding', POLICY: 'Policy', WIKI: 'Wiki', OTHER: 'Other',
};

export function DocumentationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();

  const { data: asset, isLoading } = useQuery<DocumentationAssetItem>({
    queryKey: ['documentation', id],
    queryFn: () => documentationApi.getById(id!),
    enabled: !!id,
  });

  const runChecks = useMutation({
    mutationFn: () => documentationApi.runHealthChecks(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documentation', id] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (!asset) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Documentation asset not found.
      </div>
    );
  }

  const health = HEALTH_CONFIG[asset.healthStatus] || HEALTH_CONFIG.UNKNOWN;
  const HealthIcon = health.icon;

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <Link
          to="/documentation"
          className="text-sm text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)] flex items-center gap-1 mb-3"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Documentation
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[var(--oqh-text)]">{asset.title}</h1>
            <p className="text-sm text-[var(--oqh-text-muted)] mt-1">{asset.description}</p>
          </div>
          <button
            onClick={() => runChecks.mutate()}
            disabled={runChecks.isPending}
            className="flex items-center gap-2 px-4 py-2 bg-[var(--oqh-blue)] text-white text-sm font-medium rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 transition-colors"
          >
            <RefreshCw className={`h-4 w-4 ${runChecks.isPending ? 'animate-spin' : ''}`} />
            Run Health Checks
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Health Checks */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)]">
            <div className="p-6 border-b border-[var(--oqh-border)]">
              <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Health Checks</h2>
              <p className="text-sm text-[var(--oqh-text-muted)] mt-1">
                Last checked: {asset.lastCheckedAt ? new Date(asset.lastCheckedAt).toLocaleString() : 'Never'}
              </p>
            </div>
            <div className="divide-y divide-[var(--oqh-border)]/50">
              {asset.recentChecks.length === 0 ? (
                <div className="p-6 text-center text-[var(--oqh-text-muted)] text-sm">
                  No health checks run yet. Click "Run Health Checks" to start.
                </div>
              ) : (
                asset.recentChecks.map((check) => {
                  const cfg = CHECK_STATUS_CONFIG[check.status] || CHECK_STATUS_CONFIG.FAIL;
                  const StatusIcon = cfg.icon;
                  return (
                    <div key={check.id} className={`flex items-start gap-4 p-4 ${cfg.bg}`}>
                      <StatusIcon className={`h-5 w-5 mt-0.5 ${cfg.color}`} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-[var(--oqh-text)]">
                            {check.checkType.replace(/_/g, ' ')}
                          </span>
                          <span className={`text-xs font-bold px-1.5 py-0.5 rounded ${cfg.color} ${cfg.bg}`}>
                            {check.status}
                          </span>
                        </div>
                        <p className="text-sm text-[var(--oqh-text-muted)] mt-0.5">{check.message}</p>
                        <p className="text-xs text-[#9BA8B9] mt-1">
                          {new Date(check.executedAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Reviews */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)]">
            <div className="p-6 border-b border-[var(--oqh-border)]">
              <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Reviews</h2>
            </div>
            <div className="divide-y divide-[var(--oqh-border)]/50">
              {asset.recentReviews.length === 0 ? (
                <div className="p-6 text-center text-[var(--oqh-text-muted)] text-sm">
                  No reviews yet.
                </div>
              ) : (
                asset.recentReviews.map((review) => (
                  <div key={review.id} className="p-4 flex items-start gap-4">
                    <Shield className="h-5 w-5 text-[#9BA8B9] mt-0.5" />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-[var(--oqh-text)]">
                          {review.reviewerName || 'Unassigned'}
                        </span>
                        <span className={`text-xs font-bold px-1.5 py-0.5 rounded ${REVIEW_STATUS_COLORS[review.status] || 'bg-[#F7F9FC] text-[var(--oqh-text)]'}`}>
                          {review.status}
                        </span>
                      </div>
                      {review.dueDate && (
                        <p className="text-xs text-[var(--oqh-text-muted)] mt-1 flex items-center gap-1">
                          <Clock className="h-3 w-3" /> Due: {review.dueDate}
                        </p>
                      )}
                      {review.comment && (
                        <p className="text-sm text-[var(--oqh-text-muted)] mt-1 italic">"{review.comment}"</p>
                      )}
                      {review.completedAt && (
                        <p className="text-xs text-[#9BA8B9] mt-1">
                          Completed: {new Date(review.completedAt).toLocaleString()}
                        </p>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Health Status */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h3 className="text-sm font-medium text-[var(--oqh-text-muted)] mb-3">Health Status</h3>
            <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-bold ${health.bg} ${health.color}`}>
              <HealthIcon className="h-5 w-5" />
              {asset.healthStatus}
            </div>
          </div>

          {/* Details */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 space-y-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h3 className="text-sm font-medium text-[var(--oqh-text-muted)]">Details</h3>

            <div>
              <p className="text-xs text-[#9BA8B9]">Type</p>
              <p className="text-sm font-medium text-[var(--oqh-text)]">{DOC_TYPE_LABELS[asset.docType] || asset.docType}</p>
            </div>

            <div>
              <p className="text-xs text-[#9BA8B9]">Category</p>
              <p className="text-sm font-medium text-[var(--oqh-text)]">{asset.category}</p>
            </div>

            <div>
              <p className="text-xs text-[#9BA8B9]">Owner</p>
              {asset.ownerName ? (
                <p className="text-sm font-medium text-[var(--oqh-text)]">{asset.ownerName}</p>
              ) : (
                <p className="text-sm text-orange-600 flex items-center gap-1">
                  <UserX className="h-3 w-3" /> No owner assigned
                </p>
              )}
            </div>

            {asset.projectName && (
              <div>
                <p className="text-xs text-[#9BA8B9]">Project</p>
                <p className="text-sm font-medium text-[var(--oqh-text)]">{asset.projectName}</p>
              </div>
            )}

            <div>
              <p className="text-xs text-[#9BA8B9]">Review Cycle</p>
              <p className="text-sm font-medium text-[var(--oqh-text)]">{asset.reviewCycleDays} days</p>
            </div>

            <div>
              <p className="text-xs text-[#9BA8B9]">Last Reviewed</p>
              <p className="text-sm font-medium text-[var(--oqh-text)]">
                {asset.lastReviewedAt ? new Date(asset.lastReviewedAt).toLocaleDateString() : 'Never'}
              </p>
            </div>
          </div>

          {/* Location */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 space-y-3 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h3 className="text-sm font-medium text-[var(--oqh-text-muted)]">Location</h3>
            {asset.url && (
              <a
                href={asset.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 text-sm text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] break-all"
              >
                <ExternalLink className="h-4 w-4 flex-shrink-0" />
                {asset.url}
              </a>
            )}
            {asset.repoPath && (
              <div className="flex items-center gap-2 text-sm text-[var(--oqh-text)]">
                <FolderGit2 className="h-4 w-4 flex-shrink-0 text-[#9BA8B9]" />
                <code className="text-xs bg-[#F7F9FC] px-2 py-0.5 rounded break-all">{asset.repoPath}</code>
              </div>
            )}
          </div>

          {/* Timestamps */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 space-y-2 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h3 className="text-sm font-medium text-[var(--oqh-text-muted)] mb-2">Timestamps</h3>
            <div className="text-xs text-[var(--oqh-text-muted)]">
              <p>Created: {new Date(asset.createdAt).toLocaleString()}</p>
              <p>Updated: {new Date(asset.updatedAt).toLocaleString()}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
