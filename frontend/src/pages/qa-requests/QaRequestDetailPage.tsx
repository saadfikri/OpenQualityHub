import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { qaRequestsApi } from '@/api/qa-services';
import { Badge } from '@/components/ui/Badge';
import {
  ArrowLeft, Clock, AlertTriangle, XCircle, MessageSquare,
  UserCheck, ArrowRightLeft,
} from 'lucide-react';
import type { QaServiceRequestItem, QaRequestActivity } from '@/types/api';

const STATUS_COLORS: Record<string, 'default' | 'success' | 'warning' | 'danger'> = {
  SUBMITTED: 'default', TRIAGED: 'default', ASSIGNED: 'warning',
  IN_PROGRESS: 'warning', ON_HOLD: 'danger', COMPLETED: 'success',
  CLOSED: 'success', CANCELLED: 'danger',
};

const VALID_TRANSITIONS: Record<string, string[]> = {
  SUBMITTED: ['TRIAGED', 'CANCELLED'],
  TRIAGED: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['IN_PROGRESS', 'ON_HOLD', 'CANCELLED'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED', 'CANCELLED'],
  ON_HOLD: ['IN_PROGRESS', 'CANCELLED'],
  COMPLETED: ['CLOSED', 'IN_PROGRESS'],
};

function ActivityIcon({ type }: { type: string }) {
  switch (type) {
    case 'STATUS_CHANGE': return <ArrowRightLeft className="h-4 w-4 text-[var(--oqh-blue)]" />;
    case 'ASSIGNMENT': return <UserCheck className="h-4 w-4 text-green-500" />;
    case 'COMMENT': return <MessageSquare className="h-4 w-4 text-[var(--oqh-text-muted)]" />;
    case 'PRIORITY_OVERRIDE': return <AlertTriangle className="h-4 w-4 text-orange-500" />;
    case 'HOURS_LOGGED': return <Clock className="h-4 w-4 text-violet-500" />;
    case 'SLA_BREACH': return <XCircle className="h-4 w-4 text-red-500" />;
    default: return <Clock className="h-4 w-4 text-[#9BA8B9]" />;
  }
}

function ActivityItem({ activity }: { activity: QaRequestActivity }) {
  let desc = '';
  switch (activity.activityType) {
    case 'STATUS_CHANGE':
      desc = activity.oldValue
        ? `Changed status from ${activity.oldValue} to ${activity.newValue}`
        : `Status set to ${activity.newValue}`;
      break;
    case 'ASSIGNMENT':
      desc = `Assigned to ${activity.newValue}`;
      break;
    case 'PRIORITY_OVERRIDE':
      desc = `Priority overridden from P${activity.oldValue} to P${activity.newValue}`;
      break;
    case 'COMMENT':
      desc = activity.comment || 'Comment added';
      break;
    case 'HOURS_LOGGED':
      desc = `Logged ${activity.newValue}h`;
      break;
    default:
      desc = activity.activityType.replace('_', ' ');
  }

  return (
    <div className="flex gap-3 py-3">
      <div className="mt-0.5"><ActivityIcon type={activity.activityType} /></div>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-[var(--oqh-text)]">{desc}</p>
        {activity.comment && activity.activityType !== 'COMMENT' && (
          <p className="text-sm text-[var(--oqh-text-muted)] mt-0.5">{activity.comment}</p>
        )}
        <p className="text-xs text-[#9BA8B9] mt-0.5">
          {activity.userName || 'System'} &middot;{' '}
          {new Date(activity.createdAt).toLocaleString()}
        </p>
      </div>
    </div>
  );
}

export function QaRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();

  const { data: req, isLoading, error } = useQuery<QaServiceRequestItem>({
    queryKey: ['qa-request', id],
    queryFn: () => qaRequestsApi.getById(id!),
    enabled: !!id,
  });

  const transitionMutation = useMutation({
    mutationFn: ({ status, comment }: { status: string; comment?: string }) =>
      qaRequestsApi.transition(id!, status, comment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['qa-request', id] });
      queryClient.invalidateQueries({ queryKey: ['qa-requests'] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--oqh-blue)]" />
      </div>
    );
  }

  if (error || !req) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">
        Failed to load request.
      </div>
    );
  }

  const transitions = VALID_TRANSITIONS[req.status] || [];
  const isBreached = req.resolutionDeadline && new Date(req.resolutionDeadline) < new Date()
    && !['CLOSED', 'CANCELLED', 'COMPLETED'].includes(req.status);

  return (
    <div>
      <Link to="/qa-requests" className="inline-flex items-center gap-1 text-sm text-[var(--oqh-text-muted)] hover:text-[var(--oqh-text)] mb-4">
        <ArrowLeft className="h-4 w-4" /> Back to Requests
      </Link>

      <div className="flex items-start justify-between mb-6">
        <div>
          <div className="flex items-center gap-3">
            <span className="font-mono text-sm text-[#9BA8B9]">{req.requestNumber}</span>
            <Badge variant={STATUS_COLORS[req.status] || 'default'}>
              {req.status.replace('_', ' ')}
            </Badge>
            {isBreached && (
              <span className="inline-flex items-center gap-1 text-xs text-red-600 bg-red-50 px-2 py-0.5 rounded font-medium">
                <AlertTriangle className="h-3 w-3" /> SLA BREACH
              </span>
            )}
          </div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)] mt-2">{req.title}</h1>
        </div>
        <div className="flex gap-2">
          {transitions.map((t) => (
            <button
              key={t}
              onClick={() => transitionMutation.mutate({ status: t })}
              disabled={transitionMutation.isPending}
              className={`px-3 py-1.5 text-sm font-medium rounded-lg border transition-colors ${
                t === 'CANCELLED'
                  ? 'border-red-200 text-red-600 hover:bg-red-50'
                  : 'border-[var(--oqh-blue)]/20 text-[var(--oqh-blue)] hover:bg-[var(--oqh-active-bg)]'
              }`}
            >
              {t.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Description */}
          {req.description && (
            <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
              <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-2">Description</h2>
              <p className="text-sm text-[var(--oqh-text-muted)] whitespace-pre-wrap">{req.description}</p>
            </div>
          )}

          {/* Activity Timeline */}
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)]">
            <h2 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Activity</h2>
            <div className="divide-y divide-[var(--oqh-border)]/50">
              {(req.activities || []).map((a) => (
                <ActivityItem key={a.id} activity={a} />
              ))}
              {(!req.activities || req.activities.length === 0) && (
                <p className="text-sm text-[#9BA8B9] py-4">No activity yet</p>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar details */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 space-y-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h2 className="text-sm font-semibold text-[var(--oqh-text)]">Details</h2>
            <Detail label="Service" value={req.qaServiceName} />
            <Detail label="Category" value={req.qaServiceCategory?.replace('_', ' ')} />
            <Detail label="Priority" value={`P${req.effectivePriority} (${req.impact} impact / ${req.urgency} urgency)`} />
            {req.priorityOverride != null && (
              <Detail label="Override" value={`P${req.priorityOverride} - ${req.overrideReason}`} />
            )}
            <Detail label="Requesting Team" value={req.requestingTeamName} />
            <Detail label="Requester" value={req.requestingUserName} />
            {req.projectName && <Detail label="Project" value={req.projectName} />}
            <Detail label="Assigned To" value={req.assignedUserName || 'Unassigned'} />
            {req.assignedTeamName && <Detail label="Assigned Team" value={req.assignedTeamName} />}
          </div>

          <div className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 space-y-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)] hover:-translate-y-0.5">
            <h2 className="text-sm font-semibold text-[var(--oqh-text)]">SLA & Timing</h2>
            <Detail label="Created" value={new Date(req.createdAt).toLocaleString()} />
            {req.responseDeadline && (
              <Detail label="Response Due" value={new Date(req.responseDeadline).toLocaleString()} />
            )}
            {req.respondedAt && (
              <Detail label="Responded At" value={new Date(req.respondedAt).toLocaleString()} />
            )}
            {req.resolutionDeadline && (
              <Detail label="Resolution Due" value={new Date(req.resolutionDeadline).toLocaleString()} />
            )}
            {req.resolvedAt && (
              <Detail label="Resolved At" value={new Date(req.resolvedAt).toLocaleString()} />
            )}
            {req.estimatedHours != null && <Detail label="Estimated" value={`${req.estimatedHours}h`} />}
            {req.actualHours != null && <Detail label="Actual" value={`${req.actualHours}h`} />}
          </div>
        </div>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | undefined | null }) {
  return (
    <div>
      <dt className="text-xs text-[var(--oqh-text-muted)]">{label}</dt>
      <dd className="text-sm text-[var(--oqh-text)] mt-0.5">{value || <span className="text-[#9BA8B9]">-</span>}</dd>
    </div>
  );
}
