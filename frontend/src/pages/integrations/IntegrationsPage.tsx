import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
} from 'recharts';
import {
  Plug, Plus, Activity, AlertCircle, CheckCircle2, XCircle, Power,
  ChevronRight, Trash2,
} from 'lucide-react';
import { integrationsApi } from '@/api/integrations';
import type { Page, IntegrationConfig, IntegrationDashboard } from '@/types/api';

const PROVIDER_COLORS: Record<string, string> = {
  GITHUB: '#24292e',
  GITLAB: '#fc6d26',
  JENKINS: '#d33833',
  JIRA: '#0052cc',
  GENERIC: '#6b7280',
};

const PROVIDER_BG: Record<string, string> = {
  GITHUB: 'bg-[var(--oqh-primary)] text-white',
  GITLAB: 'bg-orange-500 text-white',
  JENKINS: 'bg-red-600 text-white',
  JIRA: 'bg-[var(--oqh-blue)] text-white',
  GENERIC: 'bg-gray-500 text-white',
};

const STATUS_COLORS: Record<string, string> = {
  PROCESSED: '#10b981',
  FAILED: '#ef4444',
  SKIPPED: '#f59e0b',
  RECEIVED: '#6366f1',
  PROCESSING: '#3b82f6',
  IGNORED: '#9ca3af',
};

