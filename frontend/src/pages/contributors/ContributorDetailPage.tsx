import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, GitBranch, GitCommit, Rocket, GitMerge, ChevronLeft, ChevronRight } from 'lucide-react';
import { contributorsApi } from '@/api/contributors';
import { Badge } from '@/components/ui/Badge';
import type { ContributorDetail, Page, ContributorActivityItem } from '@/types/api';

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const activityIcons: Record<string, typeof GitCommit> = {
  COMMIT: GitCommit,
  PIPELINE_TRIGGER: Rocket,
  MERGE: GitMerge,
  TAG_PUSH: GitBranch,
  REVIEW: GitBranch,
};

const activityColors: Record<string, string> = {
  COMMIT: 'text-[var(--oqh-blue)] bg-[var(--oqh-active-bg)]',
  PIPELINE_TRIGGER: 'text-purple-600 bg-purple-50',
  MERGE: 'text-green-600 bg-green-50',
  TAG_PUSH: 'text-orange-600 bg-orange-50',
  REVIEW: 'text-yellow-600 bg-yellow-50',
};

const pipelineStatusVariant = (status: string | null): 'success' | 'danger' | 'warning' | 'default' => {
  switch (status) {
    case 'success': return 'success';
    case 'failed': return 'danger';
    case 'running': case 'pending': return 'warning';
    default: return 'default';
  }
};

const activityLabel: Record<string, string> = {
  COMMIT: 'Commit',
  PIPELINE_TRIGGER: 'Pipeline Trigger',
  MERGE: 'Merge',
  TAG_PUSH: 'Tag Push',
  REVIEW: 'Review',
};

