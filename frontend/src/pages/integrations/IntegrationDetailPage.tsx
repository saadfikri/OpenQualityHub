import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft, Copy, CheckCircle2, XCircle, Clock, AlertTriangle,
  Activity, Power, PowerOff, RefreshCw, Trash2, Shield, Pencil, X,
  GitBranch, Hash, Timer, ChevronDown, ChevronRight,
} from 'lucide-react';
import { integrationsApi } from '@/api/integrations';
import type { IntegrationConfig, Page, WebhookEventItem } from '@/types/api';

const STATUS_CONFIG: Record<string, { icon: typeof CheckCircle2; color: string; bg: string }> = {
  PROCESSED: { icon: CheckCircle2, color: 'text-emerald-700', bg: 'bg-emerald-50' },
  FAILED: { icon: XCircle, color: 'text-red-700', bg: 'bg-red-50' },
  SKIPPED: { icon: AlertTriangle, color: 'text-amber-700', bg: 'bg-amber-50' },
  RECEIVED: { icon: Clock, color: 'text-indigo-700', bg: 'bg-indigo-50' },
  PROCESSING: { icon: RefreshCw, color: 'text-[var(--oqh-blue)]', bg: 'bg-[var(--oqh-active-bg)]' },
  IGNORED: { icon: PowerOff, color: 'text-[var(--oqh-text-muted)]', bg: 'bg-[#F7F9FC]' },
};

const PROVIDER_BG: Record<string, string> = {
  GITHUB: 'bg-[var(--oqh-primary)] text-white',
  GITLAB: 'bg-orange-500 text-white',
  JENKINS: 'bg-red-600 text-white',
  JIRA: 'bg-[var(--oqh-blue)] text-white',
  GENERIC: 'bg-gray-500 text-white',
};

