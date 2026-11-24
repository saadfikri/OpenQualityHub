import api from './client';
import type { Page, Project, CreateProjectRequest, UpdateProjectRequest } from '@/types/api';

export const projectsApi = {
  getAll: (page = 0, size = 20, teamId?: string) =>
    api.get<Page<Project>>('/projects', { params: { page, size, teamId } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<Project>(`/projects/${id}`).then((r) => r.data),

  create: (data: CreateProjectRequest) =>
    api.post<Project>('/projects', data).then((r) => r.data),

  update: (id: string, data: UpdateProjectRequest) =>
    api.put<Project>(`/projects/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete(`/projects/${id}`),
};
