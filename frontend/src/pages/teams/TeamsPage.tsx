import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { teamsApi } from '@/api/teams';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/EmptyState';
import { Plus, Users, Pencil, Trash2 } from 'lucide-react';
import type { CreateTeamRequest, Team } from '@/types/api';

export function TeamsPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | null>(null);
  const [form, setForm] = useState<CreateTeamRequest>({ name: '', description: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['teams'],
    queryFn: () => teamsApi.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: teamsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      closeModal();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateTeamRequest }) => teamsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      closeModal();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: teamsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['teams'] }),
  });

  function openCreate() {
    setEditingTeam(null);
    setForm({ name: '', description: '' });
    setModalOpen(true);
  }

  function openEdit(team: Team) {
    setEditingTeam(team);
    setForm({ name: team.name, description: team.description ?? '' });
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setEditingTeam(null);
    setForm({ name: '', description: '' });
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editingTeam) {
      updateMutation.mutate({ id: editingTeam.id, data: form });
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
          <h1 className="text-2xl font-bold text-[var(--oqh-text)]">Teams</h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">Manage your quality engineering teams</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" />
          New Team
        </Button>
      </div>

      {data?.content.length === 0 ? (
        <EmptyState
          icon={<Users className="h-12 w-12" />}
          title="No teams yet"
          description="Create your first team to start organizing projects."
          action={
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" />
              Create Team
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data?.content.map((team) => (
            <div key={team.id} className="bg-white rounded-xl border border-[var(--oqh-border)] p-6 shadow-[var(--oqh-shadow)] transition-all duration-200 hover:shadow-[0_4px_18px_rgba(14,43,73,.08)] hover:border-[var(--oqh-blue)]/30 hover:-translate-y-0.5">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-[var(--oqh-text)]">{team.name}</h3>
                  {team.description && (
                    <p className="mt-1 text-sm text-[var(--oqh-text-muted)] line-clamp-2">{team.description}</p>
                  )}
                </div>
                <div className="flex gap-1">
                  <button onClick={() => openEdit(team)} className="p-1.5 text-[#9BA8B9] hover:text-[var(--oqh-text-muted)] rounded">
                    <Pencil className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => { if (confirm('Delete this team and all its projects?')) deleteMutation.mutate(team.id); }}
                    className="p-1.5 text-[#9BA8B9] hover:text-red-600 rounded"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-[var(--oqh-border)]/50 text-sm text-[var(--oqh-text-muted)]">
                {team.projectCount} project{team.projectCount !== 1 ? 's' : ''}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={modalOpen} onClose={closeModal} title={editingTeam ? 'Edit Team' : 'New Team'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            id="name"
            label="Team Name"
            required
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="e.g. Platform QA"
          />
          <Input
            id="description"
            label="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="What does this team work on?"
          />
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="secondary" type="button" onClick={closeModal}>
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {editingTeam ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
