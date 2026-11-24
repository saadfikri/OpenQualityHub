import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Database, Plus, Layers, FlaskConical, Sparkles,
  ChevronRight, Trash2,
} from 'lucide-react';
import { testDataApi } from '@/api/test-data';
import type { Page, TestDataTemplate, TestDataDashboard } from '@/types/api';

const FIELD_TYPES = [
  'STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME',
  'EMAIL', 'PHONE', 'UUID', 'ENUM', 'REGEX_PATTERN',
];

const PROFILE_TYPE_COLORS: Record<string, string> = {
  POSITIVE: 'bg-emerald-50 text-emerald-700',
  NEGATIVE: 'bg-red-50 text-red-700',
  EDGE_CASE: 'bg-amber-50 text-amber-700',
  BOUNDARY: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]',
  LOCALIZATION: 'bg-purple-50 text-purple-700',
  PERFORMANCE: 'bg-orange-50 text-orange-700',
};

export function TestDataPage() {
  const [showCreate, setShowCreate] = useState(false);
  const queryClient = useQueryClient();

  const { data: dashboard, isLoading: loadingDash } = useQuery<TestDataDashboard>({
    queryKey: ['test-data-dashboard'],
    queryFn: testDataApi.getDashboard,
  });

  const { data: templates, isLoading: loadingList } = useQuery<Page<TestDataTemplate>>({
    queryKey: ['test-data-templates'],
    queryFn: () => testDataApi.getTemplates({ size: '50' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => testDataApi.deleteTemplate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-templates'] });
      queryClient.invalidateQueries({ queryKey: ['test-data-dashboard'] });
    },
  });

  if (loadingDash && loadingList) {
    return <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading test data...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Test Data Management</h1>
          <p className="text-sm text-[var(--oqh-text-muted)] mt-1">Synthetic data templates, profiles, and generation</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-[var(--oqh-blue)] text-white text-sm font-medium rounded-lg hover:bg-[var(--oqh-primary)] transition-colors"
        >
          <Plus className="h-4 w-4" />
          New Template
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard icon={Database} label="Templates" value={dashboard?.totalTemplates ?? 0} color="blue" />
        <StatCard icon={Layers} label="Profiles" value={dashboard?.totalProfiles ?? 0} color="emerald" />
        <StatCard icon={Sparkles} label="Generations" value={dashboard?.totalGenerations ?? 0} color="indigo" />
      </div>

      {/* Recent generations */}
      {dashboard?.recentGenerations && dashboard.recentGenerations.length > 0 && (
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-6">
          <h3 className="text-sm font-semibold text-[var(--oqh-text)] mb-3">Recent Generations</h3>
          <div className="space-y-2">
            {dashboard.recentGenerations.slice(0, 5).map((gen) => (
              <div key={gen.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                <div>
                  <span className="text-sm font-medium text-[var(--oqh-text)]">{gen.templateName}</span>
                  {gen.profileName && (
                    <span className={`ml-2 px-2 py-0.5 text-[10px] font-medium rounded ${PROFILE_TYPE_COLORS[gen.profileName] || 'bg-[#F7F9FC] text-[var(--oqh-text)]'}`}>
                      {gen.profileName}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-[var(--oqh-text-muted)]">{gen.rowCount} rows</span>
                  <span className={`px-2 py-0.5 text-[10px] font-medium rounded ${
                    gen.status === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700' :
                    gen.status === 'FAILED' ? 'bg-red-50 text-red-700' :
                    'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]'
                  }`}>
                    {gen.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Template cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {templates?.content.map((item) => (
          <TemplateCard
            key={item.id}
            item={item}
            onDelete={() => {
              if (confirm(`Delete template "${item.name}"?`)) {
                deleteMutation.mutate(item.id);
              }
            }}
          />
        ))}
        {templates?.content.length === 0 && (
          <div className="col-span-full text-center py-12 text-[#9BA8B9]">
            No templates yet. Create one to start generating test data.
          </div>
        )}
      </div>

      {showCreate && (
        <CreateTemplateModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            queryClient.invalidateQueries({ queryKey: ['test-data-templates'] });
            queryClient.invalidateQueries({ queryKey: ['test-data-dashboard'] });
          }}
        />
      )}
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color }: {
  icon: typeof Database; label: string; value: number; color: string;
}) {
  const colorMap: Record<string, string> = {
    blue: 'bg-[var(--oqh-active-bg)] text-[var(--oqh-blue)]',
    emerald: 'bg-emerald-50 text-emerald-700',
    indigo: 'bg-indigo-50 text-indigo-700',
  };
  return (
    <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
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

function TemplateCard({ item, onDelete }: { item: TestDataTemplate; onDelete: () => void }) {
  return (
    <div className="bg-white rounded-xl border border-[var(--oqh-border)] shadow-[var(--oqh-shadow)] p-5 transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <FlaskConical className="h-4 w-4 text-[var(--oqh-blue)]" />
          <span className="text-xs font-medium text-[var(--oqh-text-muted)]">
            {item.fieldCount} fields
          </span>
        </div>
        <button onClick={onDelete} className="p-1 text-[#9BA8B9] hover:text-red-500 rounded transition-colors">
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
      <Link to={`/test-data/${item.id}`} className="block group">
        <h3 className="text-sm font-semibold text-[var(--oqh-text)] group-hover:text-[var(--oqh-blue)] transition-colors">{item.name}</h3>
        {item.description && <p className="text-xs text-[var(--oqh-text-muted)] mt-1 line-clamp-2">{item.description}</p>}
        <div className="flex items-center justify-between mt-4 pt-3 border-t border-[var(--oqh-border)]/50">
          <div className="text-xs text-[var(--oqh-text-muted)]">
            <span className="font-medium text-[var(--oqh-text)]">{item.profileCount}</span> profiles
            <span className="mx-1">·</span>
            <span className="font-medium text-[var(--oqh-text)]">{item.generationCount}</span> generations
            {item.projectName && <span className="ml-2">| {item.projectName}</span>}
          </div>
          <ChevronRight className="h-4 w-4 text-[#9BA8B9] group-hover:text-[var(--oqh-blue)] transition-colors" />
        </div>
      </Link>
    </div>
  );
}

function CreateTemplateModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [fields, setFields] = useState([
    { name: '', fieldType: 'STRING', displayOrder: 0, nullable: false, fakerGenerator: '' },
  ]);
  const [error, setError] = useState('');

  const addField = () => {
    setFields([...fields, { name: '', fieldType: 'STRING', displayOrder: fields.length, nullable: false, fakerGenerator: '' }]);
  };

  const removeField = (index: number) => {
    setFields(fields.filter((_, i) => i !== index));
  };

  const updateField = (index: number, key: string, value: string | boolean | number) => {
    const updated = [...fields];
    updated[index] = { ...updated[index], [key]: value };
    setFields(updated);
  };

  const createMutation = useMutation({
    mutationFn: () =>
      testDataApi.createTemplate({
        name,
        description: description || undefined,
        fields: fields.map((f, i) => ({
          name: f.name,
          fieldType: f.fieldType,
          displayOrder: i,
          nullable: f.nullable,
          fakerGenerator: f.fakerGenerator || undefined,
        })),
      }),
    onSuccess: () => onCreated(),
    onError: (err: any) => setError(err.response?.data?.message || 'Failed to create template'),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold text-[var(--oqh-text)] mb-4">New Test Data Template</h2>
        {error && <div className="mb-3 p-2 bg-red-50 text-red-700 text-sm rounded">{error}</div>}

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Template Name</label>
            <input
              type="text" value={name} onChange={(e) => setName(e.target.value)}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
              placeholder="e.g., User Registration Form"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Description</label>
            <textarea
              value={description} onChange={(e) => setDescription(e.target.value)} rows={2}
              className="w-full border border-[var(--oqh-border)] rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)]"
            />
          </div>

          {/* Field builder */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-[var(--oqh-text)]">Fields</label>
              <button onClick={addField} className="text-xs text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] font-medium">
                + Add Field
              </button>
            </div>
            <div className="space-y-2">
              {fields.map((field, i) => (
                <div key={i} className="flex items-center gap-2 p-3 bg-[#F7F9FC] rounded-lg">
                  <input
                    type="text"
                    value={field.name}
                    onChange={(e) => updateField(i, 'name', e.target.value)}
                    placeholder="Field name"
                    className="flex-1 border border-[var(--oqh-border)] rounded px-2 py-1.5 text-sm"
                  />
                  <select
                    value={field.fieldType}
                    onChange={(e) => updateField(i, 'fieldType', e.target.value)}
                    className="border border-[var(--oqh-border)] rounded px-2 py-1.5 text-sm"
                  >
                    {FIELD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                  <input
                    type="text"
                    value={field.fakerGenerator}
                    onChange={(e) => updateField(i, 'fakerGenerator', e.target.value)}
                    placeholder="Faker (e.g., Name.firstName)"
                    className="w-40 border border-[var(--oqh-border)] rounded px-2 py-1.5 text-sm"
                  />
                  <label className="flex items-center gap-1 text-xs">
                    <input
                      type="checkbox"
                      checked={field.nullable}
                      onChange={(e) => updateField(i, 'nullable', e.target.checked)}
                    />
                    Null
                  </label>
                  {fields.length > 1 && (
                    <button onClick={() => removeField(i)} className="p-1 text-[#9BA8B9] hover:text-red-500">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-[var(--oqh-text)] hover:text-[var(--oqh-text)] transition-colors">Cancel</button>
          <button
            onClick={() => createMutation.mutate()}
            disabled={!name.trim() || fields.some((f) => !f.name.trim()) || createMutation.isPending}
            className="px-4 py-2 text-sm font-medium text-white bg-[var(--oqh-blue)] rounded-lg hover:bg-[var(--oqh-primary)] disabled:opacity-50 transition-colors"
          >
            {createMutation.isPending ? 'Creating...' : 'Create Template'}
          </button>
        </div>
      </div>
    </div>
  );
}