export function ContributorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [actPage, setActPage] = useState(0);

  const { data: detail, isLoading, isError } = useQuery<ContributorDetail>({
    queryKey: ['contributor', id],
    queryFn: () => contributorsApi.getById(id!),
    enabled: !!id,
  });

  const { data: activities } = useQuery<Page<ContributorActivityItem>>({
    queryKey: ['contributor-activities', id, actPage],
    queryFn: () => contributorsApi.getActivities(id!, { page: String(actPage), size: '20' }),
    enabled: !!id,
  });

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading contributor...</div>;
  }

  if (isError || !detail) {
    return <div className="flex items-center justify-center h-64 text-red-500">Failed to load contributor.</div>;
  }

  const c = detail.contributor;
  const acts = activities?.content ?? [];

  return (
    <div className="space-y-6">
      <Link to="/contributors" className="inline-flex items-center gap-1 text-sm text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)] transition-colors">
        <ArrowLeft className="h-4 w-4" /> Back to Contributors
      </Link>

      {/* Header Card */}
      <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] p-6">
        <div className="flex items-start gap-4">
          {c.avatarUrl ? (
            <img src={c.avatarUrl} alt="" className="h-16 w-16 rounded-full" />
          ) : (
            <div className="h-16 w-16 rounded-full bg-[var(--oqh-active-bg)] flex items-center justify-center text-[var(--oqh-blue)] text-2xl font-bold">
              {(c.displayName || c.username).charAt(0).toUpperCase()}
            </div>
          )}
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-[var(--oqh-text)]">{c.displayName || c.username}</h1>
            <p className="text-sm text-[var(--oqh-text-muted)]">@{c.username} {c.email && `\u00B7 ${c.email}`}</p>
            <div className="flex items-center gap-3 mt-2">
              <Badge variant="info">{c.provider}</Badge>
              {c.linkedUserName && <Badge variant="success">Linked: {c.linkedUserName}</Badge>}
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mt-6">
          <div className="bg-[var(--oqh-active-bg)] rounded-lg p-4 text-center">
            <p className="text-2xl font-bold text-[var(--oqh-blue)]">{c.totalCommits}</p>
            <p className="text-xs text-[var(--oqh-blue)] font-medium mt-1">Commits</p>
          </div>
          <div className="bg-purple-50 rounded-lg p-4 text-center">
            <p className="text-2xl font-bold text-purple-700">{c.totalPipelines}</p>
            <p className="text-xs text-purple-600 font-medium mt-1">Pipelines</p>
          </div>
          <div className="bg-green-50 rounded-lg p-4 text-center">
            <p className="text-2xl font-bold text-green-700">{c.totalMerges}</p>
            <p className="text-xs text-green-600 font-medium mt-1">Merges</p>
          </div>
          <div className="bg-[#F7F9FC] rounded-lg p-4 text-center">
            <p className="text-sm font-medium text-[var(--oqh-text)]">{formatDate(c.lastActivityAt)}</p>
            <p className="text-xs text-[var(--oqh-text-muted)] font-medium mt-1">Last Active</p>
          </div>
        </div>
      </div>

      {/* Activity Breakdown */}
      {detail.activityBreakdown.length > 0 && (
        <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] p-6">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-3">Activity Breakdown</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
            {detail.activityBreakdown.map((ab) => {
              const colorClass = activityColors[ab.activityType] ?? 'text-[var(--oqh-text-muted)] bg-[#F7F9FC]';
              const Icon = activityIcons[ab.activityType] ?? GitBranch;
              return (
                <div key={ab.activityType} className={`rounded-lg p-3 text-center ${colorClass}`}>
                  <Icon className="h-5 w-5 mx-auto mb-1" />
                  <p className="text-lg font-bold">{ab.count}</p>
                  <p className="text-xs font-medium mt-0.5">
                    {activityLabel[ab.activityType] ?? ab.activityType.replaceAll('_', ' ')}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Projects */}
      {detail.projects.length > 0 && (
        <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] p-6">
          <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-3">Active Projects</h2>
          <div className="flex flex-wrap gap-2">
            {detail.projects.map((p) => (
              <span
                key={p.projectId}
                className="px-3 py-1.5 bg-[#F7F9FC] border border-[var(--oqh-border)] rounded-lg text-sm text-[var(--oqh-text)]"
              >
                {p.projectName}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Activity Timeline */}
      <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] p-6">
        <h2 className="text-lg font-semibold text-[var(--oqh-text)] mb-4">Activity Timeline</h2>
        {acts.length === 0 ? (
          <p className="text-sm text-[var(--oqh-text-muted)] py-4">No activities recorded yet.</p>
        ) : (
          <div className="space-y-0">
            {acts.map((act, idx) => {
              const Icon = activityIcons[act.activityType] ?? GitBranch;
              const colorClass = activityColors[act.activityType] ?? 'text-[var(--oqh-text-muted)] bg-[#F7F9FC]';
              return (
                <div key={act.id} className="relative flex gap-4 pb-6">
                  {idx < acts.length - 1 && (
                    <div className="absolute left-[17px] top-10 bottom-0 w-px bg-[var(--oqh-border)]" />
                  )}
                  <div className={`flex-shrink-0 h-9 w-9 rounded-full flex items-center justify-center ${colorClass}`}>
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-medium text-[var(--oqh-text)]">
                        {activityLabel[act.activityType] ?? act.activityType.replaceAll('_', ' ')}
                      </span>
                      {act.projectName && (
                        <Badge variant="default">{act.projectName}</Badge>
                      )}
                      {act.pipelineId && (
                        <span className="text-xs text-[var(--oqh-text-muted)]">Pipeline #{act.pipelineId}</span>
                      )}
                      {act.pipelineStatus && (
                        <Badge variant={pipelineStatusVariant(act.pipelineStatus)}>{act.pipelineStatus}</Badge>
                      )}
                    </div>
                    {act.commitMessage && (
                      <p className="text-sm text-[var(--oqh-text-muted)] mt-0.5 truncate">{act.commitMessage}</p>
                    )}
                    <div className="flex items-center gap-3 mt-1 text-xs text-[#9BA8B9]">
                      {act.branch && <span>{act.branch}</span>}
                      {act.commitSha && <span className="font-mono">{act.commitSha.substring(0, 8)}</span>}
                      <span>{formatDate(act.createdAt)}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {activities && activities.totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-[var(--oqh-border)]/50 mt-4">
            <p className="text-sm text-[var(--oqh-text-muted)]">
              Page {activities.number + 1} of {activities.totalPages}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setActPage((p) => Math.max(0, p - 1))}
                disabled={activities.first}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-[#F7F9FC]"
                aria-label="Previous page"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setActPage((p) => p + 1)}
                disabled={activities.last}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-[#F7F9FC]"
                aria-label="Next page"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
