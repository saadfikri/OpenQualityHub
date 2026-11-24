import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectsApi } from '@/api/projects';
import { teamsApi } from '@/api/teams';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { Plus, FolderKanban, Pencil, Trash2 } from 'lucide-react';
import type { CreateProjectRequest, Project } from '@/types/api';

export function ProjectsPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [form, setForm] = useState<CreateProjectRequest>({ name: '', description: '', teamId: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectsApi.getAll(),
  });

  const { data: teams } = useQuery({
    queryKey: ['teams'],
    queryFn: () => teamsApi.getAll(0, 100),
  });

  const createMutation = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      closeModal();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<CreateProjectRequest> }) =>
      projectsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      closeModal();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: projectsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });

  function openCreate() {
    setEditingProject(null);
    setForm({ name: '', description: '', teamId: teams?.content[0]?.id ?? '' });
    setModalOpen(true);
  }

  function openEdit(project: Project) {
    setEditingProject(project);
    setForm({ name: project.name, description: project.description ?? '', teamId: project.teamId });
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setEditingProject(null);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editingProject) {
      updateMutation.mutate({ id: editingProject.id, data: { name: form.name, description: form.description } });
    } else {
      createMutation.mutate(form);
    }
  }

  if (isLoading) {
    return <div className="text-center py-12 text-[var(--oqh-text-muted)]">Loading...</div>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Projects</h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Track quality across all your projects</p>
        </div>
        <Button onClick={openCreate} disabled={!teams?.content.length}>
          <Plus className="h-4 w-4" />
          New Project
        </Button>
      </div>

      {data?.content.length === 0 ? (
        <EmptyState
          icon={<FolderKanban className="h-12 w-12" />}
          title="No projects yet"
          description="Create a team first, then add projects to it."
          action={
            teams?.content.length ? (
              <Button onClick={openCreate}>
                <Plus className="h-4 w-4" />
                Create Project
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="bg-white rounded-xl border border-[var(--oqh-border)] overflow-hidden shadow-[var(--oqh-shadow)]">
          <table className="min-w-full divide-y divide-[var(--oqh-border)]">
            <thead className="bg-[#F7F9FC]">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Project</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Team</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Test Runs</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--oqh-border)]">
              {data?.content.map((project) => (
                <tr key={project.id} className="hover:bg-[#F7F9FC]">
                  <td className="px-6 py-4">
                    <div className="font-medium text-[var(--oqh-text)]">{project.name}</div>
                    {project.description && (
                      <div className="text-sm text-[var(--oqh-text-muted)] truncate max-w-xs">{project.description}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{project.teamName}</td>
                  <td className="px-6 py-4">
                    <Badge variant={project.status === 'ACTIVE' ? 'success' : 'default'}>
                      {project.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{project.testRunCount}</td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-1">
                      <button onClick={() => openEdit(project)} className="p-1.5 text-[#9BA8B9] hover:text-[var(--oqh-text-muted)] rounded">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => { if (confirm('Delete this project?')) deleteMutation.mutate(project.id); }}
                        className="p-1.5 text-[#9BA8B9] hover:text-red-600 rounded"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={modalOpen} onClose={closeModal} title={editingProject ? 'Edit Project' : 'New Project'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            id="name"
            label="Project Name"
            required
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="e.g. Web Portal"
          />
          <Input
            id="description"
            label="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="Brief description of this project"
          />
          {!editingProject && (
            <div>
              <label htmlFor="teamId" className="block text-sm font-medium text-[var(--oqh-text)] mb-1">Team</label>
              <select
                id="teamId"
                required
                value={form.teamId}
                onChange={(e) => setForm({ ...form, teamId: e.target.value })}
                className="block w-full rounded-lg border border-[var(--oqh-border)] px-3 py-2 text-sm focus:border-[var(--oqh-blue)] focus:outline-none focus:ring-1 focus:ring-[var(--oqh-blue)]"
              >
                <option value="">Select a team</option>
                {teams?.content.map((team) => (
                  <option key={team.id} value={team.id}>{team.name}</option>
                ))}
              </select>
            </div>
          )}
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" type="button" onClick={closeModal}>Cancel</Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {editingProject ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
