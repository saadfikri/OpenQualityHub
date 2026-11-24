import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft, Database, Plus, Download, Play, Trash2,
} from 'lucide-react';
import { testDataApi } from '@/api/test-data';
import type { Page, TestDataTemplateDetail, TestDataGenerationItem } from '@/types/api';

const PROFILE_TYPES = ['POSITIVE', 'NEGATIVE', 'EDGE_CASE', 'BOUNDARY', 'LOCALIZATION', 'PERFORMANCE'];

const PROFILE_COLORS: Record<string, string> = {
  POSITIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  NEGATIVE: 'bg-red-50 text-red-700 border-red-200',
  EDGE_CASE: 'bg-amber-50 text-amber-700 border-amber-200',
  BOUNDARY: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)] border-[var(--oqh-blue)]/20',
  LOCALIZATION: 'bg-purple-50 text-purple-700 border-purple-200',
  PERFORMANCE: 'bg-orange-50 text-orange-700 border-orange-200',
};

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  FAILED: 'bg-red-50 text-red-700',
  IN_PROGRESS: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]',
  PENDING: 'bg-[#F7F9FC] text-[var(--oqh-text-muted)]',
};

export function TestDataDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showCreateProfile, setShowCreateProfile] = useState(false);
  const [genProfileId, setGenProfileId] = useState('');
  const [genRowCount, setGenRowCount] = useState(100);
  const [genFormat, setGenFormat] = useState('JSON');

  const { data: template, isLoading } = useQuery<TestDataTemplateDetail>({
    queryKey: ['test-data-template', id],
    queryFn: () => testDataApi.getTemplateById(id!),
    enabled: !!id,
  });

  const { data: generations } = useQuery<Page<TestDataGenerationItem>>({
    queryKey: ['test-data-generations', id],
    queryFn: () => testDataApi.getGenerationsByTemplate(id!, { size: '20' }),
    enabled: !!id,
  });

  const generateMutation = useMutation({
    mutationFn: () =>
      testDataApi.generate({
        templateId: id!,
        profileId: genProfileId || undefined,
        rowCount: genRowCount,
        exportFormat: genFormat,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-generations', id] });
      queryClient.invalidateQueries({ queryKey: ['test-data-template', id] });
      queryClient.invalidateQueries({ queryKey: ['test-data-dashboard'] });
    },
  });

  const deleteProfileMutation = useMutation({
    mutationFn: (profileId: string) => testDataApi.deleteProfile(profileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-template', id] });
    },
  });

  const handleDownload = async (genId: string) => {
    try {
      const blob = await testDataApi.downloadGeneration(genId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `test-data-${genId}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      alert('Download failed');
    }
  };

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading template...</div>;
  }

  if (!template) {
    return <div className="text-center py-12 text-[#9BA8B9]">Template not found</div>;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button onClick={() => navigate('/test-data')} className="p-2 hover:bg-[#F7F9FC] rounded-lg transition-colors">
          <ArrowLeft className="h-5 w-5 text-[var(--oqh-text-muted)]" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">{template.name}</h1>
          {template.description && <p className="text-sm text-[var(--oqh-text-muted)] mt-1">{template.description}</p>}
          {template.projectName && (
            <span className="text-xs text-[var(--oqh-blue)] font-medium">{template.projectName}</span>
          )}
        </div>
      </div>

      {/* Fields Table */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-6">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-4 flex items-center gap-2">
          <Database className="h-4 w-4 text-[var(--oqh-blue)]" />
          Fields ({template.fields.length})
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--oqh-border)]">
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">#</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Name</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Type</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Nullable</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Faker Generator</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Constraints</th>
              </tr>
            </thead>
            <tbody>
              {template.fields.map((field, i) => (
                <tr key={field.id} className="border-b border-gray-50 hover:bg-[#F7F9FC]">
                  <td className="py-2 px-3 text-[#9BA8B9]">{i + 1}</td>
                  <td className="py-2 px-3 font-medium text-[var(--oqh-text)]">{field.name}</td>
                  <td className="py-2 px-3">
                    <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-indigo-50 text-indigo-700">
                      {field.fieldType}
                    </span>
                  </td>
                  <td className="py-2 px-3">{field.nullable ? '✓' : '—'}</td>
                  <td className="py-2 px-3 text-[var(--oqh-text-muted)] font-mono text-xs">{field.fakerGenerator || '—'}</td>
                  <td className="py-2 px-3 text-[var(--oqh-text-muted)] font-mono text-xs max-w-[200px] truncate">
                    {field.constraints && field.constraints !== '{}' ? field.constraints : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Profiles */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)]">Data Profiles ({template.profiles.length})</h3>
          <button
            onClick={() => setShowCreateProfile(true)}
            className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] hover:bg-[var(--oqh-active-bg)] rounded-lg transition-colors"
          >
            <Plus className="h-3.5 w-3.5" /> Add Profile
          </button>
        </div>
        {template.profiles.length === 0 ? (
          <p className="text-sm text-[#9BA8B9]">No profiles yet. Add one to control data generation strategy.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {template.profiles.map((profile) => (
              <div
                key={profile.id}
                className={`p-4 rounded-lg border ${PROFILE_COLORS[profile.profileType] || 'bg-[#F7F9FC] text-[var(--oqh-text)] border-[var(--oqh-border)]'}`}
              >
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-bold uppercase">{profile.profileType}</span>
                    <h4 className="text-sm font-semibold mt-1">{profile.name}</h4>
                    {profile.description && <p className="text-xs mt-1 opacity-80">{profile.description}</p>}
                  </div>
                  <button
                    onClick={() => {
                      if (confirm(`Delete profile "${profile.name}"?`)) {
                        deleteProfileMutation.mutate(profile.id);
                      }
                    }}
                    className="p-1 opacity-50 hover:opacity-100 transition-opacity"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Generate Data */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-6">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Generate Data</h3>
        <div className="flex items-end gap-4">
          <div>
            <label className="block text-xs font-medium text-[var(--oqh-text-muted)] mb-1">Profile</label>
            <select
              value={genProfileId}
              onChange={(e) => setGenProfileId(e.target.value)}
              className="border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm"
            >
              <option value="">Default (Positive)</option>
              {template.profiles.map((p) => (
                <option key={p.id} value={p.id}>{p.name} ({p.profileType})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-[var(--oqh-text-muted)] mb-1">Row Count</label>
            <input
              type="number" min={1} max={10000} value={genRowCount}
              onChange={(e) => setGenRowCount(Number(e.target.value))}
              className="border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm w-28"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-[var(--oqh-text-muted)] mb-1">Format</label>
            <select
              value={genFormat}
              onChange={(e) => setGenFormat(e.target.value)}
              className="border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm"
            >
              <option value="JSON">JSON</option>
              <option value="CSV">CSV</option>
            </select>
          </div>
          <button
            onClick={() => generateMutation.mutate()}
            disabled={generateMutation.isPending}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 transition-colors"
          >
            <Play className="h-4 w-4" />
            {generateMutation.isPending ? 'Generating...' : 'Generate'}
          </button>
        </div>
        {generateMutation.isSuccess && (
          <div className="mt-3 p-2 bg-emerald-50 text-emerald-700 text-sm rounded">
            Generated {generateMutation.data.rowCount} rows successfully.
          </div>
        )}
        {generateMutation.isError && (
          <div className="mt-3 p-2 bg-red-50 text-red-700 text-sm rounded">
            Generation failed. Check template fields.
          </div>
        )}
      </div>

      {/* Generation History */}
      <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-6">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-4">Generation History</h3>
        {!generations?.content.length ? (
          <p className="text-sm text-[#9BA8B9]">No generations yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[var(--oqh-border)]">
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Profile</th>
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Rows</th>
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Format</th>
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Status</th>
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Created</th>
                  <th className="text-left py-2 px-3 text-xs font-medium text-[var(--oqh-text-muted)] uppercase">Actions</th>
                </tr>
              </thead>
              <tbody>
                {generations.content.map((gen) => (
                  <tr key={gen.id} className="border-b border-gray-50 hover:bg-[#F7F9FC]">
                    <td className="py-2 px-3">{gen.profileName || 'Default'}</td>
                    <td className="py-2 px-3 font-medium">{gen.rowCount.toLocaleString()}</td>
                    <td className="py-2 px-3">
                      <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-[#F7F9FC] text-[var(--oqh-text)]">
                        {gen.exportFormat}
                      </span>
                    </td>
                    <td className="py-2 px-3">
                      <span className={`px-2 py-0.5 text-[10px] font-bold rounded ${STATUS_COLORS[gen.status] || 'bg-[#F7F9FC] text-[var(--oqh-text-muted)]'}`}>
                        {gen.status}
                      </span>
                    </td>
                    <td className="py-2 px-3 text-[var(--oqh-text-muted)]">
                      {new Date(gen.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-2 px-3">
                      {gen.status === 'COMPLETED' && (
                        <button
                          onClick={() => handleDownload(gen.id)}
                          className="p-1 text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] transition-colors"
                          title="Download"
                        >
                          <Download className="h-4 w-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Profile Modal */}
      {showCreateProfile && (
        <CreateProfileModal
          templateId={id!}
          onClose={() => setShowCreateProfile(false)}
          onCreated={() => {
            setShowCreateProfile(false);
            queryClient.invalidateQueries({ queryKey: ['test-data-template', id] });
          }}
        />
      )}
    </div>
  );
}

function CreateProfileModal({ templateId, onClose, onCreated }: {
  templateId: string; onClose: () => void; onCreated: () => void;
}) {
  const [name, setName] = useState('');
  const [profileType, setProfileType] = useState('POSITIVE');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');

  const createMutation = useMutation({
    mutationFn: () =>
      testDataApi.createProfile({
        name,
        templateId,
        profileType,
        description: description || undefined,
      }),
    onSuccess: () => onCreated(),
    onError: (err: any) => setError(err.response?.data?.message || 'Failed to create profile'),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold text-[var(--oqh-text)] mb-4">New Data Profile</h2>
        {error && <div className="mb-3 p-2 bg-red-50 text-red-700 text-sm rounded">{error}</div>}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Name</label>
            <input
              type="text" value={name} onChange={(e) => setName(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
              placeholder="e.g., Valid Registrations"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Profile Type</label>
            <select
              value={profileType} onChange={(e) => setProfileType(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
            >
              {PROFILE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Description</label>
            <textarea
              value={description} onChange={(e) => setDescription(e.target.value)} rows={2}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
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
            {createMutation.isPending ? 'Creating...' : 'Create Profile'}
          </button>
        </div>
      </div>
    </div>
  );
}