export function IntegrationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', description: '', webhookSecret: '', gitlabUrl: '', apiToken: '' });
  const [expandedEvent, setExpandedEvent] = useState<string | null>(null);

  const { data: integration, isLoading } = useQuery<IntegrationConfig>({
    queryKey: ['integration', id],
    queryFn: () => integrationsApi.getById(id!),
    enabled: !!id,
  });

  const { data: eventsPage } = useQuery<Page<WebhookEventItem>>({
    queryKey: ['integration-events', id],
    queryFn: () => integrationsApi.getEvents(id!, { size: '50' }),
    enabled: !!id,
  });

  const toggleMutation = useMutation({
    mutationFn: () => integrationsApi.update(id!, { enabled: !integration?.enabled }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integration', id] });
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => integrationsApi.delete(id!),
    onSuccess: () => navigate('/integrations'),
  });

  const updateMutation = useMutation({
    mutationFn: (data: { name?: string; description?: string; webhookSecret?: string }) =>
      integrationsApi.update(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integration', id] });
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
      setShowEdit(false);
    },
  });

  function openEditModal() {
    if (integration) {
      setEditForm({
        name: integration.name,
        description: integration.description || '',
        webhookSecret: '',
        gitlabUrl: '',
        apiToken: '',
      });
      setShowEdit(true);
    }
  }

  function handleEditSubmit(e: React.FormEvent) {
    e.preventDefault();
    const data: Record<string, string> = { name: editForm.name, description: editForm.description };
    if (editForm.webhookSecret) data.webhookSecret = editForm.webhookSecret;
    // Build config JSON for GitLab API credentials
    if (editForm.gitlabUrl || editForm.apiToken) {
      const configObj: Record<string, string> = {};
      if (editForm.gitlabUrl) configObj.gitlab_url = editForm.gitlabUrl;
      if (editForm.apiToken) configObj.api_token = editForm.apiToken;
      data.config = JSON.stringify(configObj);
    }
    updateMutation.mutate(data);
  }

  const webhookUrl = id ? `${window.location.origin}/api/v1/webhooks/${id}` : '';

  function copyWebhookUrl() {
    navigator.clipboard.writeText(webhookUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  if (!integration) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Integration not found</div>;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Link to="/integrations" className="p-2 text-[#9BA8B9] hover:text-[var(--oqh-text-muted)] rounded-lg hover:bg-[#F7F9FC] transition-colors">
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-[var(--oqh-text)]">{integration.name}</h1>
            <span className={`px-2 py-0.5 text-[10px] font-bold rounded ${PROVIDER_BG[integration.provider] || 'bg-gray-500 text-white'}`}>
              {integration.provider}
            </span>
            {integration.enabled ? (
              <span className="flex items-center gap-1 text-xs font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                <Activity className="h-3 w-3" /> Active
              </span>
            ) : (
              <span className="flex items-center gap-1 text-xs font-medium text-[var(--oqh-text-muted)] bg-[#F7F9FC] px-2 py-0.5 rounded">
                <PowerOff className="h-3 w-3" /> Disabled
              </span>
            )}
          </div>
          {integration.description && (
            <p className="text-sm text-[var(--oqh-text-muted)] mt-1">{integration.description}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={openEditModal}
            className="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-[var(--oqh-blue)] border border-[var(--oqh-blue)]/30 rounded-lg hover:bg-[var(--oqh-active-bg)] transition-colors"
          >
            <Pencil className="h-4 w-4" /> Edit
          </button>
          <button
            onClick={() => toggleMutation.mutate()}
            className={`inline-flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-lg border transition-colors ${
              integration.enabled
                ? 'border-amber-300 text-amber-700 hover:bg-amber-50'
                : 'border-emerald-300 text-emerald-700 hover:bg-emerald-50'
            }`}
          >
            {integration.enabled ? <PowerOff className="h-4 w-4" /> : <Power className="h-4 w-4" />}
            {integration.enabled ? 'Disable' : 'Enable'}
          </button>
          <button
            onClick={() => {
              if (confirm(`Delete integration "${integration.name}"?`)) {
                deleteMutation.mutate();
              }
            }}
            className="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-red-700 border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
          >
            <Trash2 className="h-4 w-4" /> Delete
          </button>
        </div>
      </div>

      {/* Webhook URL */}
      <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-3 flex items-center gap-2">
          <Shield className="h-4 w-4 text-[var(--oqh-blue)]" /> Webhook Configuration
        </h3>
        <div className="flex items-center gap-3">
          <div className="flex-1 bg-[#F7F9FC] rounded-lg px-4 py-2.5 font-mono text-sm text-[var(--oqh-text)] border border-[var(--oqh-border)] truncate">
            {webhookUrl}
          </div>
          <button
            onClick={copyWebhookUrl}
            className="inline-flex items-center gap-2 px-3 py-2.5 text-sm font-medium text-[var(--oqh-text)] border border-[var(--oqh-border)] rounded-lg hover:bg-[#F7F9FC] transition-colors"
          >
            {copied ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
        <div className="mt-3 grid grid-cols-2 md:grid-cols-4 gap-3 text-xs text-[var(--oqh-text-muted)]">
          <div className="flex items-center gap-1.5">
            <span className={`h-2 w-2 rounded-full ${integration.hasSecret ? 'bg-emerald-500' : 'bg-amber-400'}`} />
            Secret: {integration.hasSecret ? 'Configured' : 'Not set'}
          </div>
          <div>Events received: <span className="font-medium text-[var(--oqh-text)]">{integration.eventCount.toLocaleString()}</span></div>
          {integration.lastEventAt && (
            <div>Last event: <span className="font-medium text-[var(--oqh-text)]">{new Date(integration.lastEventAt).toLocaleString()}</span></div>
          )}
          {integration.projectName && (
            <div>Project: <span className="font-medium text-[var(--oqh-text)]">{integration.projectName}</span></div>
          )}
        </div>
      </div>

      {/* Info cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <InfoCard label="Total Events" value={integration.eventCount} />
        <InfoCard
          label="Processed"
          value={eventsPage?.content.filter((e) => e.status === 'PROCESSED').length ?? 0}
          color="text-emerald-600"
        />
        <InfoCard
          label="Failed"
          value={eventsPage?.content.filter((e) => e.status === 'FAILED').length ?? 0}
          color="text-red-600"
        />
        <InfoCard
          label="Skipped"
          value={eventsPage?.content.filter((e) => e.status === 'SKIPPED').length ?? 0}
          color="text-amber-600"
        />
        <InfoCard label="Created" value={new Date(integration.createdAt).toLocaleDateString()} />
      </div>

      {/* Edit Modal */}
      {showEdit && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setShowEdit(false)}>
          <div className="bg-white rounded-xl shadow-[0_8px_30px_rgba(14,43,73,.1)] w-full max-w-lg mx-4" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--oqh-border)]/50">
              <h2 className="text-lg font-semibold text-[var(--oqh-text)]">Edit Integration</h2>
              <button onClick={() => setShowEdit(false)} className="p-1 text-[#9BA8B9] hover:text-[var(--oqh-text-muted)] rounded">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleEditSubmit} className="px-6 py-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Name</label>
                <input
                  type="text"
                  required
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  className="w-full px-3 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Description</label>
                <textarea
                  rows={2}
                  value={editForm.description}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                  className="w-full px-3 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">
                  Webhook Secret / Signing Token
                </label>
                <input
                  type="password"
                  placeholder={integration.hasSecret ? 'Leave empty to keep current' : 'Enter secret or signing token'}
                  value={editForm.webhookSecret}
                  onChange={(e) => setEditForm({ ...editForm, webhookSecret: e.target.value })}
                  className="w-full px-3 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
                />
                <p className="text-xs text-[#9BA8B9] mt-1">
                  Supports GitLab signing tokens (whsec_...) and legacy secret tokens.
                </p>
              </div>
              {integration.provider === 'GITLAB' && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">GitLab URL</label>
                    <input
                      type="text"
                      placeholder="https://gitlab.com"
                      value={editForm.gitlabUrl}
                      onChange={(e) => setEditForm({ ...editForm, gitlabUrl: e.target.value })}
                      className="w-full px-3 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">GitLab API Token</label>
                    <input
                      type="password"
                      placeholder="glpat-xxxxxxxxxxxxxxxxxxxx"
                      value={editForm.apiToken}
                      onChange={(e) => setEditForm({ ...editForm, apiToken: e.target.value })}
                      className="w-full px-3 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
                    />
                    <p className="text-xs text-[#9BA8B9] mt-1">
                      Required to fetch test reports from GitLab API. Create one in GitLab → Settings → Access Tokens.
                    </p>
                  </div>
                </>
              )}
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowEdit(false)}
                  className="px-4 py-2 text-sm font-medium text-[var(--oqh-text)] border border-[var(--oqh-border)] rounded-lg hover:bg-[#F7F9FC]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="px-4 py-2 text-sm font-medium text-white bg-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50"
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Pipeline Event Log */}
      <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)]">
        <div className="px-5 py-4 border-b border-[var(--oqh-border)]/50">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)]">Pipeline Events</h3>
        </div>
        {eventsPage?.content && eventsPage.content.length > 0 ? (
          <div className="divide-y divide-[var(--oqh-border)]/50">
            {eventsPage.content.map((event) => {
              const cfg = STATUS_CONFIG[event.status] || STATUS_CONFIG.RECEIVED;
              const Icon = cfg.icon;
              const isExpanded = expandedEvent === event.id;
              const pipeline = parsePipelinePayload(event.payload);

              return (
                <div key={event.id}>
                  <button
                    onClick={() => setExpandedEvent(isExpanded ? null : event.id)}
                    className="w-full px-5 py-3 flex items-center gap-4 hover:bg-[#F7F9FC] transition-colors text-left"
                  >
                    <div className={`p-1.5 rounded-lg ${cfg.bg}`}>
                      <Icon className={`h-4 w-4 ${cfg.color}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        {pipeline ? (
                          <span className="text-sm font-medium text-[var(--oqh-text)]">
                            Pipeline #{pipeline.pipelineId}
                          </span>
                        ) : (
                          <span className="text-sm font-medium text-[var(--oqh-text)]">{event.eventType}</span>
                        )}
                        <span className={`px-1.5 py-0.5 text-[10px] font-medium rounded ${cfg.bg} ${cfg.color}`}>
                          {event.status}
                        </span>
                        {pipeline?.pipelineStatus && (
                          <span className={`px-1.5 py-0.5 text-[10px] font-medium rounded ${
                            pipeline.pipelineStatus === 'success' ? 'bg-emerald-50 text-emerald-700' :
                            pipeline.pipelineStatus === 'failed' ? 'bg-red-50 text-red-700' :
                            'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]'
                          }`}>
                            {pipeline.pipelineStatus}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-xs text-[var(--oqh-text-muted)]">
                        {pipeline?.branch && (
                          <span className="inline-flex items-center gap-1">
                            <GitBranch className="h-3 w-3" /> {pipeline.branch}
                          </span>
                        )}
                        {pipeline?.commitSha && (
                          <span className="inline-flex items-center gap-1 font-mono">
                            <Hash className="h-3 w-3" /> {pipeline.commitSha.substring(0, 8)}
                          </span>
                        )}
                        {pipeline?.duration && (
                          <span className="inline-flex items-center gap-1">
                            <Timer className="h-3 w-3" /> {formatDuration(pipeline.duration)}
                          </span>
                        )}
                        {pipeline?.user && (
                          <span>{pipeline.user}</span>
                        )}
                      </div>
                      {event.processingError && (
                        <p className="text-xs text-red-600 mt-1">{event.processingError}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="text-xs text-[#9BA8B9] whitespace-nowrap text-right">
                        <div>{new Date(event.receivedAt).toLocaleDateString()}</div>
                        <div>{new Date(event.receivedAt).toLocaleTimeString()}</div>
                      </div>
                      {isExpanded ? <ChevronDown className="h-4 w-4 text-[#9BA8B9]" /> : <ChevronRight className="h-4 w-4 text-[#9BA8B9]" />}
                    </div>
                  </button>

                  {/* Expanded: pipeline stages & jobs (GitLab-style) */}
                  {isExpanded && pipeline?.stages && pipeline.stages.length > 0 && (
                    <div className="px-5 pb-4">
                      {/* Stage pipeline visualization */}
                      <div className="ml-10 flex items-center gap-1 mb-3 overflow-x-auto pb-1">
                        {pipeline.stages.map((stage, i) => {
                          const allSuccess = stage.jobs.length > 0 && stage.jobs.every((j) => j.status === 'success');
                          const anyFailed = stage.jobs.some((j) => j.status === 'failed');
                          const allCreated = stage.jobs.every((j) => j.status === 'created');
                          return (
                            <div key={stage.name} className="flex items-center gap-1">
                              {i > 0 && <div className="w-4 h-px bg-[#9BA8B9]" />}
                              <div className={`px-3 py-1.5 rounded-full text-xs font-medium border ${
                                anyFailed ? 'bg-red-50 text-red-700 border-red-200' :
                                allSuccess ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                                allCreated ? 'bg-[#F7F9FC] text-[var(--oqh-text-muted)] border-[var(--oqh-border)]' :
                                'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)] border-[var(--oqh-blue)]/20'
                              }`}>
                                {stage.name}
                              </div>
                            </div>
                          );
                        })}
                      </div>

                      {/* Jobs grouped by stage */}
                      <div className="ml-10 bg-[#F7F9FC] rounded-lg border border-[var(--oqh-border)] overflow-hidden">
                        {pipeline.stages.map((stage) => (
                          <div key={stage.name}>
                            <div className="px-4 py-2 bg-[#F7F9FC] border-b border-[var(--oqh-border)] flex items-center justify-between">
                              <span className="text-xs font-semibold text-[var(--oqh-text-muted)]">{stage.name}</span>
                              <span className="text-[10px] text-[#9BA8B9]">{stage.jobs.length} job{stage.jobs.length > 1 ? 's' : ''}</span>
                            </div>
                            <div className="divide-y divide-[var(--oqh-border)]">
                              {stage.jobs.map((job, j) => (
                                <div key={j} className="px-4 py-2.5 flex items-center gap-3">
                                  <div className={`h-2.5 w-2.5 rounded-full ${
                                    job.status === 'success' ? 'bg-emerald-500' :
                                    job.status === 'failed' ? 'bg-red-500' :
                                    job.status === 'running' ? 'bg-[var(--oqh-active-bg)]0 animate-pulse' :
                                    job.status === 'pending' ? 'bg-amber-400 animate-pulse' :
                                    job.status === 'skipped' ? 'bg-[#9BA8B9]' :
                                    job.status === 'created' ? 'bg-[#9BA8B9]' :
                                    'bg-amber-500'
                                  }`} />
                                  <span className="text-sm font-medium text-[var(--oqh-text)] flex-1">{job.name}</span>
                                  <div className="flex items-center gap-3 text-xs text-[var(--oqh-text-muted)]">
                                    {job.duration != null && job.duration > 0 && (
                                      <span className="inline-flex items-center gap-1">
                                        <Timer className="h-3 w-3" /> {formatDuration(job.duration)}
                                      </span>
                                    )}
                                    <span className={`px-1.5 py-0.5 text-[10px] font-medium rounded ${
                                      job.status === 'success' ? 'bg-emerald-50 text-emerald-700' :
                                      job.status === 'failed' ? 'bg-red-50 text-red-700' :
                                      job.status === 'running' ? 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]' :
                                      job.status === 'pending' ? 'bg-amber-50 text-amber-700' :
                                      job.status === 'skipped' ? 'bg-[#F7F9FC] text-[var(--oqh-text-muted)]' :
                                      job.status === 'created' ? 'bg-[#F7F9FC] text-[#9BA8B9]' :
                                      'bg-[#F7F9FC] text-[var(--oqh-text-muted)]'
                                    }`}>
                                      {job.status}
                                    </span>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                      {pipeline.commitMessage && (
                        <div className="ml-10 mt-2 text-xs text-[var(--oqh-text-muted)]">
                          <span className="font-medium">Commit:</span> {pipeline.commitMessage}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Expanded: no pipeline data */}
                  {isExpanded && (!pipeline?.builds || pipeline.builds.length === 0) && (
                    <div className="px-5 pb-4">
                      <div className="ml-10 bg-[#F7F9FC] rounded-lg border border-[var(--oqh-border)] p-4 text-xs text-[var(--oqh-text-muted)] font-mono whitespace-pre-wrap max-h-48 overflow-auto">
                        {event.payload ? JSON.stringify(JSON.parse(event.payload), null, 2).substring(0, 2000) : 'No payload'}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="px-5 py-12 text-center text-[#9BA8B9]">No webhook events recorded yet</div>
        )}
      </div>
    </div>
  );
}

interface PipelineInfo {
  pipelineId: number | null;
  pipelineStatus: string | null;
  branch: string | null;
  commitSha: string | null;
  commitMessage: string | null;
  duration: number | null;
  user: string | null;
  builds: { name: string; stage: string; status: string; duration: number | null }[];
  stages: { name: string; jobs: { name: string; status: string; duration: number | null }[] }[];
}

function parsePipelinePayload(payload: string | null): PipelineInfo | null {
  if (!payload) return null;
  try {
    const data = JSON.parse(payload);

    // GitLab Pipeline Hook
    const oa = data.object_attributes;
    const pipelineId = oa?.id ?? data.pipeline?.id ?? null;
    const pipelineStatus = oa?.status ?? data.pipeline?.status ?? null;
    const branch = oa?.ref ?? data.ref ?? null;
    const commitSha = oa?.sha ?? data.checkout_sha ?? null;
    const duration = oa?.duration ?? data.pipeline?.duration ?? null;
    const user = data.user?.name ?? null;

    // Commit message
    const commitMessage = data.commit?.message?.split('\n')[0] ?? null;

    // Builds / Jobs
    const builds = (data.builds || []).map((b: Record<string, unknown>) => ({
      name: b.name as string || 'unknown',
      stage: b.stage as string || '',
      status: b.status as string || 'unknown',
      duration: typeof b.duration === 'number' ? b.duration : null,
    }));

    // Group by stage, preserve order of first appearance (= pipeline stage order)
    const stageOrder: string[] = [];
    const stageMap = new Map<string, { name: string; status: string; duration: number | null }[]>();
    // Use stages array from payload if available, otherwise derive from builds
    const pipelineStages: string[] = oa?.stages ?? data.stages ?? [];
    if (pipelineStages.length > 0) {
      pipelineStages.forEach((s: string) => { stageOrder.push(s); stageMap.set(s, []); });
    }
    for (const b of builds) {
      if (!stageMap.has(b.stage)) {
        stageOrder.push(b.stage);
        stageMap.set(b.stage, []);
      }
      stageMap.get(b.stage)!.push({ name: b.name, status: b.status, duration: b.duration });
    }
    const stages = stageOrder.map((s) => ({ name: s, jobs: stageMap.get(s) || [] }));

    if (!pipelineId && !branch && builds.length === 0) return null;

    return { pipelineId, pipelineStatus, branch, commitSha, commitMessage, duration, user, builds, stages };
  } catch {
    return null;
  }
}

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${Math.round(seconds)}s`;
  const mins = Math.floor(seconds / 60);
  const secs = Math.round(seconds % 60);
  if (mins < 60) return `${mins}m ${secs}s`;
  const hours = Math.floor(mins / 60);
  return `${hours}h ${mins % 60}m`;
}

function InfoCard({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-4 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
      <p className="text-xs text-[var(--oqh-text-muted)]">{label}</p>
      <p className={`text-lg font-bold mt-1 ${color || 'text-[var(--oqh-text)]'}`}>{typeof value === 'number' ? value.toLocaleString() : value}</p>
    </div>
  );
}