export function IntegrationsPage() {
  const [showCreate, setShowCreate] = useState(false);
  const [providerFilter, setProviderFilter] = useState('');
  const queryClient = useQueryClient();

  const { data: dashboard, isLoading: loadingDash } = useQuery<IntegrationDashboard>({
    queryKey: ['integration-dashboard'],
    queryFn: integrationsApi.getDashboard,
  });

  const { data: integrations, isLoading: loadingList } = useQuery<Page<IntegrationConfig>>({
    queryKey: ['integrations', providerFilter],
    queryFn: () => integrationsApi.getAll(providerFilter ? { provider: providerFilter, size: '50' } : { size: '50' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => integrationsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
      queryClient.invalidateQueries({ queryKey: ['integration-dashboard'] });
    },
  });

  const providers = ['GITHUB', 'GITLAB', 'JENKINS', 'JIRA', 'GENERIC'];

  const pieData = dashboard?.byProvider?.map((p) => ({
    name: p.provider,
    value: p.count,
    fill: PROVIDER_COLORS[p.provider] || '#6b7280',
  })) || [];

  const eventPieData = dashboard?.eventsByStatus?.map((s) => ({
    name: s.status,
    value: s.count,
    fill: STATUS_COLORS[s.status] || '#6b7280',
  })) || [];

  if (loadingDash && loadingList) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading integrations...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">External Integrations</h1>
          <p className="text-sm text-[var(--oqh-text-muted)] mt-1">Webhook-based CI/CD and tool integrations</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-[var(--oqh-blue)] text-white text-sm font-medium rounded-lg hover:bg-[var(--oqh-primary)] transition-colors"
        >
          <Plus className="h-4 w-4" />
          New Integration
        </button>
      </div>

      {/* Dashboard stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard icon={Plug} label="Total Integrations" value={dashboard?.totalIntegrations ?? 0} color="blue" />
        <StatCard icon={Power} label="Active" value={dashboard?.activeIntegrations ?? 0} color="emerald" />
        <StatCard icon={CheckCircle2} label="Events Processed" value={dashboard?.totalEventsProcessed ?? 0} color="indigo" />
        <StatCard icon={AlertCircle} label="Events Failed" value={dashboard?.totalEventsFailed ?? 0} color="red" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">By Provider</h3>
          {pieData.length > 0 ? (
            <div className="h-52">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, value }) => `${name} (${value})`}>
                    {pieData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-52 flex items-center justify-center text-[#9BA8B9]">No integrations yet</div>
          )}
        </div>
        <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-6">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Event Status Distribution</h3>
          {eventPieData.length > 0 ? (
            <div className="h-52">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={eventPieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, value }) => `${name} (${value})`}>
                    {eventPieData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-52 flex items-center justify-center text-[#9BA8B9]">No events yet</div>
          )}
        </div>
      </div>

      {/* Provider filter */}
      <div className="flex items-center gap-2">
        <span className="text-sm font-medium text-[var(--oqh-text)]">Filter:</span>
        <button
          onClick={() => setProviderFilter('')}
          className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
            !providerFilter ? 'bg-[var(--oqh-primary)] text-white' : 'bg-[#F7F9FC] text-[var(--oqh-text)] hover:bg-[#F7F9FC]'
          }`}
        >
          All
        </button>
        {providers.map((p) => (
          <button
            key={p}
            onClick={() => setProviderFilter(p === providerFilter ? '' : p)}
            className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors ${
              providerFilter === p ? 'bg-[var(--oqh-primary)] text-white' : 'bg-[#F7F9FC] text-[var(--oqh-text)] hover:bg-[#F7F9FC]'
            }`}
          >
            {p}
          </button>
        ))}
      </div>

      {/* Integration cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {integrations?.content.map((item) => (
          <IntegrationCard
            key={item.id}
            item={item}
            onDelete={() => {
              if (confirm(`Delete integration "${item.name}"?`)) {
                deleteMutation.mutate(item.id);
              }
            }}
          />
        ))}
        {integrations?.content.length === 0 && (
          <div className="col-span-full text-center py-12 text-[#9BA8B9]">No integrations found</div>
        )}
      </div>

      {/* Create modal */}
      {showCreate && (
        <CreateIntegrationModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            queryClient.invalidateQueries({ queryKey: ['integrations'] });
            queryClient.invalidateQueries({ queryKey: ['integration-dashboard'] });
          }}
        />
      )}
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color }: {
  icon: typeof Plug; label: string; value: number; color: string;
}) {
  const colorMap: Record<string, string> = {
    blue: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]',
    emerald: 'bg-emerald-50 text-emerald-700',
    indigo: 'bg-indigo-50 text-indigo-700',
    red: 'bg-red-50 text-red-700',
  };
  return (
    <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
      <div className="flex items-center gap-3">
        <div className={`p-2.5 rounded-lg ${colorMap[color]}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <p className="text-2xl font-bold text-[var(--oqh-text)]">{value.toLocaleString()}</p>
          <p className="text-xs text-[var(--oqh-text-muted)]">{label}</p>
        </div>
      </div>
    </div>
  );
}

function IntegrationCard({ item, onDelete }: { item: IntegrationConfig; onDelete: () => void }) {
  return (
    <div className="bg-white rounded-xl shadow-[var(--oqh-shadow)] border border-[var(--oqh-border)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className={`px-2 py-0.5 text-[10px] font-bold rounded ${PROVIDER_BG[item.provider] || 'bg-gray-500 text-white'}`}>
            {item.provider}
          </span>
          {item.enabled ? (
            <span className="flex items-center gap-1 text-[10px] font-medium text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded">
              <Activity className="h-3 w-3" /> Active
            </span>
          ) : (
            <span className="flex items-center gap-1 text-[10px] font-medium text-[var(--oqh-text-muted)] bg-[#F7F9FC] px-1.5 py-0.5 rounded">
              <XCircle className="h-3 w-3" /> Disabled
            </span>
          )}
        </div>
        <button onClick={onDelete} className="p-1 text-[#9BA8B9] hover:text-red-500 rounded transition-colors">
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
      <Link to={`/integrations/${item.id}`} className="block group">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] group-hover:text-[var(--oqh-blue)] transition-colors">{item.name}</h3>
        {item.description && <p className="text-xs text-[var(--oqh-text-muted)] mt-1 line-clamp-2">{item.description}</p>}
        <div className="flex items-center justify-between mt-4 pt-3 border-t border-[var(--oqh-border)]/50">
          <div className="text-xs text-[var(--oqh-text-muted)]">
            <span className="font-medium text-[var(--oqh-text)]">{item.eventCount}</span> events
            {item.projectName && <span className="ml-2">| {item.projectName}</span>}
          </div>
          <ChevronRight className="h-4 w-4 text-[#9BA8B9] group-hover:text-[var(--oqh-text-muted)] transition-colors" />
        </div>
      </Link>
    </div>
  );
}

function CreateIntegrationModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [provider, setProvider] = useState('GITHUB');
  const [webhookSecret, setWebhookSecret] = useState('');
  const [error, setError] = useState('');

  const createMutation = useMutation({
    mutationFn: () => integrationsApi.create({ name, description: description || undefined, provider, webhookSecret: webhookSecret || undefined }),
    onSuccess: () => onCreated(),
    onError: (err: any) => setError(err.response?.data?.message || 'Failed to create integration'),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-[0_8px_30px_rgba(14,43,73,.1)] w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold text-[var(--oqh-text)] mb-4">New Integration</h2>
        {error && <div className="mb-3 p-2 bg-red-50 text-red-700 text-sm rounded">{error}</div>}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
              placeholder="e.g., GitHub CI/CD - My Project"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Provider</label>
            <select
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
            >
              <option value="GITHUB">GitHub</option>
              <option value="GITLAB">GitLab</option>
              <option value="JENKINS">Jenkins</option>
              <option value="JIRA">Jira</option>
              <option value="GENERIC">Generic</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Webhook Secret (optional)</label>
            <input
              type="password"
              value={webhookSecret}
              onChange={(e) => setWebhookSecret(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
              placeholder="HMAC secret for signature validation"
            />
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-[var(--oqh-text)] hover:text-[var(--oqh-text)] transition-colors">Cancel</button>
          <button
            onClick={() => createMutation.mutate()}
            disabled={!name.trim() || createMutation.isPending}
            className="px-4 py-2 text-sm font-medium text-white bg-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 transition-colors"
          >
            {createMutation.isPending ? 'Creating...' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
}
