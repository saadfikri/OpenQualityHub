import api from './client';
import type { Page, Team, CreateTeamRequest, UpdateTeamRequest } from '@/types/api';

export const teamsApi = {
  getAll: (page = 0, size = 20, search?: string) =>
    api.get<Page<Team>>('/teams', { params: { page, size, search } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<Team>(`/teams/${id}`).then((r) => r.data),

  create: (data: CreateTeamRequest) =>
    api.post<Team>('/teams', data).then((r) => r.data),

  update: (id: string, data: UpdateTeamRequest) =>
    api.put<Team>(`/teams/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete(`/teams/${id}`),
};
